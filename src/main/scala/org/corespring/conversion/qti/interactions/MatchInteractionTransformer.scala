package org.corespring.conversion.qti.interactions

import play.api.libs.json._

import scala.xml.Node

object MatchInteractionTransformer extends InteractionTransformer {

  override def transform(node: Node, manifest: Node): Seq[Node] = node match {
    case node: Node if (node.label == "matchInteraction") =>
      <p class="prompt">{ (node \ "prompt").map(_.child).flatten }</p> ++
          <corespring-match id={ (node \\ "@responseIdentifier").text }/>
    case _ => node
  }

  override def interactionJs(qti: Node, manifest: Node): Map[String, JsObject] = (qti \\ "matchInteraction").map(implicit node => {
    def matchSet(id: String, answers: Seq[String]): Seq[Boolean] =
      ((qti \\ "simpleMatchSet").last \\ "simpleAssociableChoice").map(choice => answers.contains((choice \ "@identifier").text))

    (node \ "@responseIdentifier").text -> Json.obj(
      "componentType" -> "corespring-match",
      "correctResponse" ->
        (((node \\ "simpleMatchSet").head \\ "simpleAssociableChoice").zipWithIndex.map{ case (choice, index) => {
          Json.obj(
            "id" -> (choice \ "@identifier").text,
            "matchSet" -> matchSet(
              (choice \ "@identifier").text,
              (qti \\ "correctResponse" \\ "value")
                .find(_.text.trim.startsWith((choice \ "@identifier").text))
                .map(_.text.split(" ").drop(1).toSeq).getOrElse(Seq.empty))
          )
        }}
      ),
      "model" -> Json.obj(
        "columns" -> ((node \\ "simpleMatchSet").tail \\ "simpleAssociableChoice").map(col => Json.obj("labelHtml" -> col.text.trim)),
        "rows" -> ((node \\ "simpleMatchSet").head \\ "simpleAssociableChoice").map(row => Json.obj(
          "id" -> (row \ "@identifier").text,
          "labelHtml" -> row.text.trim
        ))
      )
    )
  }).toMap


}
