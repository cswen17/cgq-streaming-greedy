package algorithm.impl.graph

import algorithm.E._

import play.api.libs.json._
import play.api.libs.functional._


/* Case Class definition for GroundSetPayload type */
case class NodesRequestParams(
  ground_set: VertexLabel,
  sets: List[VertexSet]) extends GroundSetBase[VertexSet] {

  override val payloadSet: VertexSet = this.sets(0)
}

object NodesGround {
  /*
   * Payload class that determines ground set membership by the first
   * node visited in the independent set.
   *
   * Only 12 nodes are available, A-L
   *
   * Graph:
   *
   *  Generalized Petersen (6, 2)
   *         A -- B
   *      /  |    |  \
   *    F    G    H    C
   *    | \  | \/ |  / |
   *    |  L-|- \--I   |
   *    |    | /\ |    |
   *    E--- K   J---- D
   *      \         /
   *       --------
   *
   * Example of Ground Set Membership:
   *   ACEL belongs to the ground set A
   *   BCEK belongs to the ground set B
   *
   * Sample Payload JSON:
   *   {
   *     "ground_set": "A",
   *     "sets": ["ACEL", "BCEL", "ABEK", "AEIJ"]
   *   }
   *
   * -type Element: String for which nodes get visited. Example: "ACEL"
   * -type L: String for the node label. Example: "A"
   * -type GroundSetPayload: FirstNodePayload case class
   */
  class NodesGround extends GroundSetAPI[
    VertexSet, VertexLabel, NodesRequestParams] {
    type V = VertexSet
    type G = VertexLabel

    // Matches the Lambda function name in the AWS console
    override val awsLambdaName: String = "ground_set_oracle"

    override def requestReads: Reads[RequestParams] = {
      implicit val reads: Reads[V] = VertexSet.reads
      implicit val writes: Writes[V] = VertexSet.writes
      implicit val labelReads: Reads[G] = VertexLabel.reads
      implicit val labelWrites: Writes[G] = VertexLabel.writes
      Json.reads[RequestParams]
    }

    override def requestWrites: Writes[RequestParams] = {
      implicit val reads: Reads[V] = VertexSet.reads
      implicit val writes: Writes[V] = VertexSet.writes
      implicit val labelReads: Reads[G] = VertexLabel.reads
      implicit val labelWrites: Writes[G] = VertexLabel.writes
      Json.writes[RequestParams]
    }

    override def loadEInN_L(
        e: V, S: Set[V], l: G): RequestParams = {
      NodesRequestParams(l, Set(e).toList)
    }

    override def loadSPlusEInN_L(
        e: V, S: Set[V], l: G): List[RequestParams] = {
      val SPlusE: Set[V] = S + e
      val iter: Iterator[RequestParams] = SPlusE.map { elem =>
        NodesRequestParams(l, List(elem))
      }.toSet.iterator
      iter.toList
    }

    override def loadS_L(
        e: V, S: Set[V], l: G): List[RequestParams] = {
      val iter: Iterator[RequestParams] = S.map { elem =>
        NodesRequestParams(l, List(elem))
      }.toSet.iterator
      iter.toList
    }

    override def toJsonString(S: Set[V]): Set[String] = {
      // This method writes an Element (VertexSet) to a Json string
      implicit val writes: Writes[V] = VertexSet.writes
      S.map { s => Json.stringify(Json.toJson(s.asInstanceOf[V])) }
    }

    override def toElementSet(S: Set[JsValue]): Set[V] = {
      implicit val reads: Reads[V] = VertexSet.reads
      S.map {  jsValue => 

        val result: V= Json.fromJson(jsValue) match {
          case success: JsSuccess[V] => success.get
          case error: JsError =>
            println("Error converting JSON to VertexSet: "+error)
            VertexSet("")
        }

        result
      }
    }

  }

}
