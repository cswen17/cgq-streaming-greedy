package controllers

import scala.concurrent._
import scala.concurrent.duration._

import java.nio.file.Paths
import javax.inject._
import play.api.Logger
import play.api._
import play.api.db.slick._
import play.api.libs.json._
import play.api.mvc._
import play.db.NamedDatabase
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta._

import models.JobTokenModel._
import models.StreamingGreedyJobModel._
import util.PackagingUtil._
import validations.JobTokenValidations._
import validations.PackagingValidations._


@Singleton
class PackagingController @Inject()(
  @NamedDatabase("play") protected val dbConfigProvider: DatabaseConfigProvider,
  cc: ControllerComponents)
  (implicit ec: ExecutionContext) extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] {

  val logger: Logger = Logger(this.getClass())

  private def initTable(): Unit = {
    val schemas = streamingGreedyJobs.schema ++ jobTokens.schema
    db.run(DBIO.seq(schemas.createIfNotExists))
    ()
  }

  def create_empty_package() = Action { implicit request: Request[AnyContent] =>
    initTable()

    val (basic, basicValues): (StreamingGreedyJob, StreamingGreedySchema) = emptyModel()

    val modelUUID: String = basic.uuid
    val insertFuture: Future[Unit] = db.run(DBIO.seq(streamingGreedyJobs += basicValues))
    // To Do: Change to async callbacks
    val result: Unit = Await.result(insertFuture, Duration.Inf)
    // To Do: Change to json
    Ok(modelUUID)
  }

  def view_database() = Action { implicit request: Request[AnyContent] =>
    // Fetches all StreamingGreedyJobModel objects
    val query = streamingGreedyJobs.map { job => job }
    val action = query.result
    val selectFuture: Future[Seq[StreamingGreedySchema]] = db.run(action)
    val result: Seq[StreamingGreedySchema] = Await.result(selectFuture, Duration.Inf)

    Ok(result.toString)
  }

  def add_config_key_value_pair() = Action(parse.json) { implicit request: Request[JsValue] =>
    initTable()

    // Change parseRequestJson to accept many config key pairs
    val (uuid, kvs): (String, scala.collection.Map[String, (Option[Int], Option[String])]) = parseRequestJson(request)
    kvs.keys.foreach { key =>
      key match {
        case "redis_port" => {
          val q: Query[Rep[Option[Int]],Option[Int],Seq] = forFieldInt(key, uuid)
          val updateValue = kvs(key).productElement(0).asInstanceOf[Option[Int]]
          val updateAction = q.update(updateValue)
          val updateFuture = db.run(updateAction)
          Await.result(updateFuture, Duration.Inf)
        }
        case "job_timeout" => {
          val q: Query[Rep[Option[Int]],Option[Int],Seq] = forFieldInt(key, uuid)
          val updateValue = kvs(key).productElement(0).asInstanceOf[Option[Int]]
          val updateAction = q.update(updateValue)
          val updateFuture = db.run(updateAction)
          Await.result(updateFuture, Duration.Inf)
        }
        case _ =>  {
          val q: Query[Rep[Option[String]],Option[String],Seq] = forFieldString(key, uuid)
          val updateValue = kvs(key).productElement(1).asInstanceOf[Option[String]]
          val updateAction = q.update(updateValue)
          val updateFuture = db.run(updateAction)
          Await.result(updateFuture, Duration.Inf)
        }
      }
    }
    Ok("OK!")
  }

  def validate() = Action { implicit request: Request[AnyContent] =>
    initTable()

    val packagingValidator: PackagingModelValidator = {
      new PackagingModelValidator(dbConfigProvider)
    }
    val jobTokenValidator: JobTokenModelValidator = {
      new JobTokenModelValidator(dbConfigProvider)
    }

    val (isValid, token): (Boolean, String) = {
      getValidationToken(request, packagingValidator, jobTokenValidator)
    }

    isValid match {
      case true => Ok(s"${token}\n")
      case false => BadRequest("400 Bad Request\n")
    }
  }

  def submit_jar() = Action(parse.temporaryFile) { implicit request =>
    request.body.moveTo(Paths.get("/tmp"), replace = true)
    Ok("To Do")
  }
}