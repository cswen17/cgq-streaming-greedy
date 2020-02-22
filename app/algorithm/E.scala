package algorithm

import play.api.libs.json._


/**
 * Module for interfaces and contracts
 * Helps to generalize calculations to any client
 * Parts Covered:
 *   - Membership in N_l
 *   - Membership in I_l
 *   - v(f, S, e)
 */
object E {

  /*
   * Trait for e
   */
  trait Element {
    override def toString: String
    // implicit val reads: Reads[Element]
    // implicit val writes: Writes[Element]
  }

  trait ElementCompanion[E <: Element] {
    val reads: Reads[E]
    val writes: Writes[E]
  }


  /*
   * Trait for L
   */
  trait L


  /*
   * Constraints/methods for GroundSetPayload
   */
  abstract class GroundSetBase[E <: Element] {
    // To Do: Value purpose
    val payloadSet: E
  }

  /* 
   * Basic Lambda options class
   *
   * @required awsLambdaName The name of the lambda function
   */
  abstract class LambdaConfig {
    // *************************
    // * Override this please  *
    // *************************
    val awsLambdaName: String
    // **** End Overrides **** // 

    // Default values (optional for override)
    val invocationType: String = "RequestResponse"
    val logType: String = "None"
    val qualifier: String = "$LATEST"
  }


  /*
   * Class for Iterator-related access
   */
  abstract class IteratorAPI[L] {
    // L is supposed to be the type of the Ground Set Label
    // The Ground Set Label is the output of the iterator, i.e.
    // l in l=1,...,q. Although l is supposed to be an int, we
    // allow l to be other types for flexibility, e.g. Strings
    // like A, B, C, D, E,..., as long as there are q total items
    val groundSetIterator: Iterator[L]
  }


  /** Requirements for user classes to invoke valid Ground Set oracles
   *
   *  Subclasses of this class facilitate communication with AWS Lambda 
   *  to invoke a user supplied Ground Set oracle function, which is
   *  necessary to determine if an element in consideration is validly part
   *  of ground set, which is basically like the set of all possible members,
   *  of the underlying p-matchoid. Ground Set oracle functions should be
   *  more like simple validation functions, for sanity. A simple
   *  example is prices of a stock, which just needs to be checked that
   *  it's a positive number to 2 decimal places.
   *
   *  The user needs to supply AWS Lambda with their own Ground Set
   *  function beforehand. The AWS Lambda function can be written in any
   *  language supported by AWS Lambda, as long as the function accesses the
   *  same parameters as the RequestParams type required by this interface
   *
   *  In addition to the members of this interface, subclasses of this class
   *  must also remember to override @val awsLambdaName: String, which must
   *  match the name of the Marginal Value function in the AWS Lambda console.
   *  You wouldn't want to run the wrong lambda function after all your hard
   *  work!
   *
   *  How to subclass:
   *    - Write a class containing members for each parameter to be submitted
   *      in the request to AWS Lambda.
   *      For example, if your Ground Set oracle function uses
   *      a magic_square parameter, then your class should look like:
   *      ```
   *        case class MagicSquareGroundSet(magic_square: Array[Array[Int]])
   *      ```
   *
   *    - Type alias above-written class to the provided RequestParams type
   *
   *    - Write JSON serializer methods for above-written classes by
   *      overriding the provided requestReads, requestWrites, methods. 
   *      For how to construct JSON serializer methods,
   *      @see <a href="
   *        https://github.com/playframework/play-json#automatic-conversion">
   *        play-json</a>
   *
   *    - Override remaining methods: Write conversions to interpret the givens
   *      from the ExchangeCandidates subroutine (a work driving component of
   *      the overall algorithm) and transform their values into a
   *      RequestParams. These conversions will be called in ExchangeCandidates
   *      whenever Ground Set membership needs to be evaluated.
   *
   *  Example:
   *    - See algorithm.impl.graph.NodesGround._ for a comformant
   *      implementation
   *
   *  @tparam E type of the element from the input stream. Users should
   *            define this type at the top of their respective impl
   *            package and then import it into the subclass declaration
   */
  abstract class GroundSetAPI[E <: Element, L, GroundSet] extends LambdaConfig {

    type RequestParams = GroundSet

    /** Helper function to find whether e \in N_l, the l-th Ground Set
     *  Partition of the underlying p-matchoid
     */
    def loadEInN_L(e: E, S: Set[E], l: L): RequestParams

    /**
     * Finds (S + e) \intersect N_l via filter
     *
     * The result of this function will be iterated over, and there will be one
     * call to AWSLambda.invoke per iterated GroundSetPayload. Then the invoke
     * results that returned true will be kept for the final set.
     */
    def loadSPlusEInN_L(
      e: E, S: Set[E], l: L): List[RequestParams]

    /** Helper function to find S \intersect N_l via filter */
    def loadS_L(
      e: E, S: Set[E], l: L): List[RequestParams]

    /* JSON Serialization helper functions */
    def requestReads: Reads[RequestParams]
    def requestWrites: Writes[RequestParams]

    /* JSON Serialization helper functions for Redis */
    def toElementSet(S: Set[JsValue]): Set[E]
    def toJsonString(S: Set[E]): Set[String]
  }

