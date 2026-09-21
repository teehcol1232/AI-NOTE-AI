package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.StudyEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudyEngineGroundedAiTest {

    private lateinit var studyEngine: StudyEngine

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        studyEngine = StudyEngine(context)
    }

    @Test
    fun testChemistryNotesGenerationIsGrounded() = runBlocking {
        val chemistryNotes = """
            Acids and Bases - Arrhenius vs Bronsted-Lowry:
            An Arrhenius acid produces H+ ions in aqueous solution (e.g., HCl -> H+ + Cl-).
            An Arrhenius base produces OH- ions in aqueous solution (e.g., NaOH -> Na+ + OH-).
            Bronsted-Lowry acid is a proton (H+) donor.
            Bronsted-Lowry base is a proton (H+) acceptor.
            pH = -log[H+]
            pOH = -log[OH-]
            pH + pOH = 14 at 25°C.
            Neutralization: Acid + Base -> Salt + Water.
        """.trimIndent()

        val studyPackage = studyEngine.generateStudyPackage(
            noteText = chemistryNotes,
            selectedComponents = setOf("MCQS", "FLASHCARDS", "DEFINITIONS", "SUMMARY"),
            difficulty = "Medium",
            questionCount = 4
        )

        // Verify Subject Grounding
        assertEquals("Chemistry", studyPackage.subject)

        // Verify MCQs are present and strictly grounded
        assertTrue("MCQs should not be empty", studyPackage.mcqs.isNotEmpty())
        for (mcq in studyPackage.mcqs) {
            assertEquals("Each MCQ must have 4 options", 4, mcq.options.size)
            assertEquals("All options must be unique", 4, mcq.options.toSet().size)
            assertTrue("Correct answer '${mcq.correctAnswer}' must be in options: ${mcq.options}", mcq.options.contains(mcq.correctAnswer))
            assertFalse("Explanation must not be blank", mcq.explanation.isBlank())

            // Strict grounding check: Absolutely no physics or E=mc² hallucinations
            assertFalse("Question must not mention E=mc²: ${mcq.question}", mcq.question.contains("E = mc²", ignoreCase = true))
            assertFalse("Question must not mention velocity: ${mcq.question}", mcq.question.contains("velocity", ignoreCase = true))
        }

        // Verify Flashcards
        assertTrue("Flashcards should not be empty", studyPackage.flashcards.isNotEmpty())
        for (card in studyPackage.flashcards) {
            assertFalse("Flashcard front cannot be empty", card.front.isBlank())
            assertFalse("Flashcard back cannot be empty", card.back.isBlank())
            assertFalse("Flashcard must not hallucinate E=mc²", card.front.contains("E = mc²") || card.back.contains("E = mc²"))
        }
    }

    @Test
    fun testNoFormulasGeneratedWhenNotesHaveNoEquations() = runBlocking {
        val historyNotes = """
            The Industrial Revolution began in Great Britain in the late 18th century.
            It marked a major turning point in history with the transition to new manufacturing processes.
            Key innovations included the steam engine developed by James Watt and mechanized textile production.
            Urbanization increased dramatically as workers moved from rural areas to factory cities.
        """.trimIndent()

        val studyPackage = studyEngine.generateStudyPackage(
            noteText = historyNotes,
            selectedComponents = setOf("SUMMARY", "MCQS", "FLASHCARDS"),
            difficulty = "Medium",
            questionCount = 3
        )

        // History notes have no equations, so formulas must be empty (NO E = mc² hallucination)
        assertTrue("Formulas must be empty when notes contain no math/science equations", studyPackage.formulas.isEmpty())
        assertEquals("History", studyPackage.subject)
    }

    @Test
    fun testShortAnswerEvaluation() = runBlocking {
        val question = "What is a Bronsted-Lowry acid?"
        val modelAnswer = "A Bronsted-Lowry acid is defined as a proton (H+) donor."
        val studentAnswer = "It is a substance that donates a proton or H+ ion."
        val context = "Bronsted-Lowry acid is a proton (H+) donor. Base is a proton acceptor."

        val eval = studyEngine.evaluateShortAnswer(question, modelAnswer, studentAnswer, context)
        assertNotNull(eval)
        assertTrue("Evaluation status should be CORRECT or PARTIALLY_CORRECT: ${eval.status}",
            eval.status == "CORRECT" || eval.status == "PARTIALLY_CORRECT"
        )
        assertFalse("Feedback should not be blank", eval.feedback.isBlank())
    }

    @Test
    fun testExplainConceptMultiStyle() = runBlocking {
        val context = "Neutralization reaction: Acid + Base -> Salt + Water. Releases heat (exothermic)."
        val bundle = studyEngine.explainConcept("Neutralization", context)

        assertNotNull(bundle)
        assertFalse("Simple explanation should be present", bundle.simpleExplanation.isBlank())
        assertFalse("Detailed explanation should be present", bundle.detailedExplanation.isBlank())
        assertFalse("Example explanation should be present", bundle.exampleBasedExplanation.isBlank())
    }
}
