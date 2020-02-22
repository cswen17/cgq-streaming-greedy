package daos

import javax.inject._
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration

import play.api.db.slick._
import play.api.libs.json._
import play.api.mvc._
import play.db.NamedDatabase
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta._

import models.JobTokenModel._
import models.StreamingGreedyJobModel._

object PackagingDAO {

  case class SerialIDsPackage(
    groundSetAPISerialId: String,
    independenceAPISerialId: String,
    marginalValueAPISerialId: String,
    iteratorAPISerialId: String,
    elementCompanionSeriaId: String)

  object SerialIDsPackage {
    def empty(): SerialIDsPackage = {
      SerialIDsPackage(
        "", // groundSetAPISerialId
        "", // independenceAPISerialId
        "", // marginalValueAPISerialId
        "", // iteratorAPISerialId
        ""  // elementCompanionSerialId
      )
    }

    def fromPackaging(model: StreamingGreedyJob): SerialIDsPackage = {
      val groundSetAPISerialID: String = model.ground_set_base_serial_id.get
      val independenceAPISerialID: String = model.independence_api_serial_id.get
      val marginalValueAPISerialID: String = model.marginal_value_serial_id.get
      val iteratorAPISerialID: String = model.iterator_api_serial_id.get
      val elementCompanionSerialID: String = model.element_companion_serial_id.get
      val result: SerialIDsPackage = SerialIDsPackage(
        groundSetAPISerialID,
        independenceAPISerialID,
        marginalValueAPISerialID,
        iteratorAPISerialID,
        elementCompanionSerialID
      )
      result
    }
  }

  class PackagingDAO @Inject()
    (@NamedDatabase("play")
      protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[JdbcProfile] {

    def serialIdsFromJobTokenUUID(jobTokenUUID: String): SerialIDsPackage = {
      val jobTokenQuery = jobTokens.filter(_.uuid === jobTokenUUID)
      val jobTokenAction = jobTokenQuery.result
      val jobTokenFuture: Future[Seq[JobTokenSchema]] = db.run(jobTokenAction)
      val jobTokenRecords: Seq[JobTokenSchema] = Await.result(jobTokenFuture, Duration.Inf)
      val jobTokenRecord: Option[JobTokenSchema] = jobTokenRecords.lastOption
      val packagingUUID: String = jobTokenRecord match {
        case None => ""
        case Some(record) => {
          val jobToken: JobToken = JobToken.tupled(record)
          jobToken.packagingUUID
        }
      }

      val packagingQuery = streamingGreedyJobs.filter(_.uuid === packagingUUID)
      val packagingAction = packagingQuery.result
      val packagingFuture: Future[Seq[StreamingGreedySchema]] = db.run(packagingAction)
      val packagingRecords: Seq[StreamingGreedySchema] = Await.result(packagingFuture, Duration.Inf)
      val packagingRecord: Option[StreamingGreedySchema] = packagingRecords.lastOption
      val result: SerialIDsPackage = packagingRecord match {
        case None => SerialIDsPackage.empty()
        case Some(job) => {
          val model: StreamingGreedyJob = StreamingGreedyJob.tupled(job)
          SerialIDsPackage(
            model.ground_set_base_serial_id.get,
            model.independence_api_serial_id.get,
            model.marginal_value_serial_id.get,
            model.iterator_api_serial_id.get,
            model.element_companion_serial_id.get
          )
        }
      }
      result
    }

  }
}