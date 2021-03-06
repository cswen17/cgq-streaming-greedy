package user_uploaded

import algorithm.E._
import graph.NodesIndependence._
import user_uploaded.thisisatest.graph.FunNodesIndependence._

object IndependenceAPIFactory {

  class IndependenceAPIFactory() {

    def apply(serialId: String): algorithm.E.IndependenceAPI[algorithm.E.Element] = {

      serialId match {
        case "GraphNodesIndependence" => new NodesIndependence().asInstanceOf[IndependenceAPI[Element]]
        case "FunNodesIndependence" => new FunNodesIndependence().asInstanceOf[IndependenceAPI[Element]]
      }

    }
        
  }

}
