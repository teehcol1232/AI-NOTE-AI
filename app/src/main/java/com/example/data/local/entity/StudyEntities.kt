package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_documents")
data class StudyDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subject: String,
    val subTopic: String = "",
    val originalText: String,
    val materialType: String, // "CAMERA", "IMAGE", "PDF", "TEXT"
    val imageUris: String = "", // Comma-separated or JSON list of image/file paths
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val difficulty: String = "Medium"
)

@Entity(tableName = "generated_materials")
data class GeneratedMaterialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val summaryShort: String = "",
    val summaryMedium: String = "",
    val summaryDetailed: String = "",
    val keyPointsJson: String = "[]",
    val explanation: String = ""
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val front: String,
    val back: String,
    val isMastered: Boolean = false,
    val needsReview: Boolean = false,
    val timesReviewed: Int = 0,
    val isBookmarked: Boolean = false
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val questionText: String,
    val type: String, // "MCQ" or "SHORT_ANSWER"
    val optionsJson: String = "[]", // List of 4 options for MCQ
    val correctAnswer: String,
    val explanation: String,
    val difficulty: String = "Medium",
    val topic: String = ""
)

@Entity(tableName = "formulas")
data class FormulaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val formula: String,
    val meaning: String,
    val variablesJson: String = "[]", // [{ "variable": "V", "name": "Voltage", "unit": "Volts" }]
    val isBookmarked: Boolean = false
)

@Entity(tableName = "definitions")
data class DefinitionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val term: String,
    val definition: String,
    val isBookmarked: Boolean = false
)

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val documentTitle: String,
    val subject: String,
    val score: Int,
    val totalQuestions: Int,
    val accuracyPercent: Int,
    val timeTakenSeconds: Int,
    val weakTopicsJson: String = "[]",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "mistakes")
data class MistakeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questionId: Long = 0,
    val documentId: Long,
    val questionText: String,
    val userAnswer: String,
    val correctAnswer: String,
    val explanation: String,
    val subject: String,
    val topic: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val resolved: Boolean = false
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "School",
    val colorHex: String = "#4F46E5"
)
