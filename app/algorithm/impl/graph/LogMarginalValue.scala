package algorithm.impl.graph

import play.api.libs.json._
import play.api.libs.functional.syntax._

import algorithm.E._

object LogMarginalValue {
  class LogMarginalValue extends MarginalValueAPI[VertexSet] {
    type V = VertexSet

    case class LambdaPayload(e: V, S: Set[V], valueType: String) {
      override def toString(): String = {
        s"(e: $e, S: $S, valueType: $valueType)"
      }
    }

    case class LambdaResponse(
      statusCode: Int,
      body: Double,
      reason: String) extends MarginalValueLambdaResponse {
      // Note: Don't edit the argument names for this class!
      // It will break Json writing for LambdaResponse
      // The Lambda function returns these fields exactly.

      override def marginalValue(): Double = this.body
      override def notes(): String = this.reason
      override def responseStatusCode(): Int = this.statusCode
    }

    // Begin overrides
    override type RequestParams = LambdaPayload
    override type ResponseBody = LambdaResponse

    override val awsLambdaName: String = "logarithmic_marginal_value"

    override def marginalValue(
      e: V, S: Set[V], valueType: String): RequestParams = {

      LambdaPayload(e, S, valueType)
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

    override def responseReads: Reads[ResponseBody] = {
      Json.reads[ResponseBody]
    }

    override def responseWrites: Writes[ResponseBody] = {
      Json.writes[ResponseBody]
    }

  }
}
