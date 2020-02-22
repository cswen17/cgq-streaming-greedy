#!/usr/bin/env scala

import java.io._
import scala.util.Random

import play.api.lib.ScalaJson._
import play.api.libs.json._
import play.api.libs.functional.syntax._


object GenTestData {

  // constants
  val EXPECTED_F_RANGE_TOLERANCE = 2
  val EXPECTED_EXCLUDE_TOLERANCE = 0.1 // % of output that can be wrong
  val EXPECTED_INCLUDE_TOLERANCE = 0.8 // % of output that should be right
  val F_VALUE_FUNCTION = "logWeight"

  // types
  type OptionMap = Map[Symbol, Any]

  // Begin case classes
  case class InputJson(
    independentSets: Map[String, List[String]],
    nonIndependentSets: List[String])

  case class InputHolder(
    allIndependentSets: List[String],
    independentSetsByStartingNode: Map[String, List[String]],
    nonIndependentSets: List[String])

  case class Flags(
    independence: Double,
    streamSize: Int,
    startsWith: Option[List[String]],
    appearsAt: Option[String],
    filename: String)

  case class TestMetadata(
    inputStream: List[String],
    expectedOutputInclude: List[String],
    expectedOutputIncludeThreshold: Double,
    expectedOutputShouldNotInclude: List[String],
    expecetdOutputShouldNotIncludeThreshold: Double,
    expectedFValueTarget: Int,
    expectedFRangeTolerance: Double)

  /*
   * Parses the json data from the given file and constructs a case class
   * Throws exception on Json parsing failure
   * 
   * @param filename The name of the input file, should be .json
   */
  def parseInputFile(filename: String) : InputHolder = {
    // extracts the json from the input file
    val fileInputStream: FileInputStream = new FileInputStream(
      new File(filename))
    val inputJson: JsonObject = JsonObject.apply(fileInputStream)
    val inputHolderJs: InputJson = inputJson.validate[InputJson] match {
      case success => success.get
      case error => throw error
    }

    // post-processes some of the input file data
    val allIndependentSets: List[String] = inputHolder.independentSets.values
      .reduceLeft(_++_)
    
    // returns the refined InputHolder case class instance
    InputHolder(
      allIndependentSets,
      inputHolderJs.independentSets,
      inputHolderJs.nonIndependentSets)
  }

  /*
   * Helper function for arg parsing
   */
  def nextOption(mp: OptionMap, list: List[String]): OptionMap = {
    def isSwitch(s: String) = (s(0) == '-')
    list match {
      case Nil => mp
      case "--independence"::value::tail =>
        nextOption(mp ++ Map('independence -> value.toDouble), tail)
      case "--starts-with"::value::tail =>
        nextOption(
          mp ++ Map(
            'startsWith' -> {
              value.toIterator.map {c => List(c.toString)}.reduce {_++_}
            }),
          tail)
      case "--stream-size"::value::tail =>
        nextOption(map ++ Map('streamSize -> value.toInt), tail)
      case "--appears-at"::value::tail =>
        nextOption(map ++ Map('appearsAt -> value), tail)
      case string::opt2::tail if isSwitch(opt2) =>
        nextOption(map ++ Map('infile -> string), list.tail)
      case string::Nil => nextOption(map ++ Map('infile->string), list.tail)
      case option::tail => {
        println("Unknown option"+option)
        exit(1)
      }
    }
  }

  /*
   * Parses script args into flags and then does validation
   * Throws exceptions when required args are not found. The exceptions
   * are thrown automatically by the functions in Scala's Map API
   *
   * @param args The arguments
   */
  def parseArgs(args: Array[String]): Flags = {
    val options: OptionMap = nextOption(map, args)
    val independence: Double = options('independence)
    val streamSize: Int = options('streamSize)
    val startsWith: Option[List[String]] = options.get('startsWith)
    val appearsAt: String = options.getOrElse('appearsAt, "random")
    val filename: String = options('infile)
    Flags(independence, streamSize, startsWith, appearsAt, filename)
  }

  def main(args: Array[String]) {
    val flags: Flags = parseArgs(args)
    val inputHolder: InputHolder = parseInputFile(flags.filename)
    val sampleSize : Int = (flags.independence * flags.streamsize).toInt

    val sampleFromStartsWith: Int = flags.startsWith match {
      case Some(strList) => sampleSize / strList.length
      case None => sampleSize
    }
    val independentSets: List[String] = flags.startsWith match {
      case Some(strList) => {
        strList
          .map(startsWithNode => {
              val labels: Map[String, List[String]] = {
                inputHolder.independentSetsByStartingNode
              }
              val indies: List[String] = {
                labels.getOrElse(startsWithNode, List(""))
              }
              indies.take(sampleFromStartsWith)
            })
          .flatten
      }
      case None => inputHolder.allIndependentSets.take(sampleFromStartsWith)
    }

    val otherSampleSize: Int = flags.streamSize - independentSets.length
    val nonIndependentSets: List[String] = inputHolder.nonIndependentSets.take(
      otherSampleSize)
    val fValue: Int = {
      def numericWeight(chr: Char): Int = chr.getNumericValue - 10
      def setWeight(st: String): Int = st.map(c => numericWeight(c)).reduce(_+_)
      def lg(n: Int): Int = n match {
        case 0 => 0
        case 1 => 0
        case m => lg(m / 2)
      }

      independenceSets.map(st => lg(setWeight(st))).reduce(_+_)
    }

    val r : Random = new Random()
    val inputStream: List[String] = flags.appearsAt match {
      case "beginning" => independentSets ++ nonIndependentSets
      case "end" => nonIndependentSets ++ independentSets
      case "random" => r.shuffle(independentSets ++ nonIndependentSets)
      case "middle" => {
        val front = nonIndependentSets.take(nonIndependentSets.length / 2)
        val back = nonIndependentSets.drop(nonIndependentSets.length / 2)
        front ++ independentSets ++ back
      }
      case "alternating" => {
        val zipped: List[(String, String)] = {
          independentSets.zipAll(nonIndependentSets, "", "")
        }
        val listed: List[List[String]] = zipped.map(t => t.productIterator.toList)
        listed.flatten().filter(s => s != "")
      }
      case _ => throw Exception("Unknown")
    }

    // Writes to json
    val testMetadata: TestMetadata = TestMetadata(
      inputStream, independentSets, EXPECTED_INCLUDE_TOLERANCE,
      nonIndependentSets, EXPECTED_EXCLUDE_TOLERANCE, fValue,
      EXPECTED_F_RANGE_TOLERANCE)
    implicit val testMetadataWrites: Writes[TestMetadata] = (
      (JsPath \ "inputStream").write[Seq[String]] and
      (JsPath \ "expectedOutputInclude").write[Seq[String]] and
      (JsPath \ "expectedOutputIncludeThreshold").write[Double] and
      (JsPath \ "expectedOutputShouldNotInclude").write[Seq[String]] and
      (JsPath \ "expectedOutputShouldNotIncludeWithThreshold").write[Double] and
      (JsPath \ "expectedFValueTarget").write[Int] and
      (JsPath \ "expectedFRangeTolerance").write[Int]
    )(unlift(TestMetadata.unapply))
    val json: Json = Json.toJson(testMetadata)
    val outputPath: String = new String(s"./${flags.independence}_${flags.startsWith}_${flags.streamSize}_${flags.appearsAt}_${flags.infile}")
    val writer: PrintWriter = new PrintWriter(new Writer(outputPath))
    writer.write(Json.stringify(json))
    writer.close()
    println("Success")
    // ----End main
  }
  
  // ----End Script
}
