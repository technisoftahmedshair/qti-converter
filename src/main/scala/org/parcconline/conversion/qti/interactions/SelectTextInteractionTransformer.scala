package org.parcconline.conversion.qti.interactions

import org.corespring.conversion.qti.interactions.{SelectTextInteractionTransformer => SuperSelectTextInteractionTransformer}
import play.api.libs.json.Json

import scala.xml.Node

class SelectTextInteractionTransformer extends SuperSelectTextInteractionTransformer {

  override def interactionJs(qti: Node, manifest: Node) = super.interactionJs(qti, manifest).map{ case(id, json) => {
    id -> json.deepMerge(Json.obj(
      "feedback" -> Json.obj(
        "correctFeedbackType" -> "none",
        "partialFeedbackType" -> "none",
        "incorrectFeedbackType" -> "none"
      )
    ))
  }}


}
