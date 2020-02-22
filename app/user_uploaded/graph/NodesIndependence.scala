package user_uploaded.graph

import play.api.libs.json._
import play.api.libs.functional.syntax._

import algorithm.E._


object NodesIndependence {
  class NodesIndependence extends IndependenceAPI[VertexSet] {
    type V = VertexSet

    // Internal Case Class representation of Lambda Request params
    case class LambdaPayload(sets: List[V])

    // Begin overrides
    override type RequestParams = LambdaPayload

    override val awsLambdaName: String = "independence_set_oracle"

    override def loadInI_L(S: Set[V]): RequestParams = {
      LambdaPayload(S.toList)
    }

    override def loadNotInI_L(S: Set[V]): RequestParams = {
      LambdaPayload(S.toList)
    }

    override def requestReads: Reads[RequestParams] = {
      implicit val reads: Reads[V] = VertexSet.reads
      implicit val writes: Writes[V] = VertexSet.writes
      Json.reads[RequestParams]
    }

    override def requestWrites: Writes[RequestParams] = {
      implicit val reads: Reads[V] = VertexSet.reads
      implicit val writes: Writes[V] = VertexSet.writes
      Json.writes[RequestParams]
    }
  }

}
