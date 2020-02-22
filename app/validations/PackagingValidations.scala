package validations

import java.lang.RuntimeException
import java.net.{ConnectException, NoRouteToHostException}
import javax.inject._
import scala.concurrent._
import scala.concurrent.duration._

import play.api.db.slick._
import play.db.NamedDatabase
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import clients.RedisSetClient._
import models.StreamingGreedyJobModel._

object PackagingValidations {
  /**
    1. groundSetBaseSerialId,
    2. independenceApiSerialId,
    3. marginalValueSerialId,
    4. jobTimeout,
    5. redisHost,
    6. redisPort,
    7. lambdaGroundSetName,
    8. lambdaIndependenceName,
    9. lambdaMarginalValueName,
    10. summaryRedisKeyName,
    11. awsAccessKeyId,
    12. awsSecretKey,
    13. awsStsRoleIdLambda,
    14. awsStsRoleIdElasticache
  */
  // 1, 2, and 3 are required

  // 5, 6 are required
  // 5, 6 must be able to construct a RedisSetClient with no exceptions ( try catch )
  // 10 is required. 10 must be unique ( filter )

  // 7, 8, and 9 are required
  // if 11 or 12 is given, then 11 and 12 are required
  // if 13 or 14 is given, then 13 and 14 are required
  // To Do: Advanced AWS Client validation
  //    i.e. if 11 or 12 is given, 7, 8, and 9 must be able to construct a valid LambdaClient
  //         if 13 or 14 is given, 7, 8, and 9 must be able to construct a vaild LambdaClient
  class PackagingModelValidator @Inject()(
    @NamedDatabase("play") protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[JdbcProfile] {

    def validateModel(uuid: String): Boolean = {
      val record: Option[StreamingGreedySchema] = fetchModel(uuid)
      val model: StreamingGreedyJob = record match {
        case Some(rec) => StreamingGreedyJob.tupled(rec)
        case None => return false
      }

      val api: Boolean = validateAPI(model) 
      val configs: Boolean = {
        validateRedisConfig(model) && validateLambdaConfig(model)
      }
      val optionals: Boolean = validateUserGivenAWSCreds(model)
      api && configs && optionals
    }

    private def fetchModel(uuid: String): Option[StreamingGreedySchema] = {
      val action = streamingGreedyJobs.filter(_.uuid === uuid).result
      val future: Future[Seq[StreamingGreedySchema]] = db.run(action)
      val result: Seq[StreamingGreedySchema] = Await.result(future, Duration.Inf)
      result.lastOption
    }

    // 1, 2, and 3 are required
    def validateAPI(model: StreamingGreedyJob): Boolean = {
      val ground: Boolean = model.ground_set_base_serial_id.get != ""
      val independent: Boolean = model.independence_api_serial_id.get != ""
      val marginal: Boolean = model.marginal_value_serial_id.get != ""
      val iterator: Boolean = model.iterator_api_serial_id.get != ""
      val element: Boolean = model.element_companion_serial_id.get != ""

      ground && independent && marginal && iterator && element
    }

    // 5, 6 are required
    // 5, 6 must be able to construct a RedisSetClient with no exceptions ( try catch )
    // 10 is required. 10 must be unique ( filter )
    def validateRedisConfig(model: StreamingGreedyJob): Boolean = {
      val host: Boolean = model.redis_host.get != ""
      val port: Boolean = model.redis_port.get > 0
      val key: Boolean = model.summary_redis_key_name.get != ""
      implicit val redisHost: String = model.redis_host.get
      implicit val redisPort: Int = model.redis_port.get
      try {
        val redisKey: String = model.summary_redis_key_name.getOrElse("")
        val client: RedisSetClient = new RedisSetClient(redisKey)
        host && port && key
      } catch {
        case ce: ConnectException => false
        case nre: NoRouteToHostException => false
        case re: RuntimeException => false
      }
    }

    // 7, 8, and 9 are required
    def validateLambdaConfig(model: StreamingGreedyJob): Boolean = {
      val gLambda: Boolean = model.lambda_ground_set_name.get != ""
      val iLambda: Boolean = model.lambda_independence_name.get != ""
      val mLambda: Boolean = model.lambda_marginal_value_name.get != ""
      gLambda && iLambda && mLambda
    }

    // if 11 or 12 is given, then 11 and 12 are required
    // if 13 or 14 is given, then 13 and 14 are required
    def validateUserGivenAWSCreds(model: StreamingGreedyJob): Boolean = {
      validateAwsKeys(model) && validateStsRoles(model)
    }

    private def hasAwsAccessKeyId(key: Option[String]): Boolean = {
      key match {
        case Some(awsAccessKeyId) => true
        case None => false
      }
    }

    private def hasAwsSecretKey(key: Option[String]): Boolean = {
      key match {
        case Some(awsSecretKey) => true
        case  None => false
      }
    }

    private def areAwsKeysValid(model: StreamingGreedyJob): Boolean = {
      val isAccessKeyIdValid: Boolean = model.aws_access_key_id.getOrElse("") != ""
      val isSecretKeyValid: Boolean = model.aws_secret_key.getOrElse("") != ""
      isAccessKeyIdValid && isSecretKeyValid
    }

    private def validateAwsKeys(model: StreamingGreedyJob): Boolean = {
      val hasAccessKeyId: Boolean = hasAwsAccessKeyId(model.aws_access_key_id)
      val hasSecretKey: Boolean = hasAwsSecretKey(model.aws_secret_key)
      val isValid: Boolean = {
        if (hasAccessKeyId || hasSecretKey) areAwsKeysValid(model) else true
      }
      isValid
    }

    private def hasLambdaStsRole(key: Option[String]): Boolean = {
      key match {
        case Some(lambdaStsRole) => true
        case None => false
      }
    }

    private def hasElasticacheStsRole(key: Option[String]): Boolean = {
      key match {
        case Some(elasticacheRole) => true
        case None => false
      }
    }

    private def areStsRolesValid(model: StreamingGreedyJob): Boolean = {
      val isLambdaValid: Boolean = model.aws_sts_role_id_lambda.getOrElse("") != ""
      val isElasticacheValid: Boolean = model.aws_sts_role_id_elasticache.getOrElse("") != ""
      isLambdaValid && isElasticacheValid
    }

    private def validateStsRoles(model: StreamingGreedyJob): Boolean = {
      val hasLambda: Boolean = hasLambdaStsRole(model.aws_sts_role_id_lambda)
      val elasticache: Option[String] = model.aws_sts_role_id_elasticache
      val hasElasticache: Boolean = hasElasticacheStsRole(elasticache)
      val isValid: Boolean = {
        if (hasLambda || hasElasticache) areStsRolesValid(model) else true
      }
      isValid
    }
  }
}