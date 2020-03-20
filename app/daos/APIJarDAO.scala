package daos

import java.lang.ProcessBuilder
import java.util.UUID
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

import models.APIJarModel._

object APIJarDAO {

  class APIJarDAO @Inject()(
    @NamedDatabase("play") protected val dbConfigProvider: DatabaseConfigProvider
    ) extends HasDatabaseConfigProvider[JdbcProfile] {

    private val scriptName: String = "app/scripts/write-factory.sh"

    def createAPIJar(jarPath: String, username: String): String = {
      val uuid: String = UUID.randomUUID.toString
      val apiJarModel: APIJarModel = APIJarModel(uuid, Some(jarPath), Some(username))

      val apiJarSchema: APIJarSchema = APIJarModel.unapply(apiJarModel).get
      val apiJarAction = DBIO.seq(apiJars += apiJarSchema)
      val apiJarFuture = db.run(apiJarAction)
      val result: Unit = Await.result(apiJarFuture, Duration.Inf)

      uuid
    }

    def fetchAPIJar(uuid: String): APIJarModel = {
      val query = apiJars.filter(_.uuid === uuid)
      val action = query.result
      val future: Future[Seq[APIJarSchema]] = db.run(action)
      // get seq.last here
      val result: Seq[APIJarSchema] = Await.result(future, Duration.Inf)
      val apiJar: Option[APIJarSchema] = result.lastOption
      val model: APIJarModel = apiJar match {
        case Some(record) => APIJarModel.tupled(record)
        case None => empty()
      }
      model
    }

    def invokeWriteFactory(jarPath: String, username: String): Int = {
      val pb: ProcessBuilder = new ProcessBuilder(scriptName, jarPath, username)
      pb.start()
      0
    }

  }

}
