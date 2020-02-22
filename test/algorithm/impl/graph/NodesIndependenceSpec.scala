package algorithm.impl.graph

import org.scalatestplus.play._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import algorithm.E._
import algorithm.impl.graph.NodesIndependence._


/** Tests for NodesIndependence
 *
 *  These tests will ensure that NodesIndependence conforms to the
 *  IndependenceAPI[E] type
 */
class NodesIndependenceSpec extends PlaySpec {

  "Constructor and types" should {

    "implement expected types" in {
      assertCompiles(
        """val nodesIndependence: IndependenceAPI[
             VertexSet] = new NodesIndependence()""")
      assertCompiles(
        """val nodesIndependence: NodesIndependence = {
             new NodesIndependence()
           }""")
    }

    "instantiate 100 times without throwing exceptions" in {
      Range(1, 100).map { _ => new NodesIndependence() }.toList
    }

    "remember to override awsLambdaName" in {
      val nodesIndependence: IndependenceAPI[VertexSet] = {
        new NodesIndependence()
      }
      nodesIndependence.awsLambdaName mustBe "independence_set_oracle"
    }

  }

  "requestReads and requestWrites" should {
    "produce correct JSON strings" in {
      val nodesIndependence: IndependenceAPI[VertexSet] = {
        new NodesIndependence()
      }
      type LambdaPayload = nodesIndependence.RequestParams
      implicit val reads: Reads[LambdaPayload] = nodesIndependence.requestReads
      val testPayload: String = """
      {
        "sets": [
          {"vertices": "ABCD"},
          {"vertices": "ABCE"},
          {"vertices": "ABCF"},
          {"vertices": "ABCG"}
        ]
      }
      """
      val testJson: JsValue = Json.parse(testPayload)
      testJson.validate[LambdaPayload] match {
        case success: JsSuccess[LambdaPayload] => success.get
        case error: JsError => fail(testJson.toString)
      }
    }
  }

  "loadInI_L and loadNotInI_L" should {
  }
}
