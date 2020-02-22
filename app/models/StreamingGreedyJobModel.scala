package models

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import slick.jdbc.MySQLProfile.api._
import java.sql.Date
import java.util.UUID
import scala.reflect.ClassTag

object StreamingGreedyJobModel {
  // private type UUID = java.util.UUID

  type StreamingGreedySchema = (
    String, // uuid
    Option[String], // ground_set_base_serial_id
    Option[String], // independence_api_serial_id
    Option[String], // marginal_value_serial_id
    Option[String], // iterator_api_serial_id
    Option[String], // element_companion_serial_id
    Option[Int],  // job_timeout
    Option[String],  // redis_host
    Option[Int],  // redis_port
    Option[String],  // lambda_ground_set_name
    Option[String],  // lambda_independence_name
    Option[String],  // lambda_marginal_value_name
    Option[String],  // summary_redis_key_name
    Option[String],  // aws_access_key_id
    Option[String],  // aws_secret_key
    Option[String],  // aws_sts_role_id_lambda
    Option[String]  // aws_sts_role_id_elasticache
  )

  def emptyModel(): (StreamingGreedyJob, StreamingGreedySchema) = {
    val emptyJob: StreamingGreedyJob = StreamingGreedyJob(
      UUID.randomUUID.toString,  // uuid
      None,  // ground_set_base_serial_id
      None,  // independence_api_serial_id
      None,  // marginal_value_serial_id
      None,  // iterator_api_serial_id
      None,  // element_companion_serial_id
      None,  // job_timeout
      None,  // redis_host
      None,  // redis_port
      None,  // lambda_ground_set_name
      None,  // lambda_independence_name
      None,  // lambda_marginal_value_name
      None,  // summary_redis_key_name
      None,  // aws_access_key_id
      None,  // aws_secret_key
      None,  // aws_sts_role_id_lambda
      None  // aws_sts_role_id_elasticache
    )
    (emptyJob, emptyJob.asTuple())
  }

  case class StreamingGreedyJob(
    uuid: String,
    ground_set_base_serial_id: Option[String],
    independence_api_serial_id: Option[String],
    marginal_value_serial_id: Option[String],
    iterator_api_serial_id: Option[String],
    element_companion_serial_id: Option[String],
    job_timeout: Option[Int],
    redis_host: Option[String],
    redis_port: Option[Int],
    lambda_ground_set_name: Option[String],
    lambda_independence_name: Option[String],
    lambda_marginal_value_name: Option[String],
    summary_redis_key_name: Option[String],
    aws_access_key_id: Option[String],
    aws_secret_key: Option[String],
    aws_sts_role_id_lambda: Option[String],
    aws_sts_role_id_elasticache: Option[String]
  ) {
    def asTuple(): StreamingGreedySchema = {
      (
        uuid,
        ground_set_base_serial_id,
        independence_api_serial_id,
        marginal_value_serial_id,
        iterator_api_serial_id,
        element_companion_serial_id,
        job_timeout,
        redis_host,
        redis_port,
        lambda_ground_set_name,
        lambda_independence_name,
        lambda_marginal_value_name,
        summary_redis_key_name,
        aws_access_key_id,
        aws_secret_key,
        aws_sts_role_id_lambda,
        aws_sts_role_id_elasticache
      )
    }
  }

  class StreamingGreedyJobs(tag: Tag) extends Table[StreamingGreedySchema](tag, "STREAMING_GREEDY_JOB"){
    def uuid = column[String]("UUID", O.PrimaryKey, O.SqlType("VARCHAR(128)"))
    def groundSetBaseSerialId = column[Option[String]]("GROUND_SET_BASE_SERIAL_ID", O.SqlType("VARCHAR(256)"))
    def independenceApiSerialId = column[Option[String]]("INDEPENDENCE_API_SERIAL_ID", O.SqlType("VARCHAR(256)"))
    def marginalValueSerialId = column[Option[String]]("MARGINAL_VALUE_SERIAL_ID", O.SqlType("VARCHAR(256)"))
    def iteratorApiSerialId = column[Option[String]]("ITERATOR_API_SERIAL_ID", O.SqlType("VARCHAR(256)"))
    def elementCompanionSerialId = column[Option[String]]("ELEMENT_COMPANION_SERIAL_ID", O.SqlType("VARCHAR(256)"))
    def jobTimeout = column[Option[Int]]("JOB_TIMEOUT")
    def redisHost = column[Option[String]]("REDIS_HOST", O.SqlType("VARCHAR(256)"))
    def redisPort = column[Option[Int]]("REDIS_PORT")
    def lambdaGroundSetName = column[Option[String]]("LAMBDA_GROUND_SET_NAME", O.SqlType("VARCHAR(256)"))
    def lambdaIndependenceName = column[Option[String]]("LAMBDA_INDEPENDENCE_NAME", O.SqlType("VARCHAR(256)"))
    def lambdaMarginalValueName = column[Option[String]]("LAMBDA_MARGINAL_VALUE_NAME", O.SqlType("VARCHAR(256)"))
    def summaryRedisKeyName = column[Option[String]]("SUMMARY_REDIS_KEY_NAME", O.SqlType("VARCHAR(256)"))
    def awsAccessKeyId = column[Option[String]]("AWS_ACCESS_KEY_ID", O.SqlType("VARCHAR(256)"))
    def awsSecretKey = column[Option[String]]("AWS_SECRET_KEY", O.SqlType("VARCHAR(256)"))
    def awsStsRoleIdLambda = column[Option[String]]("AWS_STS_ROLE_ID_LAMBDA", O.SqlType("VARCHAR(256)"))
    def awsStsRoleIdElasticache = column[Option[String]]("AWS_STS_ROLE_ID_ELASTICACHE", O.SqlType("VARCHAR(256)"))

    // val sgjtpl = StreamingGreedyJob.tupled _
    def * = (
      uuid,
      groundSetBaseSerialId,
      independenceApiSerialId,
      marginalValueSerialId,
      iteratorApiSerialId,
      elementCompanionSerialId,
      jobTimeout,
      redisHost,
      redisPort,
      lambdaGroundSetName,
      lambdaIndependenceName,
      lambdaMarginalValueName,
      summaryRedisKeyName,
      awsAccessKeyId,
      awsSecretKey,
      awsStsRoleIdLambda,
      awsStsRoleIdElasticache
    )

  }

  val streamingGreedyJobs = TableQuery[StreamingGreedyJobs]
}