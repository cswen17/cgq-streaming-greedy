package user_uploaded

import algorithm.E._
import graph.VertexLabelIterator._

object IteratorAPIFactory {

  class IteratorAPIFactory() {

    def apply(serialId: String): algorithm.E.IteratorAPI[algorithm.E.L] = {

      serialId match {
        case "GraphVertexLabelIterator" => new VertexLabelIterator().asInstanceOf[IteratorAPI[L]]
      }

    }
        
  }

}
