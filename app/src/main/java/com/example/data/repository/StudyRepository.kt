package com.example.data.repository

import com.example.data.ai.StudyEngine
import com.example.data.local.dao.StudyDao
import com.example.data.local.entity.*
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class StudyRepository(
    private val studyDao: StudyDao,
    private val studyEngine: StudyEngine
) {
    val allDocuments: Flow<List<StudyDocumentEntity>> = studyDao.getAllDocuments()
    val favoriteDocuments: Flow<List<StudyDocumentEntity>> = studyDao.getFavoriteDocuments()
    val allQuizAttempts: Flow<List<QuizAttemptEntity>> = studyDao.getAllQuizAttempts()
    val unresolvedMistakes: Flow<List<MistakeEntity>> = studyDao.getUnresolvedMistakes()
    val totalFlashcardsCount: Flow<Int> = studyDao.getTotalFlashcardsCount()
    val totalQuizzesCount: Flow<Int> = studyDao.getTotalQuizzesCount()
    val averageQuizScore: Flow<Double?> = studyDao.getAverageQuizScore()
    val allSubjects: Flow<List<SubjectEntity>> = studyDao.getAllSubjects()

    fun searchDocuments(query: String): Flow<List<StudyDocumentEntity>> = studyDao.searchDocuments(query)

    fun getDocumentsBySubject(subject: String): Flow<List<StudyDocumentEntity>> = studyDao.getDocumentsBySubject(subject)

    suspend fun getDocumentById(id: Long): StudyDocumentEntity? = studyDao.getDocumentById(id)

    suspend fun getGeneratedMaterial(documentId: Long): GeneratedMaterialEntity? = studyDao.getGeneratedMaterial(documentId)

    fun observeGeneratedMaterial(documentId: Long): Flow<GeneratedMaterialEntity?> = studyDao.observeGeneratedMaterial(documentId)

    fun getFlashcardsForDocument(documentId: Long): Flow<List<FlashcardEntity>> = studyDao.getFlashcardsForDocument(documentId)

    suspend fun getFlashcardsSync(documentId: Long): List<FlashcardEntity> = studyDao.getFlashcardsSync(documentId)

    fun getQuestionsForDocument(documentId: Long): Flow<List<QuestionEntity>> = studyDao.getQuestionsForDocument(documentId)

    suspend fun getQuestionsSync(documentId: Long): List<QuestionEntity> = studyDao.getQuestionsSync(documentId)

    fun getFormulasForDocument(documentId: Long): Flow<List<FormulaEntity>> = studyDao.getFormulasForDocument(documentId)

    fun getDefinitionsForDocument(documentId: Long): Flow<List<DefinitionEntity>> = studyDao.getDefinitionsForDocument(documentId)

    suspend fun toggleFavorite(id: Long) = studyDao.toggleFavorite(id)

    suspend fun renameDocument(id: Long, newTitle: String) = studyDao.renameDocument(id, newTitle)

    suspend fun deleteDocument(id: Long) = studyDao.deleteDocument(id)

    suspend fun updateFlashcardReview(id: Long, mastered: Boolean, needsReview: Boolean) =
        studyDao.updateFlashcardReview(id, mastered, needsReview)

    suspend fun toggleFlashcardBookmark(id: Long) = studyDao.toggleFlashcardBookmark(id)

    suspend fun toggleFormulaBookmark(id: Long) = studyDao.toggleFormulaBookmark(id)

    suspend fun toggleDefinitionBookmark(id: Long) = studyDao.toggleDefinitionBookmark(id)

    suspend fun markMistakeResolved(id: Long) = studyDao.markMistakeResolved(id)

    suspend fun deleteMistake(id: Long) = studyDao.deleteMistake(id)

    suspend fun insertSubject(subject: SubjectEntity) = studyDao.insertSubject(subject)

    // Save full generated study package
    suspend fun saveStudyPackage(
        originalText: String,
        materialType: String,
        packageData: GeneratedStudyPackage,
        difficulty: String
    ): Long {
        val docId = studyDao.insertDocument(
            StudyDocumentEntity(
                title = packageData.title.ifBlank { "Untitled Study Set" },
                subject = packageData.subject.ifBlank { "General" },
                subTopic = packageData.subTopic,
                originalText = originalText,
                materialType = materialType,
                difficulty = difficulty
            )
        )

        // Save generated material
        val kpJson = JSONArray(packageData.keyPoints).toString()
        studyDao.insertGeneratedMaterial(
            GeneratedMaterialEntity(
                documentId = docId,
                summaryShort = packageData.summaryShort,
                summaryMedium = packageData.summaryMedium,
                summaryDetailed = packageData.summaryDetailed,
                keyPointsJson = kpJson,
                explanation = packageData.explanation
            )
        )

        // Save flashcards
        val flashcardEntities = packageData.flashcards.map {
            FlashcardEntity(
                documentId = docId,
                front = it.front,
                back = it.back
            )
        }
        if (flashcardEntities.isNotEmpty()) {
            studyDao.insertFlashcards(flashcardEntities)
        }

        // Save MCQs & Short Questions
        val questionEntities = (packageData.mcqs + packageData.shortQuestions).map {
            QuestionEntity(
                documentId = docId,
                questionText = it.question,
                type = it.type,
                optionsJson = JSONArray(it.options).toString(),
                correctAnswer = it.correctAnswer,
                explanation = it.explanation,
                difficulty = it.difficulty,
                topic = it.topic
            )
        }
        if (questionEntities.isNotEmpty()) {
            studyDao.insertQuestions(questionEntities)
        }

        // Save formulas
        val formulaEntities = packageData.formulas.map { f ->
            val varsJson = JSONArray().apply {
                f.variables.forEach { v ->
                    put(JSONObject().apply {
                        put("variable", v.variable)
                        put("name", v.name)
                        put("unit", v.unit)
                    })
                }
            }.toString()
            FormulaEntity(
                documentId = docId,
                formula = f.formula,
                meaning = f.meaning,
                variablesJson = varsJson
            )
        }
        if (formulaEntities.isNotEmpty()) {
            studyDao.insertFormulas(formulaEntities)
        }

        // Save definitions
        val definitionEntities = packageData.definitions.map {
            DefinitionEntity(
                documentId = docId,
                term = it.term,
                definition = it.definition
            )
        }
        if (definitionEntities.isNotEmpty()) {
            studyDao.insertDefinitions(definitionEntities)
        }

        return docId
    }

    // Record quiz attempt & mistakes
    suspend fun recordQuizResult(
        documentId: Long,
        documentTitle: String,
        subject: String,
        score: Int,
        totalQuestions: Int,
        timeTakenSeconds: Int,
        mistakesList: List<MistakeEntity>,
        weakTopics: List<String>
    ): Long {
        val accuracy = if (totalQuestions > 0) ((score.toFloat() / totalQuestions.toFloat()) * 100).toInt() else 0
        val attemptId = studyDao.insertQuizAttempt(
            QuizAttemptEntity(
                documentId = documentId,
                documentTitle = documentTitle,
                subject = subject,
                score = score,
                totalQuestions = totalQuestions,
                accuracyPercent = accuracy,
                timeTakenSeconds = timeTakenSeconds,
                weakTopicsJson = JSONArray(weakTopics).toString()
            )
        )

        mistakesList.forEach { mistake ->
            studyDao.insertMistake(mistake)
        }

        return attemptId
    }

    // AI Engine delegation methods
    suspend fun extractFromMaterial(
        rawText: String = "",
        imageUris: List<android.net.Uri> = emptyList(),
        isHandwritten: Boolean = true,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): ExtractedNoteData = studyEngine.extractFromMaterial(rawText, imageUris, isHandwritten, onProgress)

    suspend fun generateStudyPackage(
        noteText: String,
        selectedComponents: Set<String>,
        difficulty: String,
        questionCount: Int,
        avoidQuestions: List<String> = emptyList(),
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): GeneratedStudyPackage = studyEngine.generateStudyPackage(
        noteText = noteText,
        selectedComponents = selectedComponents,
        difficulty = difficulty,
        questionCount = questionCount,
        avoidQuestions = avoidQuestions,
        onProgress = onProgress
    )

    suspend fun evaluateShortAnswer(
        question: String,
        modelAnswer: String,
        studentAnswer: String,
        contextText: String
    ): EvaluationResult = studyEngine.evaluateShortAnswer(question, modelAnswer, studentAnswer, contextText)

    suspend fun explainConcept(concept: String, contextText: String): ExplanationBundle =
        studyEngine.explainConcept(concept, contextText)
}
