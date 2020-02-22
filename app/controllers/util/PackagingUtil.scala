package controllers.util

import java.util.UUID
import scala.collection._
import scala.collection.immutable.TreeMap

import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc._
import play.db.NamedDatabase
import slick.jdbc.MySQLProfile.api._
import slick.lifted.BaseColumnExtensionMethods

import models.JobTokenModel._
import models.StreamingGreedyJobModel._
import validations.JobTokenValidations._
import validations.PackagingValidations._


object PackagingUtil {

  val logger: Logger = Logger(this.getClass())

  val valuesList: List[String] = List(
    "ground_set_base_serial_id",
    "independence_api_serial_id",
    "marginal_value_serial_id",
    "iterator_api_serial_id",
    "element_companion_serial_id",
    "job_timeout",
    "redis_host",
    "redis_port",
    "lambda_ground_set_name",
    "lambda_independence_name",
    "lambda_marginal_value_name",
    "summary_redis_key_name",
    "aws_access_key_id",
    "aws_secret_key",
    "aws_sts_role_id_lambda",
    "aws_sts_role_id_elasticache"
  )

  /**
   * Given a multi-key-value-pair object in JSON, returns a Collection of config key value pairs
   * Example:
   *   {
   *      "uuid": "14137d43-51a3-4c9b-89b5-f6a5462afd78",
   *      "values": {
   *        "lambda_independence_name": "independent_vertex_set",
   *        "job_timeout": 23400,
   *        "ground_set_base_serial_id": "GSB4200399-14223-XL23"
   *      }
   *   }
   * Expect: To Do
   */
  type IS = (Option[Int], Option[String]) 
  def parseRequestJson(request: Request[JsValue]): (String, Map[String, IS]) = {

    val json: JsValue = request.body
    val uuid: String = (json \ "uuid").validate[String] match {
      case success: JsSuccess[String] => success.get
      case error: JsError => ""
    }
    val values: JsValue = (json \ "values").validate[JsValue] match {
      case success: JsSuccess[JsValue] => success.get
      case error: JsError => throw new Exception("No values field found in request")
    }
    val availableValuesOpt: List[Option[(String, IS)]] = valuesList.map { field =>
      val result: Option[(String, IS)] = (values \ field).asOpt[Int] match {
        case Some(result) => Some((field, (Some(result), None)))
        case None => (values \ field).asOpt[String] match {
          case Some(result) => Some((field, (None, Some(result))))
          case None => None
        }
      }
      result
    }
    val availableValues: List[(String, IS)] = {
      availableValuesOpt.filter { field => field != None }.map { opt => opt.get }
    }
    val resultMap: scala.collection.Map[String, IS] = availableValues.toMap
    (uuid, resultMap)
  }

  def forFieldString(fieldName: String, uuid: String) = {
    fieldName match {
      case "ground_set_base_serial_id" => for { job <- streamingGreedyJobs if job.uuid === uuid } yield job.groundSetBaseSerialId
      case "independence_api_serial_id" => for { job <- streamingGreedyJobs if job.uuid === uuid } yield job.independenceApiSerialId
      case "marginal_value_serial_id" => for { job <- streamingGreedyJobs if job.uuid === uuid } yield job.marginalValueSerialId
      case "iterator_api_serial_id" => for { job <- streamingGreedyJobs if job.uuid === uuid } yield job.iteratorApiSerialId
      case "element_companion_serial_id" => for { job <- streamingGreedyJobs if job.uuid === uuid } yield job.elementCompanionSerialId
      case "redis_host" => for { job <- streamingGreedyJobs if job.uuid === uuid } yield job.redisHost
      case "lambda_ground_set_name" => for { job <- streamingGreedyJobs if job.uuid === uuid } yield job.lambdaGroundSetName
      case "lambda_independence_name" => for { job <- streamingGreedyJobs if job.uuid === uuid } yield job.lambdaIndependenceName
      case "lambda_marginal_value_name" => for { job <- streamingGreedyJobs if job.uuid === uuid } yield job.lambdaMarginalValueName
      case "summary_redis_key_name" => for { job <- streamingGreedyJobs if job.uuid === uuid } yield job.summaryRedisKeyName
      case "aws_access_key_id" => for { job <- streamingGreedyJobs if job.uuid === uuid } yield job.awsAccessKeyId
      case "aws_secret_key" => for { job <- streamingGreedyJobs if job.uuid === uuid } yield job.awsSecretKey
      case "aws_sts_role_id_lambda" => for { job <- streamingGreedyJobs if job.uuid === uuid } yield job.awsStsRoleIdLambda
      case "aws_sts_role_id_elasticache" => for { job <- streamingGreedyJobs if job.uuid === uuid } yield job.awsStsRoleIdElasticache
      case _ => throw new Exception(s"No matching field for ${fieldName}")
    }
  }  

  def forFieldInt(fieldName: String, uuid: String) = {
    fieldName match {
      case "job_timeout" => for { job <- streamingGreedyJobs if job.uuid === uuid } yield job.jobTimeout
      case "redis_port" => for { job <- streamingGreedyJobs if job.uuid === uuid } yield job.redisPort
      case _ => throw new Exception(s"No matching field for ${fieldName}")
    }
  }

  def getValidationToken(
    request: Request[AnyContent],
    packagingValidator: PackagingModelValidator,
    jobTokenValidator: JobTokenModelValidator): (Boolean, String) = {

    val queryString: Map[String, Seq[String]] = request.queryString
    val uuid: String = queryString.getOrElse("uuid", Seq[String]("")).last

    val isValid: Boolean = packagingValidator.validateModel(uuid)
    isValid match {
      case false => (false, "")
      case true => {
        val tokenFromDatabase: String = databaseToken(uuid, jobTokenValidator)
        (true, tokenFromDatabase)
      }
    }
  }

  private def databaseToken(
    packagingUUID: String,
    jobTokenValidator: JobTokenModelValidator): String = {
    jobTokenValidator.createToken(packagingUUID)
  }
}