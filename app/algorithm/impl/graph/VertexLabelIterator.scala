package algorithm.impl.graph

import algorithm.E._


object VertexLabelIterator {
  class VertexLabelIterator extends IteratorAPI[VertexLabel] {
    // Used in the ExchangeCandidates class.
    override val groundSetIterator: Iterator[VertexLabel] = VertexLabel.genIter 
  }
}
