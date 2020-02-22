package algorithm

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import play.api.libs.json._

import algorithm.E._
import clients.LambdaClient._
import clients.RedisSetClient._

object Gate {
  class Gate[
    E <: Element,
    L,
    GroundSetPayload <: GroundSetBase[E]](e: E, l: L)
    (implicit
      ground: GroundSetAPI[E, L, GroundSetPayload],
      independent: IndependenceAPI[E]) {

    // Adapter Types and Values
    private type P = GroundSetBase[E]
    private type I = independent.RequestParams

    // Json Reads and Writes to help lambda client
    val groundSetReads = ground.requestReads.asInstanceOf[Reads[P]]
    val groundSetWrites = ground.requestWrites.asInstanceOf[Writes[P]]
    private implicit val gReads: Reads[P] = groundSetReads
    private implicit val gWrites: Writes[P] = groundSetWrites
    private implicit val iReads: Reads[I] = independent.requestReads
    private implicit val iWrites: Writes[I] = independent.requestWrites


    val groundSet: String = ground.awsLambdaName
    val independentSet: String = independent.awsLambdaName
    val key: String = "S_" + groundSet + "_" + independentSet

    // Redis setup
    implicit val hostName: String = "localhost"
    implicit val port: Int = 6379
    val redis: RedisSetClient = new RedisSetClient(key)

    private val gLambda: LambdaClient[P] = new LambdaClient[P](groundSet)
    private val iLambda: LambdaClient[I] = new LambdaClient[I](independentSet)

    private def getS(): Set[E] = {
      // Retrieves the centrally stored element set from Redis
      // Called by 
      val raw: Set[JsValue] = redis.readAllJson()
      val S: Set[E] = ground.toElementSet(raw)
      S
    }

    def eInN_L(e: E, S: Set[E], l: L): Boolean = {
      // Construct Payload
      // The Redis Set Client returns a Set of Strings. Each String needs
      // to be parsed as JSON and then returned as a Set[E].
      // The only JSON parsing functions allowed to be used must come from
      // the E trait.
      val p: P = ground.loadEInN_L(e, S, l)
      // Call N_l lambda function using lambda client
      val isInN_L: Boolean = gLambda.invoke(p)
      isInN_L
    }

    def ePlusSIntersectN_LNotInI_L(
        e: E, S: Set[E], l: L): Boolean = {
      // Calculates ((S + e) \intersect N_l) \in I_l
      // First constructs a GroundSetPayload for (S + e)
      val ps: List[P] = ground.loadSPlusEInN_L(e, S, l)

      // Launches each GroundSetPayload against the
      // N_l lambda function using lambda client
      // Saves which part of (S + e) is being launched
      // Filters the GroundSetPayloads that returned false
      val payloadsInN_l: Set[E] = {
        ps.filter(gLambda.invoke).map { res =>
          res.payloadSet
        }.toSet
      }
      // Records which elements are in N_l
      // Constructs an IndependencePayload for elements in N_l
      val iPayload: I = independent.loadNotInI_L(payloadsInN_l)
      // Launches the IndependencePayload against I_l
      val isNotInI_L: Boolean = !iLambda.invoke(iPayload)
      // Returns the result
      println("********")
      println(isNotInI_L)
      isNotInI_L
    }

    // API function
    def check(): Boolean = {
      val S: Set[E] = getS()
      val isEInN_L: Boolean = eInN_L(this.e, S, this.l)
      isEInN_L && ePlusSIntersectN_LNotInI_L(this.e, S, this.l)
    }
  }
}