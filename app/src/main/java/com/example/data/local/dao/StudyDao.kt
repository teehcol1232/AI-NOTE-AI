package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    // Documents
    @Query("SELECT * FROM study_documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<StudyDocumentEntity>>

    @Query("SELECT * FROM study_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): StudyDocumentEntity?

    @Query("SELECT * FROM study_documents WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteDocuments(): Flow<List<StudyDocumentEntity>>

    @Query("SELECT * FROM study_documents WHERE subject = :subject ORDER BY createdAt DESC")
    fun getDocumentsBySubject(subject: String): Flow<List<StudyDocumentEntity>>

    @Query("SELECT * FROM study_documents WHERE title LIKE '%' || :query || '%' OR originalText LIKE '%' || :query || '%' OR subject LIKE '%' || :query || '%'")
    fun searchDocuments(query: String): Flow<List<StudyDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: StudyDocumentEntity): Long

    @Update
    suspend fun updateDocument(document: StudyDocumentEntity)

    @Query("UPDATE study_documents SET title = :newTitle WHERE id = :id")
    suspend fun renameDocument(id: Long, newTitle: String)

    @Query("UPDATE study_documents SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("DELETE FROM study_documents WHERE id = :id")
    suspend fun deleteDocument(id: Long)

    // Generated Materials (Summaries, Key Points, Explanations)
    @Query("SELECT * FROM generated_materials WHERE documentId = :documentId LIMIT 1")
    suspend fun getGeneratedMaterial(documentId: Long): GeneratedMaterialEntity?

    @Query("SELECT * FROM generated_materials WHERE documentId = :documentId LIMIT 1")
    fun observeGeneratedMaterial(documentId: Long): Flow<GeneratedMaterialEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneratedMaterial(material: GeneratedMaterialEntity): Long

    // Flashcards
    @Query("SELECT * FROM flashcards WHERE documentId = :documentId ORDER BY id ASC")
    fun getFlashcardsForDocument(documentId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE documentId = :documentId ORDER BY id ASC")
    suspend fun getFlashcardsSync(documentId: Long): List<FlashcardEntity>

    @Query("SELECT COUNT(*) FROM flashcards")
    fun getTotalFlashcardsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>)

    @Query("UPDATE flashcards SET isMastered = :mastered, needsReview = :needsReview, timesReviewed = timesReviewed + 1 WHERE id = :id")
    suspend fun updateFlashcardReview(id: Long, mastered: Boolean, needsReview: Boolean)

    @Query("UPDATE flashcards SET isBookmarked = NOT isBookmarked WHERE id = :id")
    suspend fun toggleFlashcardBookmark(id: Long)

    // Questions (MCQs & Short-Answers)
    @Query("SELECT * FROM questions WHERE documentId = :documentId")
    fun getQuestionsForDocument(documentId: Long): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE documentId = :documentId")
    suspend fun getQuestionsSync(documentId: Long): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    // Formulas
    @Query("SELECT * FROM formulas WHERE documentId = :documentId")
    fun getFormulasForDocument(documentId: Long): Flow<List<FormulaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFormulas(formulas: List<FormulaEntity>)

    @Query("UPDATE formulas SET isBookmarked = NOT isBookmarked WHERE id = :id")
    suspend fun toggleFormulaBookmark(id: Long)

    // Definitions
    @Query("SELECT * FROM definitions WHERE documentId = :documentId")
    fun getDefinitionsForDocument(documentId: Long): Flow<List<DefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefinitions(definitions: List<DefinitionEntity>)

    @Query("UPDATE definitions SET isBookmarked = NOT isBookmarked WHERE id = :id")
    suspend fun toggleDefinitionBookmark(id: Long)

    // Quiz Attempts
    @Query("SELECT * FROM quiz_attempts ORDER BY timestamp DESC")
    fun getAllQuizAttempts(): Flow<List<QuizAttemptEntity>>

    @Query("SELECT * FROM quiz_attempts WHERE id = :id LIMIT 1")
    suspend fun getQuizAttemptById(id: Long): QuizAttemptEntity?

    @Query("SELECT COUNT(*) FROM quiz_attempts")
    fun getTotalQuizzesCount(): Flow<Int>

    @Query("SELECT AVG(accuracyPercent) FROM quiz_attempts")
    fun getAverageQuizScore(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizAttempt(attempt: QuizAttemptEntity): Long

    // Mistakes
    @Query("SELECT * FROM mistakes WHERE resolved = 0 ORDER BY timestamp DESC")
    fun getUnresolvedMistakes(): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes ORDER BY timestamp DESC")
    fun getAllMistakes(): Flow<List<MistakeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMistake(mistake: MistakeEntity)

    @Query("UPDATE mistakes SET resolved = 1 WHERE id = :id")
    suspend fun markMistakeResolved(id: Long)

    @Query("DELETE FROM mistakes WHERE id = :id")
    suspend fun deleteMistake(id: Long)

    // Subjects
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: Long)
}
