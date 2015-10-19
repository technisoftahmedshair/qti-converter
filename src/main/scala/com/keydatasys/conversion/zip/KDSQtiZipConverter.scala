package com.keydatasys.conversion.zip

import java.io.{FileOutputStream, File, ByteArrayOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream, ZipFile}

import com.keydatasys.conversion.qti.ItemExtractor
import com.keydatasys.conversion.qti.util.{HtmlProcessor, PathFlattener}
import org.corespring.common.file.SourceWrapper
import org.corespring.common.json.JsonUtil
import org.corespring.common.util.Rewriter
import org.corespring.conversion.zip.QtiToCorespringConverter
import play.api.libs.json._

import scala.collection.JavaConversions._
import scala.io.Source
import scalaz.{Validation, Success, Failure}

object KDSQtiZipConverter extends QtiToCorespringConverter with PathFlattener with HtmlProcessor with JsonUtil {

  private val collectionName = "kds"
  private val collectionId = "5453b4e4e4b05f38dd6440a8"

  override def convert(zip: ZipFile, path: String = "target/corespring-json.zip", metadata: Option[JsObject] = None): ZipFile = {

    val fileMap = zip.entries.filterNot(_.isDirectory).map(entry => {
      entry.getName.flattenPath -> SourceWrapper(entry.getName, zip.getInputStream(entry))
    }).toMap

    val extractor = new ItemExtractor(fileMap, metadata.getOrElse(Json.obj()))
    val itemCount = extractor.ids.length
    val processedFiles = extractor.ids.zipWithIndex.map{ case(id, index) => {
      println(s"Processing ${id} (${index+1}/$itemCount)")
      val itemJson = extractor.itemJson
      val meta = extractor.metadata
      val result: Validation[Error, (JsValue, JsObject
        )] = (itemJson.get(id).getOrElse(Failure(new Error("Missing item JSON"))),
        meta.get(id).getOrElse(Failure(new Error("Missing item metadata")))) match {
        case (Failure(error), _) => Failure(error)
        case (_, Failure(error)) => Failure(error)
        case (Success(itemJson), Success(md)) => {
          implicit val metadata = md
          Success((postProcess(itemJson), taskInfo))
        }
      }
      result match {
        case Success((json, taskInfo)) => {
          val basePath = s"${collectionName}_${collectionId}/$id"
          Seq(s"$basePath/player-definition.json" -> Source.fromString(Json.prettyPrint(json)),
            s"$basePath/profile.json" -> Source.fromString(Json.prettyPrint(
              Json.obj("taskInfo" -> taskInfo, "originId" -> id)))) ++
            extractor.filesFromManifest(id).map(filename => s"$basePath/data/${filename.flattenPath}" -> fileMap.get(filename))
              .filter { case (filename, maybeSource) => maybeSource.nonEmpty }
              .map { case (filename, someSource) => (filename, someSource.get.toSource()) }
        }
        case _ => Seq.empty[(String, Source)]
      }
    }}.flatten.toMap
    writeZip(toZipByteArray(processedFiles), path)
  }

  private def postProcess(item: JsValue): JsValue = item match {
    case json: JsObject => {
      json ++ Json.obj(
        "xhtml" -> unescapeCss(postprocessHtml((json \ "xhtml").as[String])),
        "components" -> postprocessHtml((json \ "components")),
        "summaryFeedback" -> postprocessHtml((json \ "summaryFeedback").asOpt[String].getOrElse(""))
      )
    }
    case _ => item
  }


  /**
   * Scala's XML parser won't even preserve these characters in CDATA tags.
   */
  private def unescapeCss(string: String): String = new Rewriter("""<style type="text/css">(.*?)</style>""") {
    def replacement() = s"""<style type="text/css">${group(1).replaceAll("&gt;", ">")}</style>"""
  }.rewrite(string)

  private def writeZip(byteArray: Array[Byte], path: String) = {
    val file = new File(path)
    val fileOutput = new FileOutputStream(file)
    try {
      fileOutput.write(byteArray)
    } finally {
      fileOutput.close()
    }
    new ZipFile(file)
  }

  private def taskInfo(implicit metadata: Option[JsValue]): JsObject = {
    partialObj(
      "relatedSubject" -> Some(Json.arr()),
      "domains" -> Some(Json.arr()),
      "extended" -> metadata.map(md => Json.obj(
        "kds" -> md
      ))
    )
  }

  private def toZipByteArray(files: Map[String, Source]) = {
    val bos = new ByteArrayOutputStream()
    val zipFile = new ZipOutputStream(bos)
    files.foreach{ case (filename, contents) => {
      zipFile.putNextEntry(new ZipEntry(filename))
      zipFile.write(contents.map(_.toByte).toArray)
    }}
    zipFile.close
    bos.toByteArray
  }


}