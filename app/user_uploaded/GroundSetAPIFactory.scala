package user_uploaded

import algorithm.E._
import graph.NodesGround._

object GroundSetAPIFactory {

  class GroundSetAPIFactory() {

    def apply(serialId: String): algorithm.E.GroundSetAPI[algorithm.E.Element,algorithm.E.L,algorithm.E.GroundSetBase[algorithm.E.Element]] = {

      serialId match {
        case "GraphNodesGround" => new NodesGround().asInstanceOf[GroundSetAPI[Element, L, GroundSetBase[Element]]]
      }

    }
        
  }

}
