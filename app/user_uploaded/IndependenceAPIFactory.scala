package user_uploaded

import algorithm.E._
import graph.NodesIndependence._

object IndependenceAPIFactory {

  class IndependenceAPIFactory() {

    def apply(serialId: String): algorithm.E.IndependenceAPI[algorithm.E.Element] = {

      serialId match {
        case "GraphNodesIndependence" => new NodesIndependence().asInstanceOf[IndependenceAPI[Element]]
      }

    }
        
  }

}
