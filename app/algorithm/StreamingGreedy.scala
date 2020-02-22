package algorithm

import java.util.UUID
import java.util.UUID._
import java.util.concurrent.atomic._
import play.api.libs.json._

import algorithm.E._ 
import algorithm.ExchangeCandidates._
import algorithm.impl.graph._
import algorithm.impl.graph.LogMarginalValue._
import algorithm.impl.graph.NodesGround._
import algorithm.impl.graph.NodesIndependence._
import clients.LambdaClient._
import clients.RedisSetClient._


object StreamingGreedy {
  /*
   * Warning: MAJOR CALCULATION IS MISSING! This impl is only half the algorithm!
   *
   * To Do: Implement 
   *   if f_S(e) >= \alpha + (1 + \beta)\sum_{c \in C}v(f, S, c):
   *     S <- S \diff C  + e
   *   return S
   */
  // Need a type parameter in StreamingGreedy class
  // End user configures job through web interface, selecting
  // which class to use for E, \alpha, \beta, which time they want
  // the job to run, job timeouts, job log level, and stream source
  // This class takes in the classes to use for E, GroundSetAdapter,
  // IndependenceAdapter, MarginalValueAdapter and starts the job
  class StreamingGreedy[E <: Element, L, GroundSetPayload <: GroundSetBase[E]]
    (implicit
      label: IteratorAPI[L],
      firstNode: GroundSetAPI[E, L, GroundSetPayload],
      independent: IndependenceAPI[E],
      marginalValue: MarginalValueAPI[E]) {
    // To Do: arg-ify alpha and beta
    val cacheKey: String = randomUUID.toString
    var redisKey: String = "S_ground_set_oracle_independence_set_oracle"

    val alpha: Double = 0.5
    val beta: Double = 0.5

    val id: AtomicInteger = new AtomicInteger(0)

    implicit val host: String = "localhost"
    implicit val port: Int =  6379

    // note: always construct a new IteratorAPI instance to reset the iterator
    type Label = IteratorAPI[L]
    type Ground = GroundSetAPI[E, L, GroundSetPayload]
    type Independence = IndependenceAPI[E]
    type MarginalValue = MarginalValueAPI[E]

    def run(stream: Iterator[E]): Set[E] = {
      //      val e2: E = E("BFJL")
      //      this.streamingGreedy(e2)
      //      val e3: E = E("ADFH")
      //      this.streamingGreedy(e3)
      //      val e4: E = E("ABCD")
      //      this.streamingGreedy(e4)
      //      val e1: E = E("ACEH")
      //      this.streamingGreedy(e1)
      stream.foreach { e =>
        streamingGreedy(e)
      }

      // val label: Label = new LIterator()
      // val firstNode: Ground = new NodesGround()
      // val independent: Independence = new NodesIndependence()
      // val marginalValue: MarginalValue = new LogMarginalValue()

      val groundKey: String = firstNode.awsLambdaName
      val independenceKey: String = independent.awsLambdaName
      val redisSKey: String = "S_" + groundKey + "_" + independenceKey

      val result: Set[E] = retrieveS(redisSKey, firstNode)
      result
    }

    def generateRedisKey(e: E, ground: Ground, independence: Independence): String = {
      val redisKeyBase: String = s"C_${ground.awsLambdaName}_${independence.awsLambdaName}"
      val exchangeCandidatesRunId: Int = this.id.getAndIncrement()
      val elementSuffix: String = s"_${e}_${exchangeCandidatesRunId}"
      val result: String = redisKeyBase + elementSuffix
      if (this.redisKey == "") {
        this.redisKey = result
      }
      result
    }

    /*
     * Method that gets called once per element pulled from stream
     * It runs one iteration of StreamingGreedy, which encompasses the
     * ExchangeCandidates subroutine and an additional calculation:
     *
     * ```
     * if f_S(e) >= \alpha + (1 + \beta)\sum_{c \in C}v(f, S, c):
     *     S <- S \diff C  + e
     *   return S
     * ```
     * To Do: For performance reasons, the result type of this
     * function should probably be Unit. But for debugging, let's
     * leave the result type as is
     */
    // All the E(s) need to be changed to something generic
    def streamingGreedy(e: E): Set[E] = {

      // val label: Label = new LIterator()
      // implicit val firstNode: Ground = new NodesGround()
      // implicit val independent: Independence = new NodesIndependence()
      // implicit val marginalValue: MarginalValue = new LogMarginalValue()

      type Candidate = ExchangeCandidates[L, E, GroundSetPayload]

      // Creates an ExchangeCandidates instance
      // implicit val groundSetAdapter: GroundSetAPI[E, L, GroundSetPayload] = firstNode
      // implicit val independentSetAdapter: IndependenceAPI[E] = independent
      // implicit val marginalValueAdapter: MarginalValueAPI[E] = marginalValue
      val run: Candidate = new Candidate(e, label)//, firstNode, independent)

      // Runs the ExchangeCandidates Subroutine
      // To Do: erase redisKeyName, it doesn't get used in ExchangeCandidates.
      // To Do: rethink redis key name throughout ExchangeCandidates and StreamingGreedy
      //   Each stream needs one unified Redis Key for S
      //   Each ExchangeCandidates instance needs one unified Redis Key for C
      val redisKey: String = this.generateRedisKey(e, firstNode, independent)
      val C: Set[E] = run.exchangeCandidates(redisKey)
      println("=======")
      println("C is: ")
      println(C)
      println("=======")

      // Retrieve S from Redis
      val groundKey: String = firstNode.awsLambdaName
      val independenceKey: String = independent.awsLambdaName
      val redisSKey: String = "S_" + groundKey + "_" + independenceKey

      // Expected answer is Empty set. Both elements should not be in Candidate Set
      println("Beginning to evaluate alpha and beta")
      val SThisIter: Set[E] = evaluateAlphaBeta(e, C, redisSKey, firstNode)

      println("=======")
      println("Result: ")
      println(SThisIter)
      println("=======")

      SThisIter
    }

    def retrieveSNoKey(): Set[E] = {
      // To Do: Implement me! (redisKey is a placeholder right now, please adjust)
      retrieveS(redisKey, firstNode)
    }

    private def retrieveS(redisSKey: String, groundSetAdapter: Ground): Set[E] = {
      val client: RedisSetClient = new RedisSetClient(redisSKey)
      val jsonSet: Set[JsValue] = client.readAllJson()
      val result: Set[E] = groundSetAdapter.toElementSet(jsonSet)
      println("=============")
      println("S is: ")
      println(result)
      println("=============")
      result
    }

    def evaluateAlphaBeta(e: E, C: Set[E], redisSKey: String, groundSetAdapter: Ground)(implicit marginalValue: MarginalValue): Set[E] = {
  
      // Calculate f_S(e) via one invoke
      val S: Set[E] = retrieveS(redisSKey, groundSetAdapter)
      val (submodularValue, reason): (Double, String) = valueOf(e, S, "f_S(e)")
      println(reason)

      // Calculate \alpha + (1 + \beta)\sum_{c \in C}v(f, S, c)
      //   by calculating \sum_{c \in C}v(f, S, c)
      //   by looping over each element of C and calling one invoke each loop iter
      val (incrementalValues, reasons): (Double, List[String]) = multiValueOf(C, S, "v(f, S, s)")
      reasons.foreach { reason => println(reason) }

      val rhs: Double = this.alpha + (1 + this.beta) * incrementalValues
      println(s"f_S(e): $submodularValue \talpha + (1 + beta) * sum of v(f, C, c): $rhs")

      if (submodularValue > rhs) {
        val resultS: Set[E] = calculateNextS(e, S, C, redisSKey, groundSetAdapter)
        resultS
      } else {
        val resultS: Set[E] = retrieveS(redisSKey, groundSetAdapter)
        resultS
      }
    }

    private def calculateNextS(
      e: E,
      S: Set[E],
      C: Set[E],
      redisSKey: String,
      groundSetAdapter: Ground): Set[E] = {
        val client: RedisSetClient = new RedisSetClient(redisSKey)

        val eJson: String = groundSetAdapter.toJsonString(Set(e)).last

        // S = S \ C + e
        val SCopy: Set[E] = retrieveS(redisSKey, groundSetAdapter)
        val SDiffC: Set[E] = S.diff(C)
        val elementsToRemove: Set[E] = S -- SDiffC

        // Maybe break this out into a separate function
        elementsToRemove.foreach { c =>
          // val cJsVal: JsValue = Json.toJson(c)
          // val cJson: String = Json.stringify(cJsVal)
          val cJson: String = groundSetAdapter.toJsonString(Set(c)).last
          val rResponse: RedisResponse = client.remove(cJson)
          rResponse.status match {
            case 0 => ()
            case 1 => {
              println(rResponse.body.get)
            }
          }
        }
        client.add(eJson)
        val resultS: Set[E] = retrieveS(redisSKey, groundSetAdapter)
        resultS
    }

    private def multiValueOf(C: Set[E], S: Set[E], valueType: String)(implicit marginalValue: MarginalValue): (Double, List[String]) = {
      println(s"Got into multiValue of for C: $C , S: $S, valueType $valueType")
      val invokedValues: Set[(Double, String)] = C.map { c => valueOf(c, S, valueType) }
      
      val multiValues: List[(Double, List[String])] = invokedValues.toList.scanLeft(0.0, List[String]()) { (cCurr, cNext) =>
        val marginalValue: Double = cCurr._1 + cNext._1
        val reasons: List[String] = cCurr._2 ++ List(cNext._2.toString)
        println(s"multiValueOf Result: ($marginalValue , $reasons )")
        Tuple2[Double, List[String]](marginalValue, reasons)
      }

      val result: (Double, List[String]) = multiValues.takeRight(1)(0)

      result
    }

    def valueOf(s: E, S: Set[E], valueType: String)(implicit marginalValue: MarginalValue): (Double, String) = {
      // Sets up a marginal value lambda, and runs it on s
      type MarginalValue = MarginalValueAPI[E]

      //val marginalValue: MarginalValue = new LogMarginalValue() // Needs to be generalized/converted to user input
      type Payload = marginalValue.RequestParams
      type Raw = marginalValue.ResponseBody

      val marginalValueName: String = marginalValue.awsLambdaName
      implicit val marginalValueReads: Reads[Payload] = marginalValue.requestReads
      implicit val marginalValueWrites: Writes[Payload] = marginalValue.requestWrites
      val marginalValueClient: LambdaClient[Payload] = {
        new LambdaClient[Payload](marginalValueName)
      }

      // set up payload
      val payload: Payload = marginalValue.marginalValue(s, S, valueType)
      println(payload)

      // set up the Raw by using MarginalValueAPI.LambdaResponse
      implicit val rawReads: Reads[Raw] = marginalValue.responseReads
      implicit val rawWrites: Writes[Raw] = marginalValue.responseWrites
      val valueResponse: Raw = marginalValueClient.invokeRaw[Raw](payload)
      val rawResponse: MarginalValueLambdaResponse = valueResponse.asInstanceOf[MarginalValueLambdaResponse]

      rawResponse.responseStatusCode() match {
        case 200 => (rawResponse.marginalValue(), rawResponse.notes())
        // Fixme: Maybe throw an exception in the future
        case _ => (rawResponse.marginalValue(), rawResponse.notes())
      }
    }
  }
}
