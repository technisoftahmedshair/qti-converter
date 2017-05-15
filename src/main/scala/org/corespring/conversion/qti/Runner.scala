package org.corespring.conversion.qti

import java.util.zip.ZipFile

import com.keydatasys.conversion.zip.KDSQtiZipConverter
import com.progresstesting.conversion.zip.{NewProgressTestingQtiZipConverter, ProgressTestingQtiZipConverter}
import org.corespring.conversion.zip.ConversionOpts
import org.measuredprogress.conversion.zip.MeasuredProgressQtiZipConverter
import play.api.libs.json._
import scala.concurrent.Await._
import scala.concurrent.duration._
import scalaz.{Failure, Success, Validation}
import scala.concurrent.ExecutionContext.Implicits.global

object Runner extends App {

  val parsed = new FlagMap(Seq(
    Flag("input", "i", None),
    Flag("limit", "l", None),
    Flag("output", "o", None),
    Flag("metadata", "m", Some("{}")),
    Flag("vendor", "v", Some("kds"))
  )).toMap(args)

  val converters = Map(
    "kds" -> KDSQtiZipConverter,
    "progresstesting" -> ProgressTestingQtiZipConverter,
    "pt" -> NewProgressTestingQtiZipConverter,
    "measuredprogress" -> MeasuredProgressQtiZipConverter
  )

  parsed match {
    case Success(usefulArgs) => {
      val input =
        new ZipFile(usefulArgs.get("input").getOrElse(throw new IllegalStateException("Undefined for input")))
      val outputPath = usefulArgs.get("output").getOrElse(throw new IllegalStateException("Undefined for output"))
      val vendor = usefulArgs.get("vendor").getOrElse(throw new IllegalStateException("Undefined for vendor"))
      val metadata = Json.parse(usefulArgs.get("metadata").getOrElse("{}")).as[JsObject]
      val converter = converters
        .get(vendor).getOrElse(throw new IllegalArgumentException(s"You must specify a supported vendor: ${converters.keys.mkString(", ")}"))

      val opts = ConversionOpts(usefulArgs.get("limit").map(_.toInt).getOrElse(0))
      result(converter.convert(input, outputPath, Some(metadata), opts)
        .map( _ => {
          println("all done")
        }), 25.minutes)
    }
    case Failure(error) => {
      println(error.getMessage)
      println(
        """ Usage:
          |   sbt run --input qti.zip --output json.zip --vendor kds --metadata \"{\\\"scoringType\\\": \\\"PARCC\\\"}\""""".stripMargin)
      sys.exit(1)
    }
  }

}

case class Flag(long: String, short: String, default: Option[String])

class FlagMap(flags: Seq[Flag]) {

  private def defaults = flags.map(flag => flag.default.map(flag.long -> _)).flatten.toMap

  private def fromArgs(args: Array[String]) = args.sliding(2).map{ case Array(key, value) => {
    flags.find(flag => (key == s"--${flag.long}" || key == s"-${flag.short}")) match {
      case Some(flag) => Some(flag.long -> value)
      case _ => None
    }
  }}.flatten.toMap

  private def missing(args: Map[String, String]) = flags.map(_.long).filterNot(flag => args.keySet.contains(flag))

  def toMap(args: Array[String]): Validation[Error, Map[String, String]] = {
    val result = defaults ++ fromArgs(args)
    missing(result) match {
      case empty: Seq[String] if (empty.isEmpty) => Success(result)
      case missingFields => Failure(new Error(s"Missing values for ${missingFields.mkString(", ")}"))
    }
  }

}