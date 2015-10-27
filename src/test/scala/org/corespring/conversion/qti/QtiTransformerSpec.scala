package org.corespring.conversion.qti

import org.specs2.mutable.Specification
import play.api.libs.json.JsObject

import scala.xml.XML

class QtiTransformerSpec extends Specification {

  val qti = <assessmentItem xmlns="http://www.imsglobal.org/xsd/imsqti_v2p1"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="http://www.imsglobal.org/xsd/imsqti_v2p1 http://www.imsglobal.org/xsd/imsqti_v2p1.xsd"
                            identifier="composite" title="Composite Item" adaptive="false" timeDependent="false">

    <!-- definition for question one, a short text entry response with correct answer = "white" -->
    <responseDeclaration identifier="Q_01" cardinality="single" baseType="string">
      <correctResponse>
        <value>397.66</value>
        <value>397.7</value>
      </correctResponse>
    </responseDeclaration>

    <!-- definition for question one, a short text entry response with correct answer = "white" -->
    <responseDeclaration identifier="Q_02" cardinality="single" baseType="string">
      <correctResponse>
        <value></value>
      </correctResponse>
    </responseDeclaration>

    <itemBody>

      <p class="intro-2">A closed box in the shape of a rectangular prism has a length of 13 cm, a width of
        5.3 cm, and a height of 7.1 cm.</p>




      <p class="prompt"><u>Part A</u>: Find its surface area in square centimeters:</p>
      <p class= "p-indent-20"><textEntryInteraction responseIdentifier="Q_01" expectedLength="10"/> square centimeters</p>

      <feedbackBlock
      outcomeIdentifier="responses.Q_01.value"
      identifier="397.66">
        <div class="feedback-block-correct">Nice work, that's correct!</div>
      </feedbackBlock>
      <feedbackBlock
      outcomeIdentifier="responses.Q_01.value"
      identifier="397.7">
        <div class="feedback-block-correct">Nice work, that's correct!</div>
      </feedbackBlock>


      <!-- this is for any response not defined as a correct response in responseDeclaration -->
      <feedbackBlock
      outcomeIdentifier="responses.Q_01.value"
      incorrectResponse="true">
        <div class="feedback-block-incorrect">Good try, but its surface area is 397.66 square centimeters.</div>
      </feedbackBlock>

      <p/>
      <p><u>Part B</u>: A smaller box has dimensions that are half the  measurements of the
        original.</p>

      <p class="prompt">Find the ratio of the surface area of the original box to the surface area of the smaller box.</p>
      <p class= "p-indent-20"><textEntryInteraction responseIdentifier="Q_02" expectedLength="20"/></p>

    </itemBody>
  </assessmentItem>


 "transform" should {
   "transform bad qti found on qa" in {
     val json = QtiTransformer.transform(qti)
     (json \ "components").asOpt[JsObject].isDefined === true
   }
 }

}
