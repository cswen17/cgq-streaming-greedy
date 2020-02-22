package algorithm

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import play.api.libs.json._

import algorithm.E._
import clients.LambdaClient._
import clients.RedisSetClient._

object Admitter {
  class Admitter[
    E <: Element,
    L,
    GroundSetPayload <: GroundSetBase[E]] (X: Set[E])
      (implicit ground: GroundSetAPI[E, L, GroundSetPayload],
      independent: IndependenceAPI[E],
      marginal: MarginalValueAPI[E]) {

    // API values
    private type M = marginal.RequestParams
    private type R = MarginalValueLambdaResponse
    private type D = Reads[R]
    private type W = Writes[R]

    private implicit val mReads: Reads[M] = marginal.requestReads
    private implicit val mWrites: Writes[M] = marginal.requestWrites
    private implicit val rReads: D = marginal.responseReads.asInstanceOf[D]
    private implicit val rWrites: W = marginal.responseWrites.asInstanceOf[W]

    // Lambda Client setup
    val marginalValue: String = marginal.awsLambdaName
    val groundSet: String = ground.awsLambdaName
    val independentSet: String = independent.awsLambdaName

    // Redis Client setup
    implicit val hostName: String = "localhost"
    implicit val port: Int = 6379
    val key: String = "S_" + groundSet + "_" + independentSet

    // Clients
    private val mLambda: LambdaClient[M] = new LambdaClient[M](marginalValue)
    private val redis: RedisSetClient = new RedisSetClient(key)

    private def getS(): Set[E] = {
      println("in findcandidate, loading from redis")
      val raw: Set[JsValue] = redis.readAllJson()
      val S: Set[E] = ground.toElementSet(raw)
      S
    }

    private def minCandidate(x: E) = {
      // Create the payloads
      val S: Set[E] = getS()
      val req: M = marginal.marginalValue(x, S, "f_S(e)") 
      
      // Launch the payload against the lambda client
      // Invoke raw response
      println("Starting invocation of log_marginal_value")
      val res: R = mLambda.invokeRaw[R](req)

      // Parse the response into the LambdaResponse case class...
      val v: Double = res.marginalValue()

      // Find the min number returned by the lambda client
      v
    }

    def findC_L(): Option[E] = this.X.size match {
      case 0 => None
      case _ => Some(this.X.minBy(minCandidate))
    }
    
  }
}