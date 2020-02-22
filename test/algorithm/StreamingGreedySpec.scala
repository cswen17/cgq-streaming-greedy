package algorithm

import org.scalatestplus.play._

import algorithm.E._
import algorithm.StreamingGreedy._
import algorithm.impl.graph._
import algorithm.impl.graph.LogMarginalValue._
import algorithm.impl.graph.NodesGround._
import algorithm.impl.graph.NodesIndependence._
import algorithm.impl.graph.VertexLabelIterator._



class StreamingGreedySpec extends PlaySpec {

  private type VS = VertexSet
  private type VL = VertexLabel
  private type NRP = NodesRequestParams
  private type SG = StreamingGreedy[VS, VL, NRP]

  "StreamingGreedy" should {
    "run" in {
      val v1: VertexSet = VertexSet("ABCD")
      val v2: VertexSet = VertexSet("ACEH")
      val stream: List[VS] = List[VS](v1, v2)
      val streamIter: Iterator[VS] = stream.iterator
      

      implicit val firstNode: GroundSetAPI[VS, VL, NRP] = new NodesGround()
      implicit val label: IteratorAPI[VL] = new VertexLabelIterator()
      implicit val independent: IndependenceAPI[VS] = new NodesIndependence()
      implicit val marginal: MarginalValueAPI[VS] = new LogMarginalValue()

      val streamingGreedy: SG = new StreamingGreedy[VS, VL, NRP]()
      

      streamingGreedy.run(streamIter)
    }
  }
}
