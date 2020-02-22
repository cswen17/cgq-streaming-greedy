package clients

import java.nio.ByteBuffer

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.lambda.{
  AWSLambda, AWSLambdaClient, AWSLambdaClientBuilder}
import com.amazonaws.services.lambda.model.{
  AWSLambdaException, InvokeRequest, InvokeResult}
import play.api.{Logger}
import play.api.libs.json._
import play.api.libs.functional.syntax._


/*
 * Class for managing Lambda function invocations
 *
 * S is the case class of the payload
 * To Do: Change the parameter name because S is also the name of the
 * result set in the StreamingGreedy algorithm
 */
object LambdaClient {
  // Json Read Model
  case class LambdaResult(statusCode: Int, body: Boolean, reason: String)

  class LambdaClient [S] (
    val functionName: String,
    val invocationType: String = "RequestResponse",
    val logType: String = "None",
    val qualifier: String = "$LATEST") (
      implicit val writes: Writes[S],
      implicit val reads: Reads[S]) {

    val logger: Logger = Logger(this.getClass())

    /*
     * Creates a client for AWS Lambda
     */
    def client(): AWSLambda = {
      val provider: ProfileCredentialsProvider = new ProfileCredentialsProvider()
      val client: AWSLambda = AWSLambdaClientBuilder
        .standard()
        .withCredentials(provider)
        .build()
      client
    }

    /*
     * Invokes a lambda function assuming the result is a boolean
     */
    def invoke(payload: S): Boolean = {

      logger.info(s"----LambdaClient begin invoke for function: ${this.functionName}")

      val payloadJson: JsValue = Json.toJson(payload)
      val payloadString: String = Json.stringify(payloadJson)

      logger.info(s"\t\tPayload: $payloadString")

      // Assumption: Invoke Request params are configured at class init
      var invokeRequest: InvokeRequest = new InvokeRequest()
      invokeRequest = invokeRequest
        .withFunctionName(this.functionName)
        .withInvocationType(this.invocationType)
        .withLogType(this.logType)
        .withPayload(payloadString)

      logger.info(s"\t\tSuccessfully constructed InvokeRequest object to AWS: $invokeRequest")

      val client: AWSLambda = this.client()
      try {
        // To Do: Timing variables, with logging or metrics
        logger.info("\t\tStarting request to AWS Lambda...")
        val response: InvokeResult = client.invoke(invokeRequest)
        logger.info(s"\t\tSuccess. AWS Lambda response received: $response")

        // Convert InvokeResult payload ByteBuffer to model
        // Lambda Result Reader
        implicit val lambdaResultReader: Reads[LambdaResult] = (
          (JsPath \ "statusCode").read[Int] and
          (JsPath \ "body").read[Boolean] and
          (JsPath \ "reason").read[String]
        ) (LambdaResult.apply _)

        // Json Prep
        logger.info("\t\tBeginning response conversion...")
        val buffer: ByteBuffer = response.getPayload()
        val bytes: Array[Byte] = buffer.array()
        val string: String = new String(bytes)
        logger.info(s"\t\tSuccess. Response payload as string evaluates to $string")

        // Result extraction
        val json: JsValue = Json.parse(string)
        json.validate[LambdaResult] match {
          case success: JsSuccess[LambdaResult] => {
            val lambdaResult: LambdaResult = success.get
            val result: Boolean = lambdaResult.body
            val reason: String = lambdaResult.reason
            logger.info(s"\t\tReturning $result with reason $reason")
            result
          }
          case error: JsError => {
            logger.warn(error.toString)
            return false
          }
        }

      } catch {
        case le: AWSLambdaException => {
          logger.warn(le.toString)
          return false
        }
      }
    }

    /*
     * Invokes a lambda function and returns the raw result
     */
    def invokeRaw[Raw](payload: S)(
        implicit reader: Reads[Raw], writer: Writes[Raw]): Raw = {
      // Write a function that calls the lambda,
      // uses the implicit reader on the result
      // and returns the result
      val payloadJson: JsValue = Json.toJson(payload)
      val payloadString: String = Json.stringify(payloadJson)

      var invokeRequest: InvokeRequest = new InvokeRequest()
      invokeRequest = invokeRequest
        .withFunctionName(this.functionName)
        .withInvocationType(this.invocationType)
        .withLogType(this.logType)
        .withPayload(payloadString)

      val client: AWSLambda = this.client()
      try {
        val response: InvokeResult = client.invoke(invokeRequest)

        // Json Prep
        val buffer: ByteBuffer = response.getPayload()
        val bytes: Array[Byte] = buffer.array()
        val string: String = new String(bytes)

        // Result extraction
        val json: JsValue = Json.parse(string)
        json.validate[Raw] match {
          case success: JsSuccess[Raw] => {
            val result: Raw = success.get
            logger.info("Returning "+result)
            result
          }
          case error: JsError => {
            // To Do: Logging
            logger.warn(error.toString)
            throw new Exception(error.toString)
          }
        }
      } catch {
        case le: AWSLambdaException => {
          // To Do: Logging
          logger.warn(le.toString)
          throw new Exception(le.toString)
        }
      }
    }

  }

}
