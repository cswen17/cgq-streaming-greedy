package clients

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.lambda.{
  AWSLambda, AWSLambdaClient, AWSLambdaClientBuilder}
import com.amazonaws.services.lambda.model.{
  AWSLambdaException, InvokeRequest, InvokeResult}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.Matchers._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import clients.LambdaClient._

class LambdaClientsSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar {

  case class TestPayload(test: String, payload: String)
  implicit val testWriter: Writes[TestPayload] = (
    (JsPath \ "test").write[String] and
    (JsPath \ "payload").write[String]
  )(unlift(TestPayload.unapply))
  implicit val testReader: Reads[TestPayload] = (
    (JsPath \ "test").read[String] and
    (JsPath \ "payload").read[String]
  )(TestPayload.apply _)

  case class GroundSetOraclePayload(sets: List[String], ground_set: String)
  implicit val groundSetWriter: Writes[GroundSetOraclePayload] = (
    (JsPath \ "sets").write[List[String]] and
    (JsPath \ "ground_set").write[String]
  )(unlift(GroundSetOraclePayload.unapply))
  implicit val groundSetReader: Reads[GroundSetOraclePayload] = (
    (JsPath \ "sets").read[List[String]] and
    (JsPath \ "ground_set").read[String]
  )(GroundSetOraclePayload.apply _)

  val GROUND_SET_ORACLE = "ground_set_oracle"
  val NONEXISTENT_LAMBDA_FUNCTION = "nonexistent_function_name"

  "LambdaClient client" should {
    "initialize an AWSLambda" in {

      // Setup
      val lambdaClient: LambdaClient[TestPayload] = new LambdaClient[TestPayload]("test_function")

      // Assertions
      val awsLambda = lambdaClient.client()
      awsLambda shouldBe a [AWSLambda]
    }
  }

  "LambdaClient invoke" should {
    "invoke ground_set_oracle lambda function successfully" in {
      // Assumes you have set up aws credentials with `aws configure`
      // Also need a ground_set_oracle lambda function
      cancel("there appears to be no internet connection")

      // Setup
      val lambdaClient: LambdaClient[GroundSetOraclePayload] = new LambdaClient[GroundSetOraclePayload](GROUND_SET_ORACLE)

      // Assertions
      val groundSetPayload: GroundSetOraclePayload = GroundSetOraclePayload(List("ACEL"), "A")
      val isGroundSetMember: Boolean = lambdaClient.invoke(groundSetPayload)
      isGroundSetMember shouldBe true

      val nonMemberPayload: GroundSetOraclePayload = GroundSetOraclePayload(List("ACEL"), "B")
      val isNonMember: Boolean = lambdaClient.invoke(nonMemberPayload)
      isNonMember shouldBe false
    }

    "return false when AWS error" in {
      cancel("there appears to be no internet connection")

      // Setup
      val lambdaClient: LambdaClient[TestPayload] = new LambdaClient[TestPayload](NONEXISTENT_LAMBDA_FUNCTION)

      // Assertions
      val testPayload: TestPayload = TestPayload("test", "payload")
      val result: Boolean = lambdaClient.invoke(testPayload)

      result shouldBe false
    }

    "return false when JSON error" in {
      cancel("there appears to be no internet connection")
      // Setup
      val lambdaMock = mock[AWSLambda]
      val invokeResult: InvokeResult = new InvokeResult()
      when(lambdaMock.invoke(any())).thenReturn(invokeResult)

      val lambdaClient: LambdaClient[TestPayload] = new LambdaClient[TestPayload](NONEXISTENT_LAMBDA_FUNCTION)

      val testPayload: TestPayload = TestPayload("test", "payload")
      val result: Boolean = lambdaClient.invoke(testPayload)
      result shouldBe false
    }
  }
}
