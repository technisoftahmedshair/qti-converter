package com.keydatasys.conversion.qti

import com.keydatasys.conversion.qti.interactions.{GraphicGapMatchInteractionTransformer => KDSGraphicGapMatchInteractionTransformer, ChoiceInteractionTransformer => KDSChoiceInteractionTransformer, TextEntryInteractionTransformer => KDSTextEntryInteractionTransformer, MatchInteractionTransformer => KDSMatchInteractionTransformer, _}
import org.corespring.common.xml.XMLNamespaceClearer
import org.corespring.conversion.qti.interactions._
import org.corespring.conversion.qti.processing.ProcessingTransformer
import org.corespring.conversion.qti.{QtiTransformer => SuperQtiTransformer}

import scala.xml.transform.RewriteRule
import scala.xml.{Node, Elem}

object QtiTransformer extends SuperQtiTransformer with ProcessingTransformer {

  override def ItemBodyTransformer = new RewriteRule with XMLNamespaceClearer {

    override def transform(node: Node): Seq[Node] = {
      node match {
        case elem: Elem if elem.label == "itemBody" => {
          <div class="item-body kds qti">{ elem.child }</div>
        }
        case _ => node
      }
    }
  }

  def interactionTransformers(qti: Elem) = Seq(
    CalculatorWidgetTransformer,
    CorespringTabTransformer,
    CoverflowInteractionTransformer,
    DragAndDropInteractionTransformer,
    ExtendedTextInteractionTransformer,
    FeedbackBlockTransformer(qti),
    FocusTaskInteractionTransformer,
    FoldableInteractionTransformer,
    HottextInteractionTransformer,
    KDSChoiceInteractionTransformer,
    new KDSGraphicGapMatchInteractionTransformer(),
    LineInteractionTransformer,
    KDSMatchInteractionTransformer,
    NumberedLinesTransformer(qti),
    NumberLineInteractionTransformer,
    OrderInteractionTransformer,
    PointInteractionTransformer,
    ProtractorWidgetTransformer,
    RubricBlockTransformer,
    RulerWidgetTransformer,
    SelectPointInteractionTransformer(qti),
    SelectTextInteractionTransformer,
    TeacherInstructionsTransformer,
    TextEntryInteractionTransformer(qti)
  )

  def statefulTransformers = Seq(
    FeedbackBlockTransformer,
    NumberedLinesTransformer,
    KDSTextEntryInteractionTransformer
  )

}