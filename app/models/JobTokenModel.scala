package models

import java.sql.Date
import java.util.UUID
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

import slick.jdbc.MySQLProfile.api._
import slick.lifted.Shape._


object JobTokenModel {

  type JobTokenSchema = (
    String, // uuid
    String, // packaging_uuid
    Option[String]  // streaming_greedy_uuid    
  )

  case class JobToken(
    uuid: String,
    packagingUUID: String,
    streamingGreedyUUID: Option[String]
  ) {
    def asTuple(): (JobToken, JobTokenSchema) = (this, JobToken.unapply(this).get)
  }

  def createJobToken(
    packagingUUID: String, streamingGreedyUUID: Option[String]): JobToken = {
    val uuid: String = UUID.randomUUID.toString
    val jobToken: JobToken = JobToken(uuid, packagingUUID, streamingGreedyUUID)
    jobToken
  }

  class JobTokens(tag: Tag) extends Table[JobTokenSchema](tag, "JOB_TOKEN") {
    val uuid = column[String]("UUID", O.PrimaryKey, O.SqlType("VARCHAR(128)"))
    val packagingUUID = column[String]("PACKAGING_UUID", O.SqlType("VARCHAR(128)"))
    val streamingGreedyUUID = column[Option[String]]("STREAMING_GREEDY_UUID", O.SqlType("VARCHAR(128)"))

    def * = (uuid, packagingUUID, streamingGreedyUUID)
  }

  val jobTokens = TableQuery[JobTokens]
}