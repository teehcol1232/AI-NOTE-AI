package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.StudyEngine
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.StudyRepository
import com.example.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class Screen {
    HOME,
    LIBRARY,
    CREATE,
    QUIZ_HUB,
    PROFILE,
    EXTRACT_REVIEW,
    GENERATE_CONFIG,
    PROCESSING,
    DOCUMENT_DETAIL,
    FLASHCARD_STUDY,
    MCQ_QUIZ,
    PRACTICE_TEST,
    MISTAKES_PRACTICE
}

data class CreateFlowState(
    val rawText: String = "",
    val imageUris: List<Uri> = emptyList(),
    val materialType: String = "TEXT", // "CAMERA", "IMAGE", "PDF", "TEXT"
    val isHandwritten: Boolean = true,
    val extractedData: ExtractedNoteData? = null,
    val selectedComponents: Set<String> = setOf(
        "SUMMARY", "KEY_POINTS", "FLASHCARDS", "MCQS", "SHORT_QUESTIONS", "DEFINITIONS", "FORMULAS"
    ),
    val difficulty: String = "Medium",
    val questionCount: Int = 10,
    val processingStage: String = "",
    val processingPercent: Float = 0f,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyRepository

    init {
        val db = AppDatabase.getInstance(application)
        val engine = StudyEngine(application)
        repository = StudyRepository(db.studyDao(), engine)
    }

    // Navigation State
    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Navigation argument
    private val _selectedDocumentId = MutableStateFlow<Long?>(null)
    val selectedDocumentId: StateFlow<Long?> = _selectedDocumentId.asStateFlow()

    // Create flow state
    private val _createState = MutableStateFlow(CreateFlowState())
    val createState: StateFlow<CreateFlowState> = _createState.asStateFlow()

    // Library search & filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSubjectFilter = MutableStateFlow<String?>(null)
    val selectedSubjectFilter: StateFlow<String?> = _selectedSubjectFilter.asStateFlow()

    // Repository flows
    val allDocuments: StateFlow<List<StudyDocumentEntity>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quizAttempts: StateFlow<List<QuizAttemptEntity>> = repository.allQuizAttempts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unresolvedMistakes: StateFlow<List<MistakeEntity>> = repository.unresolvedMistakes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFlashcardsCount: StateFlow<Int> = repository.totalFlashcardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalQuizzesCount: StateFlow<Int> = repository.totalQuizzesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val averageQuizScore: StateFlow<Double?> = repository.averageQuizScore
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val subjects: StateFlow<List<SubjectEntity>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered documents for Library
    val filteredDocuments: StateFlow<List<StudyDocumentEntity>> = combine(
        allDocuments,
        _searchQuery,
        _selectedSubjectFilter
    ) { docs, query, subject ->
        docs.filter { doc ->
            val matchesQuery = query.isBlank() ||
                doc.title.contains(query, ignoreCase = true) ||
                doc.subject.contains(query, ignoreCase = true) ||
                doc.originalText.contains(query, ignoreCase = true)
            val matchesSubject = subject == null || doc.subject.equals(subject, ignoreCase = true)
            matchesQuery && matchesSubject
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Document Detail
    private val _activeDocument = MutableStateFlow<StudyDocumentEntity?>(null)
    val activeDocument: StateFlow<StudyDocumentEntity?> = _activeDocument.asStateFlow()

    private val _activeMaterial = MutableStateFlow<GeneratedMaterialEntity?>(null)
    val activeMaterial: StateFlow<GeneratedMaterialEntity?> = _activeMaterial.asStateFlow()

    private val _activeFlashcards = MutableStateFlow<List<FlashcardEntity>>(emptyList())
    val activeFlashcards: StateFlow<List<FlashcardEntity>> = _activeFlashcards.asStateFlow()

    private val _activeQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val activeQuestions: StateFlow<List<QuestionEntity>> = _activeQuestions.asStateFlow()

    private val _activeFormulas = MutableStateFlow<List<FormulaEntity>>(emptyList())
    val activeFormulas: StateFlow<List<FormulaEntity>> = _activeFormulas.asStateFlow()

    private val _activeDefinitions = MutableStateFlow<List<DefinitionEntity>>(emptyList())
    val activeDefinitions: StateFlow<List<DefinitionEntity>> = _activeDefinitions.asStateFlow()

    // Concept Explanation State
    private val _activeExplanation = MutableStateFlow<ExplanationBundle?>(null)
    val activeExplanation: StateFlow<ExplanationBundle?> = _activeExplanation.asStateFlow()
    val isExplaining = MutableStateFlow(false)

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun openDocumentDetail(documentId: Long) {
        _selectedDocumentId.value = documentId
        viewModelScope.launch {
            _activeDocument.value = repository.getDocumentById(documentId)
            _activeMaterial.value = repository.getGeneratedMaterial(documentId)
            _activeFlashcards.value = repository.getFlashcardsSync(documentId)
            _activeQuestions.value = repository.getQuestionsSync(documentId)
            _currentScreen.value = Screen.DOCUMENT_DETAIL

            // Also collect dynamic updates
            launch {
                repository.getFlashcardsForDocument(documentId).collect {
                    _activeFlashcards.value = it
                }
            }
            launch {
                repository.getFormulasForDocument(documentId).collect {
                    _activeFormulas.value = it
                }
            }
            launch {
                repository.getDefinitionsForDocument(documentId).collect {
                    _activeDefinitions.value = it
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSubjectFilter(subject: String?) {
        _selectedSubjectFilter.value = subject
    }

    fun toggleFavorite(documentId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(documentId)
        }
    }

    fun renameDocument(documentId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameDocument(documentId, newTitle)
            if (_activeDocument.value?.id == documentId) {
                _activeDocument.value = _activeDocument.value?.copy(title = newTitle)
            }
        }
    }

    fun deleteDocument(documentId: Long) {
        viewModelScope.launch {
            repository.deleteDocument(documentId)
            if (_selectedDocumentId.value == documentId) {
                _currentScreen.value = Screen.LIBRARY
            }
        }
    }

    // --- Create Flow Functions ---

    fun startCreateWithText(text: String) {
        _createState.value = CreateFlowState(
            rawText = text,
            materialType = "TEXT",
            isHandwritten = false
        )
        processExtraction()
    }

    fun startCreateWithImages(uris: List<Uri>, isCamera: Boolean = false, isHandwritten: Boolean = true) {
        _createState.value = CreateFlowState(
            imageUris = uris,
            materialType = if (isCamera) "CAMERA" else "IMAGE",
            isHandwritten = isHandwritten
        )
        processExtraction()
    }

    fun setExtractedNotesText(updatedText: String) {
        val current = _createState.value.extractedData ?: return
        _createState.value = _createState.value.copy(
            extractedData = current.copy(extractedText = updatedText)
        )
    }

    fun setGenerationOptions(
        selectedComponents: Set<String>,
        difficulty: String,
        questionCount: Int
    ) {
        _createState.value = _createState.value.copy(
            selectedComponents = selectedComponents,
            difficulty = difficulty,
            questionCount = questionCount
        )
    }

    fun processExtraction() {
        val state = _createState.value
        _createState.value = state.copy(
            isProcessing = true,
            processingStage = "Reading your material...",
            processingPercent = 0.2f,
            errorMessage = null
        )
        _currentScreen.value = Screen.PROCESSING

        viewModelScope.launch {
            try {
                val extracted = repository.extractFromMaterial(
                    rawText = state.rawText,
                    imageUris = state.imageUris,
                    isHandwritten = state.isHandwritten,
                    onProgress = { stage, percent ->
                        _createState.value = _createState.value.copy(
                            processingStage = stage,
                            processingPercent = percent
                        )
                    }
                )
                _createState.value = _createState.value.copy(
                    isProcessing = false,
                    extractedData = extracted
                )
                _currentScreen.value = Screen.EXTRACT_REVIEW
            } catch (e: Exception) {
                _createState.value = _createState.value.copy(
                    isProcessing = false,
                    errorMessage = "Failed to process notes: ${e.message}"
                )
            }
        }
    }

    fun executeGeneration() {
        val state = _createState.value
        val extracted = state.extractedData ?: return
        _createState.value = state.copy(
            isProcessing = true,
            processingStage = "Organizing concepts...",
            processingPercent = 0.3f,
            errorMessage = null
        )
        _currentScreen.value = Screen.PROCESSING

        viewModelScope.launch {
            try {
                val studyPackage = repository.generateStudyPackage(
                    noteText = extracted.extractedText,
                    selectedComponents = state.selectedComponents,
                    difficulty = state.difficulty,
                    questionCount = state.questionCount,
                    onProgress = { stage, percent ->
                        _createState.value = _createState.value.copy(
                            processingStage = stage,
                            processingPercent = percent
                        )
                    }
                )

                // Save into Room DB
                val savedId = repository.saveStudyPackage(
                    originalText = extracted.extractedText,
                    materialType = state.materialType,
                    packageData = studyPackage,
                    difficulty = state.difficulty
                )

                _createState.value = _createState.value.copy(isProcessing = false)
                openDocumentDetail(savedId)
            } catch (e: Exception) {
                _createState.value = _createState.value.copy(
                    isProcessing = false,
                    errorMessage = "Generation error: ${e.message}"
                )
            }
        }
    }

    // --- Study Actions ---

    fun updateFlashcard(cardId: Long, mastered: Boolean, needsReview: Boolean) {
        viewModelScope.launch {
            repository.updateFlashcardReview(cardId, mastered, needsReview)
        }
    }

    fun toggleFlashcardBookmark(cardId: Long) {
        viewModelScope.launch {
            repository.toggleFlashcardBookmark(cardId)
        }
    }

    fun toggleFormulaBookmark(formulaId: Long) {
        viewModelScope.launch {
            repository.toggleFormulaBookmark(formulaId)
        }
    }

    fun toggleDefinitionBookmark(defId: Long) {
        viewModelScope.launch {
            repository.toggleDefinitionBookmark(defId)
        }
    }

    fun submitQuizResult(
        documentId: Long,
        documentTitle: String,
        subject: String,
        score: Int,
        total: Int,
        timeTakenSeconds: Int,
        mistakes: List<MistakeEntity>,
        weakTopics: List<String>
    ) {
        viewModelScope.launch {
            repository.recordQuizResult(
                documentId = documentId,
                documentTitle = documentTitle,
                subject = subject,
                score = score,
                totalQuestions = total,
                timeTakenSeconds = timeTakenSeconds,
                mistakesList = mistakes,
                weakTopics = weakTopics
            )
        }
    }

    fun markMistakeResolved(mistakeId: Long) {
        viewModelScope.launch {
            repository.markMistakeResolved(mistakeId)
        }
    }

    fun deleteMistake(mistakeId: Long) {
        viewModelScope.launch {
            repository.deleteMistake(mistakeId)
        }
    }

    fun explainConcept(concept: String) {
        val context = _activeDocument.value?.originalText ?: ""
        isExplaining.value = true
        _activeExplanation.value = null
        viewModelScope.launch {
            val bundle = repository.explainConcept(concept, context)
            _activeExplanation.value = bundle
            isExplaining.value = false
        }
    }

    fun clearActiveExplanation() {
        _activeExplanation.value = null
        isExplaining.value = false
    }

    suspend fun evaluateWrittenAnswer(
        question: String,
        modelAnswer: String,
        userAnswer: String
    ): EvaluationResult {
        val context = _activeDocument.value?.originalText ?: ""
        return repository.evaluateShortAnswer(question, modelAnswer, userAnswer, context)
    }

    // Quick presets for 1-tap instant demo
    fun loadPresetStudySet(presetType: String) {
        val (text, subject, title) = when (presetType) {
            "BIOLOGY" -> Triple(
                """
                    Cellular Respiration and Photosynthesis:
                    1. Photosynthesis occurs in chloroplasts: 6CO2 + 6H2O + light -> C6H12O6 + 6O2.
                    2. Light-dependent reactions generate ATP and NADPH in thylakoids.
                    3. Calvin Cycle synthesizes glucose in stroma.
                    4. Cellular Respiration in mitochondria: C6H12O6 + 6O2 -> 6CO2 + 6H2O + ~36 ATP.
                    5. Glycolysis breaks down 1 glucose into 2 pyruvate, yielding net 2 ATP.
                    6. Krebs Cycle (Citric Acid Cycle) oxidizes acetyl-CoA to CO2, reducing NAD+ to NADH.
                    7. Electron Transport Chain creates proton gradient across inner mitochondrial membrane.
                """.trimIndent(),
                "Biology",
                "Biology — Photosynthesis & Respiration"
            )
            "COMPUTER_SCIENCE" -> Triple(
                """
                    Operating Systems: Concurrency and Thread Management:
                    1. Process: An executing instance of a program with its own dedicated memory address space.
                    2. Thread: Lightweight unit of execution within a process sharing heap, code, and global memory.
                    3. Race Condition: When multiple threads access shared data concurrently and the outcome depends on execution timing.
                    4. Deadlock: Four Coffman conditions required: Mutual exclusion, Hold and wait, No preemption, Circular wait.
                    5. Critical Section: Code segment accessing shared resources protected by Mutexes or Semaphores.
                    6. Context Switch: Storing and restoring the execution state (registers, program counter) of a thread.
                """.trimIndent(),
                "Computer Science",
                "Computer Science — Concurrency & OS"
            )
            else -> Triple(
                """
                    Calculus — Derivatives and Rates of Change:
                    1. Definition of Derivative: f'(x) = lim (h -> 0) [f(x + h) - f(x)] / h.
                    2. Power Rule: d/dx [x^n] = n * x^(n - 1).
                    3. Product Rule: d/dx [u * v] = u'v + uv'.
                    4. Quotient Rule: d/dx [u / v] = (u'v - uv') / v².
                    5. Chain Rule: d/dx [f(g(x))] = f'(g(x)) * g'(x).
                    6. Critical Points: Occur where f'(x) = 0 or f'(x) is undefined.
                """.trimIndent(),
                "Mathematics",
                "Mathematics — Calculus & Derivatives"
            )
        }

        _createState.value = CreateFlowState(
            rawText = text,
            materialType = "TEXT",
            isHandwritten = false
        )
        processExtraction()
    }
}

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
