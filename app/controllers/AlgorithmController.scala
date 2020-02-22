 package controllers

import scala.concurrent._
import scala.concurrent.duration._

import javax.inject._
import play.api._
import play.api.{Logger}
import play.api.cache._
import play.api.db.slick._
import play.api.libs.json._
import play.api.mvc._
import play.db.NamedDatabase
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta._

import algorithm.E._
import algorithm.StreamingGreedy._
import algorithm.impl.graph._
import algorithm.impl.graph.LogMarginalValue._
import algorithm.impl.graph.NodesGround._
import algorithm.impl.graph.NodesIndependence._
import algorithm.impl.graph.VertexLabelIterator._
import daos.PackagingDAO._
import user_uploaded.GroundSetAPIFactory._
import user_uploaded.IndependenceAPIFactory._
import user_uploaded.IteratorAPIFactory._
import user_uploaded.MarginalValueAPIFactory._
import util.AlgorithmUtil._
import validations.JobTokenValidations._


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class AlgorithmController @Inject()(
  @NamedDatabase("play") protected val dbConfigProvider: DatabaseConfigProvider,
  cache: SyncCacheApi,
  cc: ControllerComponents
  ) extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] {

  val logger: Logger = Logger(this.getClass())

  // To Do: Wrap in some safety block elsewhere in case constructors don't work
  // in the future
  val gsFactory: GroundSetAPIFactory = new GroundSetAPIFactory()
  val inFactory: IndependenceAPIFactory = new IndependenceAPIFactory()
  val itFactory: IteratorAPIFactory = new IteratorAPIFactory()
  val mFactory: MarginalValueAPIFactory = new MarginalValueAPIFactory()

  private type GSAPI = GroundSetAPI[Element, L, GroundSetBase[Element]]
  private type INAPI = IndependenceAPI[Element]
  private type MVAPI = MarginalValueAPI[Element]
  private type ITAPI = IteratorAPI[L]
  private type SGE = StreamingGreedy[Element, L, GroundSetBase[Element]]

  /**
   * Initializes a StreamingGreedy object that uses the Graph implementation
   * Places the StreamingGreedy object in a cache
   * Returns the uuid of the StreamingGreedy object that got created
   */
  def init() = Action(parse.json) { implicit request: Request[JsValue] => 
      type JTMV = JobTokenModelValidator
      val validator: JTMV = new JobTokenModelValidator(dbConfigProvider)
      val dao: PackagingDAO = new PackagingDAO(dbConfigProvider)

      type IV = InitializationsValidation
      val vRequest: IV = validateRequest(request, validator, dao)

      if (vRequest.isValid) {
        // Initialize Streaming Greedy Components
        implicit val firstNode: GSAPI = gsFactory.apply(vRequest.groundSetAPISerialID)
        implicit val independent: INAPI = inFactory.apply(vRequest.independenceAPISerialID)
        implicit val marginal: MVAPI = mFactory.apply(vRequest.marginalValueAPISerialID)
        implicit val label: ITAPI = itFactory.apply(vRequest.iteratorAPISerialID)

        val job: SGE = new StreamingGreedy[Element, L, GroundSetBase[Element]]()
        val cacheKey: String = job.cacheKey
        val cacheVoid: Unit = cache.set(cacheKey, job)
        Created(cacheKey)
      } else {
        Forbidden(":(\n")
      }
  }

  /**
   * Retrieves the StreamingGreedy object asociated with the request's uuid
   * Extracts the stream element json from the request, converts it to the
   * configured API Element object, and calls StreamingGreedy#streamingGreedy
   * on it
   */
  def streaming_greedy() = Action { implicit request: Request[AnyContent] =>
    // To Do: Parse request headers for valid initializations token. Refuse the request
    // if the valid initializations token is not present

    // Parse request Body for cache key
    val requestUtils: RequestUtils = new RequestUtils("VertexSet")
    implicit val requestReads: Reads[StreamingGreedyRequest] = requestUtils.streamingGreedyRequestReads
    val requestContent: String = request.body.asText match {
      case Some(content: String) => content
      case None => "{}"
    }
    val raw: JsValue = Json.parse(requestContent)
    val streamingGreedyRequest: StreamingGreedyRequest = raw.validate[StreamingGreedyRequest] match {
      case success: JsSuccess[StreamingGreedyRequest] => success.get
      case error: JsError => {
        logger.error(s"There has been an error parsing $requestContent : $error")
        StreamingGreedyRequest(null, "")
      }
    }

    val e: Element = streamingGreedyRequest.element

    // Fetch Streaming Greedy
    val cacheKey: String = streamingGreedyRequest.cacheKey
    logger.info(s"The Cache Key is $cacheKey")
    val streamingGreedyOption: Option[SGE] = cache.get[SGE](cacheKey)
    val streamingGreedy: SGE = streamingGreedyOption.get

    val es: Set[Element] = streamingGreedy.streamingGreedy(e)
    logger.info(s"The result is ${es.toString}")
    Ok(es.toString)
  }

  /**
   * Retrieves the StreamingGreedy object associated with the request's uuid
   * Calls StreamingGreedy#retrieveS on it
   */
  def summary() = Action { implicit request: Request[AnyContent] =>
    val requestUtils: RequestUtils = {
      new RequestUtils("VertexSet")
    }
    implicit val requestReads: Reads[CachedUUIDRequest] = requestUtils.cachedUUIDRequestReads
    val requestParams: Map[String, Seq[String]] = request.queryString

    val cacheKey: String = requestParams.get("cacheKey") match {
      case Some(keys) => keys.last
      case None => ""
    }

    // Fetch Streaming Greedy
    val streamingGreedyOption: Option[SGE] = cache.get[SGE](cacheKey)
    val streamingGreedy: SGE = streamingGreedyOption.get

    // Fixme: retrieveSNoKey is currently hardcoded. See StreamingGreedy.scala
    val vs: Set[Element] = streamingGreedy.retrieveSNoKey()
    println(vs)
    Ok(vs.toString)
  }
}
