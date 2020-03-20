package utils

import java.lang.System._
import java.nio.file.{Path, Paths}
import java.util.Properties
import scala.tools.nsc.io.{Path => ScalaPath}


object PathUtils {

  val CWD_SYS_PROP = "user.dir"
  val INTERMEDIARY_HOLDING = rootRel(List[String](
    "app", "user_uploaded", "intermediary_holding"))

	def projectRoot(): Path = {
    val systemProperties: Properties = getProperties()
    val cwd: String = systemProperties.getProperty(CWD_SYS_PROP)
    Paths.get(cwd)
	}

  def rootRel(pathComponents: List[String]): ScalaPath = {
    val root: String = projectRoot().toString
    val asScalaPath: ScalaPath = ScalaPath(root)
    pathComponents.map(c => ScalaPath(c)).scanLeft(asScalaPath){ (p, c) =>
      p / c
    }.last
  }

}