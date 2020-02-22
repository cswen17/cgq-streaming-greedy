package algorithm.impl.graph

import algorithm.E._

import play.api.libs.json._
import play.api.libs.functional.syntax._


/*
 * Use this class as the type parameter for subclasses of
 * GroundSetAdapter, IndependenceAdapter, and MarginalValueAdapter.
 *
 * A VertexSet will get JSON serialized as it is passed between
 * AWSLambda, our StreamingGreedy service, and Redis.
 *
 * Represents a vertex of the Petersen graph.
 * Choices for the "vertices" parameter should be limited
 * between the strings "A" through "L"
 */
case class VertexSet(vertices: String) extends Element {
  override def toString(): String = {
    s"{vertices: ${this.vertices}}"
  }
  // override implicit val reads: Reads[Element] = Json.reads[VertexSet].asInstanceOf[Reads[Element]]
  // override implicit val Writes: Writes[Element] = Json.writes[VertexSet].asInstanceOf[Writes[Element]]
}


object VertexSet extends ElementCompanion[VertexSet]{
  override val reads: Reads[VertexSet] = Json.reads[VertexSet]
  override val writes: Writes[VertexSet] = Json.writes[VertexSet]
}

/*
 * For GroundSetIterator use only
 */
case class VertexLabel(label: String) extends L


object VertexLabel {
  def reads: Reads[VertexLabel] = Json.reads[VertexLabel]
  def writes: Writes[VertexLabel] = Json.writes[VertexLabel]

  def genIter: Iterator[VertexLabel] = {
    List("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L").map { v =>
      VertexLabel(v)
    }.iterator
  }
}


