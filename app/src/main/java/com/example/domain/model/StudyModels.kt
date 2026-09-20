package com.example.domain.model

data class VariableInfo(
    val variable: String,
    val name: String,
    val unit: String = ""
)

data class FormulaItem(
    val formula: String,
    val meaning: String,
    val variables: List<VariableInfo> = emptyList()
)

data class DefinitionItem(
    val term: String,
    val definition: String
)

data class FlashcardItem(
    val front: String,
    val back: String
)

data class QuestionItem(
    val question: String,
    val type: String = "MCQ", // "MCQ" or "SHORT_ANSWER"
    val options: List<String> = emptyList(),
    val correctAnswer: String,
    val explanation: String,
    val difficulty: String = "Medium",
    val topic: String = ""
)

data class GeneratedStudyPackage(
    val title: String,
    val subject: String,
    val subTopic: String = "",
    val summaryShort: String = "",
    val summaryMedium: String = "",
    val summaryDetailed: String = "",
    val keyPoints: List<String> = emptyList(),
    val definitions: List<DefinitionItem> = emptyList(),
    val formulas: List<FormulaItem> = emptyList(),
    val flashcards: List<FlashcardItem> = emptyList(),
    val mcqs: List<QuestionItem> = emptyList(),
    val shortQuestions: List<QuestionItem> = emptyList(),
    val explanation: String = ""
)

data class ExtractedNoteData(
    val extractedText: String,
    val detectedTitle: String = "Untitled Note",
    val detectedSubject: String = "General Study",
    val headings: List<String> = emptyList(),
    val bulletPoints: List<String> = emptyList(),
    val detectedFormulas: List<String> = emptyList(),
    val confidencePercent: Int = 92,
    val isHandwritten: Boolean = true,
    val pageCount: Int = 1
)

data class EvaluationResult(
    val status: String, // "CORRECT", "PARTIALLY_CORRECT", "NEEDS_IMPROVEMENT"
    val feedback: String,
    val missingElements: List<String> = emptyList(),
    val suggestedAnswer: String
)

data class ExplanationBundle(
    val term: String,
    val simpleExplanation: String,
    val detailedExplanation: String,
    val exampleBasedExplanation: String
)
