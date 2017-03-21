package org.corespring.conversion.zip

import java.io._
import java.util.zip._
import org.corespring.common.util._
import play.api.libs.json._

import scala.io.Source

/**
 * Represents an interface which can translate a QTI zip file into a CoreSpring JSON zip file
 */
trait QtiToCorespringConverter extends HtmlProcessor with UnicodeCleaner {

  def convert(zip: ZipFile, path: String, metadata: Option[JsObject]): ZipFile

  /**
    * Scala's XML parser won't even preserve these characters in CDATA tags.
    */
  def unescapeCss(string: String): String = new Rewriter("""<style type="text/css">(.*?)</style>""") {
    def replacement() = s"""<style type="text/css">${group(1).replaceAll("&gt;", ">")}</style>"""
  }.rewrite(string)

  def postProcess(item: JsValue): JsValue = item match {
    case json: JsObject => {
      val xhtml = unescapeCss(postprocessHtml((json \ "xhtml").as[String]))
      cleanUnicode(json ++ Json.obj(
        "xhtml" -> xhtml,
        "components" -> postprocessHtml((json \ "components")),
        "summaryFeedback" -> postprocessHtml((json \ "summaryFeedback").asOpt[String].getOrElse(""))
      ))
    }
    case _ => item
  }

  def toZipByteArray(files: Map[String, Source]) = {
    val bos = new ByteArrayOutputStream()
    val zipFile = new ZipOutputStream(bos)
    files.foreach{ case (filename, contents) => {
      zipFile.putNextEntry(new ZipEntry(filename))
      zipFile.write(asBytes(contents, filename))
    }}
    zipFile.close
    bos.toByteArray
  }

  private def asBytes(contents: Source, filename: String) = {
    filename.endsWith(".png") match {
      case true => contents.map(_.toByte).toArray
      case _  => contents.mkString.getBytes
    }
  }



  def writeZip(byteArray: Array[Byte], path: String) = {
    val file = new File(path)
    val fileOutput = new FileOutputStream(file)
    try {
      fileOutput.write(byteArray)
    } finally {
      fileOutput.close()
    }
    new ZipFile(file)
  }



}
