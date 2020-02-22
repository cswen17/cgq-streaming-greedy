package validations

import javax.inject._
import scala.concurrent._
import scala.concurrent.duration._

import play.api.db.slick._
import play.db.NamedDatabase
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta._

import models.JobTokenModel._

object JobTokenValidations {

  class JobTokenModelValidator @Inject()
    (@NamedDatabase("play") protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[JdbcProfile] {

    def validate(uuid: String, packagingUUID: Option[String]): Boolean = {
      val query = jobTokens.filter(_.uuid === uuid)
      val action = query.result
      val future: Future[Seq[JobTokenSchema]] = db.run(action)
      val records: Seq[JobTokenSchema] = Await.result(future, Duration.Inf)
      val record: Option[JobTokenSchema] = records.lastOption
      println(s"While validating token, found record ${record}")
      record match {
        case None => false
        case Some(tupledArgs) => {
          val jobToken: JobToken = JobToken.tupled(tupledArgs)
          val recPackagingUUID: String = jobToken.packagingUUID
          val isValid: Boolean = packagingUUID match {
            case None => recPackagingUUID != ""
            case Some(expectedUUID: String) => recPackagingUUID == expectedUUID
          }
          println(s"""
            Fetched ${recPackagingUUID} from the database.
            Expect ${packagingUUID}. Result is ${isValid}""")
          isValid
        }
      }
    }

    def createToken(packagingUUID: String): String = {
      val (jobToken, record): (JobToken, JobTokenSchema) = createJobToken(packagingUUID, None).asTuple()
      val future: Future[Unit] = db.run(DBIO.seq(jobTokens += record))
      Await.result(future, Duration.Inf)
      jobToken.uuid
    }
  }

}