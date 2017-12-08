package org.corespring.conversion.qti.manifest

import java.util.zip.ZipFile

import com.keydatasys.conversion.qti.util.PassageScrubber
import org.apache.commons.io.IOUtils
import org.corespring.common.util.EntityEscaper
import org.corespring.utils.ErrorDir
import org.slf4j.LoggerFactory

import scala.xml.{Node, XML}

object ZipReader extends PassageScrubber with EntityEscaper {

  lazy val logger = LoggerFactory.getLogger(ZipReader.this.getClass)

  private def stripCDataTags(xmlString: String) =
    """(?s)<!\[CDATA\[(.*?)\]\]>""".r.replaceAllIn(xmlString, "$1")


  def fileContents(zip: ZipFile, name:String) : Option[String] = {
    val entry = zip.getEntry(name)
    if(entry == null){
      None
    } else {
      val is = zip.getInputStream(entry)
      val s = IOUtils.toString(is, "UTF-8")
      IOUtils.closeQuietly(is)
      Some(s)
    }
  }

  def fileXML(zip: ZipFile, name:String) : Option[Node] = fileContents(zip, name)
    .flatMap{ s =>
      val cleaned = stripCDataTags(s)
      val escaped = escapeEntities(cleaned)
      val scrubbed = scrub(escaped)
      try {
        val xmlOut = XML.loadString(scrubbed)
        Some(xmlOut)
      } catch {
        case e :Exception => {
          logger.error(s"Error reading $name, message: ${e.getMessage}")
          e.printStackTrace()
          if(logger.isDebugEnabled()){
            //e.printStackTrace()
          }
          ErrorDir.dump(
            name,
            Some(e),
            "content.xml" -> s,
            "cleaned.xml" -> cleaned,
            "escaped.xml" -> escaped,
            "scrubbed.xml" -> scrubbed
          )

          None
        }
      }
    }
}