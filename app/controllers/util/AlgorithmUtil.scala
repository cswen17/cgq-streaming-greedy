package controllers.util

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc._

import algorithm.E._
import daos.PackagingDAO._
import user_uploaded.ElementCompanionFactory._
import validations.JobTokenValidations._


object AlgorithmUtil {
  case class StreamingGreedyRequest(element: Element, cacheKey: String)

  case class CachedUUIDRequest(cacheKey: String)

  class RequestUtils(companionSerialId: String) {

    val ecFactory: ElementCompanionFactory = new ElementCompanionFactory()
    val CompanionObject: ElementCompanion[Element] = ecFactory.apply(companionSerialId)

    val streamingGreedyRequestReads: Reads[StreamingGreedyRequest] = {
        implicit val eReads: Reads[Element] = CompanionObject.reads
        Json.reads[StreamingGreedyRequest]
    }
    val streamingGreedyRequestWrites: Writes[StreamingGreedyRequest] = {
        implicit val eWrites: Writes[Element] = CompanionObject.writes
        Json.writes[StreamingGreedyRequest]
    }

    val cachedUUIDRequestReads: Reads[CachedUUIDRequest] = Json.reads[CachedUUIDRequest]
    val cachedUUIDRequestWrites: Writes[CachedUUIDRequest] = Json.writes[CachedUUIDRequest]
  }

  case class InitializationsValidation(
    isValid: Boolean,
    groundSetAPISerialID: String,
    independenceAPISerialID: String,
    marginalValueAPISerialID: String,
    iteratorAPISerialID: String,
    elementCompanionSerialID: String)

  def validateRequest(
    request: Request[JsValue],
    jobTokenValidator: JobTokenModelValidator,
    packagingDAO: PackagingDAO): InitializationsValidation = {
    val (isValid, token): (Boolean, String) = prepareRequest(request, jobTokenValidator)
    // fetch serial IDs from a validator object
    val serialIds: SerialIDsPackage = packagingDAO.serialIdsFromJobTokenUUID(token)
    val (ground, independence, marginal, iterator, element): (
      String, String, String, String, String) = SerialIDsPackage.unapply(serialIds).get
    InitializationsValidation(
      isValid, ground, independence, marginal, iterator, element)
  }

  private def prepareRequest(
    request: Request[JsValue],
    jobTokenValidator: JobTokenModelValidator): (Boolean, String) = {
    
    val json: JsValue = request.body
    (json \ "token").validate[String] match {
      case success: JsSuccess[String] => {
        val token: String = success.get
        (jobTokenValidator.validate(token, None), token)
      }
      case error: JsError => {
        println(error)
        (false, "")
      }
    }
  }

}