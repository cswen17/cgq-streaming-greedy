package user_uploaded

import algorithm.E._
import graph.LogMarginalValue._

object MarginalValueAPIFactory {

  class MarginalValueAPIFactory() {

    def apply(serialId: String): MarginalValueAPI[Element] = {

      serialId match {
        case "GraphLogMarginalValue" => new LogMarginalValue().asInstanceOf[MarginalValueAPI[Element]]
      }

    }
        
  }

}
