package user_uploaded

import algorithm.E._
import graph._

object ElementCompanionFactory {

  class ElementCompanionFactory() {

    def apply(serialId: String): algorithm.E.ElementCompanion[Element] = {

      serialId match {
        case "VertexSet" => VertexSet.asInstanceOf[ElementCompanion[Element]]
      }

    }
        
  }

}
