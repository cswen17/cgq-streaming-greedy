package algorithm

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import play.api.libs.json._

import algorithm.Admitter._
import algorithm.E._
import algorithm.Gate._
import algorithm.Groomer._
import clients.LambdaClient._
import clients.RedisSetClient._


object ExchangeCandidates {

  /*
   * Runs the Exchange-Candidates subroutine
   *
   * @param L type for Ground Set Iterator, calculates l = 1,...,q
   * @field e an element from the stream
   * @field ground A Ground Set Adapter
   * @field independenceAdapter An Independence Set Adapter
   * @field marginalValueAdapter A Marginal Value Adapter
   */
  class ExchangeCandidates[
    L,
    E <: Element,
    GroundSetPayload <: GroundSetBase[E]](
      e: E,
      iterator: IteratorAPI[L])(
      implicit ground: GroundSetAPI[E, L, GroundSetPayload],
      independent: IndependenceAPI[E],
      marginal: MarginalValueAPI[E]) {

    // Create a Redis Set Client
    val groundSet: String = ground.awsLambdaName
    val independentSet: String = independent.awsLambdaName

    // Clients
    // Redis Set Client- Redis passes in configuration values like
    // host name, port, and a per-connection set name where
    // the set allocated for your actions is set aside
    implicit val hostName: String = "localhost"
    implicit val port: Int = 6379

    // Lookup and iterate through l
    // GroundSetAPI comes with an iterator that, by API requirements,
    // should be used to iterate through all the partitions of the
    // independence set
    val groundSetIterator: Iterator[L] = iterator.groundSetIterator

    /*
     * API function
     */
    def exchangeCandidates(key: String): Set[E] = {
      // Algorithm step by step implementation:
      //   \foreach l = 1, ..., q
      val redis: RedisSetClient = new RedisSetClient(key)

      println("Element stream: " + e)
      groundSetIterator.foreach { l =>

        // Create a future for GroundSetCondition
        val isExchangeable: Future[Boolean] = Future {
          val gate: Gate[E, L, GroundSetPayload] = {
            new Gate[E, L, GroundSetPayload](e, l)
          }
          gate.check()
        }
        // If the future is successful, call the next future, which finds S_l
        val isCandidate: Boolean = Await.result(isExchangeable, Duration.Inf)
        println("The value of isCandidate is " + isCandidate)


        try {
          if (isCandidate) {
            // Find X
            val X: Set[E] = groom(l, e)
            // Find c_l from X
            // Figure out how to pass S to findC_L
            println("Finished calling findX(), starting to call admit")
            val admission: Option[E] = admit(X)
            println("The value of c_l is " + admission)

            // Put c_l in a Redis Set
            // Convert c_l to json first
            admission match {
              case Some(success) => {
                // There's no guarantee that E has a json reads/writes API,
                // so we'll use the functions provided in GroundSetAPI as
                // a workaround
                val c_lJson: Set[String] = ground.toJsonString(Set(success))
                val c_l: String = c_lJson.last

                redis.add(c_l)
                println("Successfully added "+c_l)
              }
              case None => println(s"Found no candidate for $e")
            }
          }
        } catch {
          case exception: Throwable => {
            // Let the foreach continue
            println(s"While finding candidate, caught $exception")
          }
        }
      }

      val raw: Set[JsValue] =  redis.readAllJson()
      // Convert cJsVals to E and return
      val C: Set[E] = ground.toElementSet(raw)
      println(s"In ExchangeCandidates, the result value of C is $C")
      C
    }

    /*
     * Helper function to call a Future to find S_l
     */
    private def groom(l: L, e: E)
      (implicit
        ground: GroundSetAPI[E, L, GroundSetPayload],
        independent: IndependenceAPI[E]): Set[E] = {

      // Create a new CollectPartition Future
      val groomerFuture: Future[Set[E]] = Future {
        val groomer: Groomer[E, L, GroundSetPayload] = {
          new Groomer[E, L, GroundSetPayload](e, l)
        }
        val S_L: Set[E] = groomer.findS_L()
        println("The value of S_L is: " + S_L)
        val X: Set[E] = groomer.findX(S_L)
        println("The value of X is: " + X)
        X
      }
      val X: Set[E] = Await.result(groomerFuture, Duration.Inf)
      X
    }

    /*
     * Helper function to call a Future to find c_l
     * c_l <- argmin_{x \in X}v(f, S, x)
     * C <- C + c_l
     */
    private def admit(X: Set[E])
      (implicit
        marginal: MarginalValueAPI[E],
        ground: GroundSetAPI[E, L, GroundSetPayload],
        independent: IndependenceAPI[E]): Option[E] = {

      // Create a new Admitter Future
      val admitterFuture: Future[Option[E]] = Future {
        println("In admit, creating Future")
        println(s"X is $X")
        val admitter: Admitter[E, L, GroundSetPayload] = {
          new Admitter[E, L, GroundSetPayload](X)
        }

        println("Calling admitter.findC_L")
        val c_l: Option[E] = admitter.findC_L()
        println("The value of c_l is: " + c_l)
        c_l
      }

      val c_l: Option[E] = Await.result(admitterFuture, Duration.Inf)
      c_l
    }
  }
}
