package models

import slick.jdbc.MySQLProfile.api._
import java.sql.Date
import java.util.UUID
import scala.reflect.ClassTag

object APIJarModel {

  type APIJarSchema = (
    String,         // uuid
    Option[String], // jar_file_name
    Option[String]  // username
  )

  case class APIJarModel(
    uuid: String,
    jar_file_name: Option[String],
    username: Option[String]
  ) {

    override def toString(): String = {
      s"(uuid: $uuid, jar_file_name: $jar_file_name, $username)"
    }

  }

  def empty(): APIJarModel = APIJarModel(
    "",    // uuid
    None,  // jar_file_name
    None   // username
  )

  class APIJars(tag: Tag) extends Table[APIJarSchema](tag, "API_JAR") {
    def uuid = column[String]("UUID", O.PrimaryKey, O.SqlType("VARCHAR(128)"))
    def jarFileName = column[Option[String]]("JAR_FILE_NAME", O.SqlType("VARCHAR(256)"))
    def username = column[Option[String]]("USERNAME", O.SqlType("VARCHAR(256)"))

    def * = (uuid, jarFileName, username)
  }

  val apiJars = TableQuery[APIJars]

}