  /** Requirements for user classes to invoke valid Independence Set oracles
   *
   *  Subclasses of this class facilitate communication with AWS Lambda 
   *  to invoke a user supplied Independence Set oracle function, which is
   *  necessary to determine if an element in consideration is validly part
   *  of the independence set of the underlying p-matchoid.
   *
   *  The user needs to supply AWS Lambda with their own Independence Set
   *  function beforehand. The AWS Lambda function can be written in any
   *  language supported by AWS Lambda, as long as the function accesses the
   *  same parameters as the RequestParams type required by this interface
   *
   *  In addition to the members of this interface, subclasses of this class
   *  must also remember to override @val awsLambdaName: String, which must
   *  match the name of the Marginal Value function in the AWS Lambda console.
   *  You wouldn't want to run the wrong lambda function after all your hard
   *  work!
   *
   *  How to subclass:
   *    - Write a class containing members for each parameter to be submitted
   *      in the request to AWS Lambda.
   *      For example, if your Independence Set oracle function uses
   *      a matrix_column and matrix parameter, then your class
   *      should look like:
   *      ```
   *        case class LinearIndependenceSet(
   *          matrix_column: List[Double], matrix: List[List[Double]])
   *      ```
   *
   *    - Type alias above-written class to the provided RequestParams type
   *
   *    - Write JSON serializer methods for above-written classes by
   *      overriding the provided requestReads, requestWrites, methods. 
   *      For how to construct JSON serializer methods,
   *      @see <a href="
   *        https://github.com/playframework/play-json#automatic-conversion">
   *        play-json</a>
   *
   *    - Override remaining methods: Write conversions to interpret the givens
   *      from the ExchangeCandidates subroutine (a work driving component of
   *      the overall algorithm) and transform their values into a
   *      RequestParams. These conversions will be called in ExchangeCandidates
   *      whenever Indepence Set membership needs to be evaluated.
   *
   *  Example:
   *    - See algorithm.impl.graph.NodesIndependence._ for a comformant
   *      implementation
   *
   *  @tparam E type of the element from the input stream. Users should
   *            define this type at the top of their respective impl
   *            package and then import it into the subclass declaration
   */
  abstract class IndependenceAPI[E <: Element] extends LambdaConfig {
    type RequestParams

    /** Helper function to find whether S \in I_l */
    def loadInI_L(S: Set[E]): RequestParams

    /** Helper function to find whether S \notin I_l */
    def loadNotInI_L(S: Set[E]): RequestParams

    /* JSON Serialization helper functions */
    def requestReads: Reads[RequestParams]
    def requestWrites: Writes[RequestParams]
  }


  /** Requirements for user classes to invoke valid Marginal Value functions
   *
   *  Subclasses of this class facilitate communication with AWS Lambda 
   *  to invoke a user supplied Marginal Value function, which is necessary
   *  to calculate the greediest selection of elements from the input stream.
   *
   *  The user needs to supply AWS Lambda with their own Marginal Value function
   *  beforehand. The AWS Lambda function can be written in any language
   *  supported by AWS Lambda, as long as the function accesses the same
   *  parameters as the RequestParams type required by this interface
   *
   *  In addition to the members of this interface, subclasses of this class
   *  must also remember to override @val awsLambdaName: String, which must
   *  match the name of the Marginal Value function in the AWS Lambda console.
   *  You wouldn't want to run the wrong lambda function after all your hard
   *  work!
   *
   *  How to subclass:
   *    - Write classes containing members for each parameter to be submitted
   *      in the request to, and received in the response from, AWS Lambda
   *      respectively. For example, if your Marginal Value function uses
   *      a units_of_potato_seeds and units_of_fertilizer parameters,
   *      then calculates units_of_potatoes_produced, then your first class
   *      should look like:
   *      ```
   *        case class PotatoProduction(
   *          units_of_potato_seeds: Double, units_of_fertilizer: Double)
   *      ```
   *      and your second class should look like:
   *      ```
   *        case class Produced(units_of_potatoes_produced: Double)
   *      ```
   *
   *    - Type alias above-written classes to the provided RequestParams and
   *      ResponseBody types
   *
   *    - Write JSON serializer methods for above-written classes by
   *      overriding the provided requestReads, ..., responseWrites methods
   *      For how to construct JSON serializer methods,
   *      @see <a href="
   *        https://github.com/playframework/play-json#automatic-conversion">
   *        play-json</a>
   *
   *    - Override marginalValue: Write a conversion to interpret the givens
   *      from the ExchangeCandidates subroutine (a work driving component of
   *      the overall algorithm) and transform their values into a
   *      RequestParams. This conversion will be called in ExchangeCandidates
   *      whenever the Marginal Value needs to be calculated.
   *
   *  Example:
   *    - See algorithm.impl.graph.LogMarginalValue._ for a comformant
   *      implementation
   *
   *  @tparam E type of the element from the input stream. Users should
   *            define this type at the top of their respective impl
   *            package and then import it into the subclass declaration
   */
   abstract class MarginalValueAPI[E <: Element] extends LambdaConfig {
    type RequestParams
    /** Note: Please subclass [[MarginalValueLambdaResponse]] for this type! */
    type ResponseBody

    /** Factory function to create the request to the lambda function */
    def marginalValue(e: E, S: Set[E], valueType: String): RequestParams

    /* JSON Serialization helper functions */
    def requestReads: Reads[RequestParams]
    def requestWrites: Writes[RequestParams]

    def responseReads: Reads[ResponseBody]
    def responseWrites: Writes[ResponseBody]
  }


  /**
   * Constraints/methods for LambdaResponse
   *
   * Users SHOULD subclass this interface when writing their
   * MarginalValueAPI.ResponseBody type alias!
   *
   * Example:
   *   - @see
   *     algorithm.impl.graph.LogMarginalValue.LogMarginalValueLambdaResponse
   */
  abstract class MarginalValueLambdaResponse {
    // The AWS Lambda response body may have a nested structure, or
    // several extra fields that are not related to the calculated Marginal
    // Value. This function helps refine the AWS Lambda response into the
    // required Double value used by the ExchangeCandidates subroutine.
    def marginalValue(): Double
    def notes(): String
    def responseStatusCode(): Int
  }
}
