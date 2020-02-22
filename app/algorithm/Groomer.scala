package algorithm

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import play.api.libs.json._

import algorithm.E._
import clients.LambdaClient._
import clients.RedisSetClient._


object Groomer {
  class Groomer[
    E <: Element,
    L,
    GroundSetPayload <: GroundSetBase[E]] (e: E, l: L)
    (implicit
      ground: GroundSetAPI[E, L, GroundSetPayload],
      independent: IndependenceAPI[E]) {

    // Adapter Types and Values
    private type P = GroundSetBase[E]
    private type I = independent.RequestParams

    val groundSetReads = ground.requestReads.asInstanceOf[Reads[P]]
    val groundSetWrites = ground.requestWrites.asInstanceOf[Writes[P]]
    private implicit val gReads: Reads[P] = groundSetReads
    private implicit val gWrites: Writes[P] = groundSetWrites
    private implicit val iReads: Reads[I] = independent.requestReads
    private implicit val iWrites: Writes[I] = independent.requestWrites

    val groundSet: String = ground.awsLambdaName
    val independentSet: String = independent.awsLambdaName
    val key: String = "S_" + groundSet + "_" + independentSet

    // Clients
    implicit val hostName: String = "localhost"
    implicit val port: Int = 6379
    val redis: RedisSetClient = new RedisSetClient(key)

    private val gLambda: LambdaClient[P] = new LambdaClient[P](groundSet)
    private val iLambda: LambdaClient[I] = new LambdaClient[I](independentSet)

    private def getS(): Set[E] = {
      val raw: Set[JsValue] = redis.readAllJson()
      val S: Set[E] = ground.toElementSet(raw)
      S
    }

    // S_l = S \intersect N_l
    def findS_L(): Set[E] = {
      val S: Set[E] = this.getS()
      println("S is: " + S)

      val ps: List[P] = ground.loadS_L(this.e, S, this.l)
      
      val sInN_L: List[P] = ps.filter(gLambda.invoke)
      val S_L: Set[E] = sInN_L.map { res => res.payloadSet }.toSet
      S_L
    }

    // X := {s \in S_l: (S_l - s + e) \in I_l}
    // Return X to ExchangeCandidates and begin the next part: Finding c_l
    def findX(S_L: Set[E]): Set[E] = S_L.filter { s =>
      val S_LMinusSPlusE: Set[E] = S_L - s + this.e
      val sInI_LPayload: I = independent.loadInI_L(S_LMinusSPlusE)
      val shouldFilter: Boolean = iLambda.invoke(sInI_LPayload)
      println(s"Filtering out $s if shouldFilter is true. shouldFilter: $shouldFilter")
      shouldFilter
    }
    
  }
}