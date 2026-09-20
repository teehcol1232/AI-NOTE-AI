package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.StudyDao
import com.example.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        StudyDocumentEntity::class,
        GeneratedMaterialEntity::class,
        FlashcardEntity::class,
        QuestionEntity::class,
        FormulaEntity::class,
        DefinitionEntity::class,
        QuizAttemptEntity::class,
        MistakeEntity::class,
        SubjectEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studyDao(): StudyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_study_notes.db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate standard subjects and sample initial document
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(context).studyDao()
                            seedInitialData(dao)
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(dao: StudyDao) {
            val defaultSubjects = listOf(
                SubjectEntity(name = "Science", iconName = "Science", colorHex = "#0EA5E9"),
                SubjectEntity(name = "Physics", iconName = "Bolt", colorHex = "#6366F1"),
                SubjectEntity(name = "Chemistry", iconName = "Science", colorHex = "#8B5CF6"),
                SubjectEntity(name = "Biology", iconName = "Eco", colorHex = "#10B981"),
                SubjectEntity(name = "Mathematics", iconName = "Calculate", colorHex = "#EC4899"),
                SubjectEntity(name = "Computer Science", iconName = "Terminal", colorHex = "#3B82F6"),
                SubjectEntity(name = "History", iconName = "HistoryEdu", colorHex = "#F59E0B"),
                SubjectEntity(name = "Literature", iconName = "MenuBook", colorHex = "#14B8A6")
            )
            defaultSubjects.forEach { dao.insertSubject(it) }

            // Insert initial welcome study document as example (Physics - Electricity & Ohm's Law)
            val docId = dao.insertDocument(
                StudyDocumentEntity(
                    title = "Physics — Electricity & Circuits",
                    subject = "Physics",
                    subTopic = "Ohm's Law and Circuit Analysis",
                    originalText = """
                        Physics Study Notes: Electric Circuits and Ohm's Law
                        
                        1. Fundamental Concepts
                        - Electric Current (I): The rate of flow of electric charge through a conductor. Measured in Amperes (A). I = Q / t.
                        - Voltage (V) or Potential Difference: The electric potential energy per unit charge between two points. Measured in Volts (V).
                        - Resistance (R): The opposition to the flow of electric current. Measured in Ohms (Ω).
                        
                        2. Ohm's Law
                        - Statement: Current through a conductor between two points is directly proportional to the voltage across the two points, provided physical conditions (temperature) remain constant.
                        - Formula: V = I * R
                        - Equivalent forms: I = V / R, R = V / I
                        
                        3. Electric Power
                        - Electrical energy transferred per unit time. Measured in Watts (W).
                        - Formulas: P = V * I = I^2 * R = V^2 / R
                        
                        4. Resistors in Circuits
                        - Series: Total resistance is the sum of individual resistances: R_total = R1 + R2 + R3. Current remains identical across all series components.
                        - Parallel: 1 / R_total = 1 / R1 + 1 / R2 + 1 / R3. Voltage remains identical across each parallel branch.
                        
                        5. Key Definitions
                        - Direct Current (DC): Unidirectional flow of electric charge.
                        - Alternating Current (AC): Electric charge periodically reverses direction.
                    """.trimIndent(),
                    materialType = "TEXT",
                    difficulty = "Medium"
                )
            )

            // Generated summary
            dao.insertGeneratedMaterial(
                GeneratedMaterialEntity(
                    documentId = docId,
                    summaryShort = "Electric circuits rely on three interdependent quantities: Voltage (V), Current (I), and Resistance (R), bound by Ohm's Law (V = IR). Power is dissipated according to P = VI.",
                    summaryMedium = "This study note covers the fundamentals of electric circuits. Current (I) measures charge flow in Amperes, Voltage (V) is the potential difference driving current, and Resistance (R) opposes that flow. Ohm's Law asserts V = IR under constant temperature. In series circuits, resistances sum up linearly and current is constant; in parallel circuits, the inverse resistances sum up and voltage is constant across branches.",
                    summaryDetailed = "Electric circuits are foundational to physics and electrical engineering. The chapter establishes definitions for Current (I = Q/t in Amperes), Voltage (energy per charge in Volts), and Resistance (Ohms). Ohm's Law (V = IR) defines the linear relationship between potential difference and current. Circuit topologies diverge into Series (R_eq = R1 + R2 + ..., constant current) and Parallel (1/R_eq = 1/R1 + 1/R2 + ..., constant potential difference). Electrical power calculates rate of energy expenditure (P = VI = I²R = V²/R in Watts).",
                    keyPointsJson = """
                        ["Ohm's Law: V = I * R governs linear conductors under steady temperature.", "Current (I) is measured in Amperes; Voltage (V) in Volts; Resistance (R) in Ohms.", "Power equations: P = VI, P = I²R, P = V²/R (measured in Watts).", "Series circuits: Resistance adds up linearly; current is uniform throughout.", "Parallel circuits: Inverse resistances add up; voltage is uniform across branches."]
                    """.trimIndent(),
                    explanation = "Electric circuits can be visualized like water flowing through pipes: Voltage is the water pressure, Current is the flow rate of water, and Resistance is the constriction in the pipe narrowing the flow."
                )
            )

            // Flashcards
            val flashcards = listOf(
                FlashcardEntity(
                    documentId = docId,
                    front = "What does Ohm's Law state mathematically?",
                    back = "V = I * R (Voltage equals Current multiplied by Resistance)."
                ),
                FlashcardEntity(
                    documentId = docId,
                    front = "What is the SI unit of Electric Current?",
                    back = "Ampere (A), where 1 A = 1 Coulomb per second."
                ),
                FlashcardEntity(
                    documentId = docId,
                    front = "How does total resistance behave in a series circuit?",
                    back = "It is the sum of all individual resistors: R_total = R1 + R2 + ... + Rn."
                ),
                FlashcardEntity(
                    documentId = docId,
                    front = "What stays constant across branches in a parallel circuit?",
                    back = "Voltage (Potential Difference) is identical across every parallel branch."
                ),
                FlashcardEntity(
                    documentId = docId,
                    front = "What is the formula for Electric Power in terms of current and resistance?",
                    back = "P = I² * R (Power equals current squared times resistance, measured in Watts)."
                )
            )
            dao.insertFlashcards(flashcards)

            // Questions
            val questions = listOf(
                QuestionEntity(
                    documentId = docId,
                    questionText = "Which equation correctly represents Ohm's Law?",
                    type = "MCQ",
                    optionsJson = """["V = I * R", "P = V * I", "F = m * a", "E = m * c²"]""",
                    correctAnswer = "V = I * R",
                    explanation = "Ohm's Law states that Voltage (V) is directly proportional to current (I) times resistance (R).",
                    difficulty = "Easy",
                    topic = "Ohm's Law"
                ),
                QuestionEntity(
                    documentId = docId,
                    questionText = "If a 12V battery is connected across a 4Ω resistor, what current flows through the circuit?",
                    type = "MCQ",
                    optionsJson = """["3 A", "48 A", "0.33 A", "16 A"]""",
                    correctAnswer = "3 A",
                    explanation = "Using I = V / R: I = 12V / 4Ω = 3 Amperes.",
                    difficulty = "Medium",
                    topic = "Circuit Calculations"
                ),
                QuestionEntity(
                    documentId = docId,
                    questionText = "In a series circuit with two resistors (10Ω and 20Ω), what is the total equivalent resistance?",
                    type = "MCQ",
                    optionsJson = """["30 Ω", "6.67 Ω", "200 Ω", "10 Ω"]""",
                    correctAnswer = "30 Ω",
                    explanation = "In series, resistances add directly: R_total = 10Ω + 20Ω = 30Ω.",
                    difficulty = "Easy",
                    topic = "Series Circuits"
                ),
                QuestionEntity(
                    documentId = docId,
                    questionText = "Explain how voltage and current behave differently in series vs. parallel circuits.",
                    type = "SHORT_ANSWER",
                    optionsJson = "[]",
                    correctAnswer = "In series, current is constant through all elements and voltage divides. In parallel, voltage is constant across all branches and current divides.",
                    explanation = "Series circuits feature single path flow (constant current), while parallel circuits share common junction nodes (constant voltage).",
                    difficulty = "Medium",
                    topic = "Circuit Topologies"
                )
            )
            dao.insertQuestions(questions)

            // Formulas
            val formulas = listOf(
                FormulaEntity(
                    documentId = docId,
                    formula = "V = I * R",
                    meaning = "Ohm's Law relating voltage, current, and resistance",
                    variablesJson = """[{"variable":"V","name":"Voltage","unit":"Volts (V)"},{"variable":"I","name":"Current","unit":"Amperes (A)"},{"variable":"R","name":"Resistance","unit":"Ohms (Ω)"}]"""
                ),
                FormulaEntity(
                    documentId = docId,
                    formula = "P = V * I = I²R = V²/R",
                    meaning = "Electric Power dissipation",
                    variablesJson = """[{"variable":"P","name":"Power","unit":"Watts (W)"},{"variable":"V","name":"Voltage","unit":"Volts (V)"},{"variable":"I","name":"Current","unit":"Amperes (A)"},{"variable":"R","name":"Resistance","unit":"Ohms (Ω)"}]"""
                ),
                FormulaEntity(
                    documentId = docId,
                    formula = "1 / R_p = 1/R1 + 1/R2",
                    meaning = "Equivalent resistance for parallel resistors",
                    variablesJson = """[{"variable":"R_p","name":"Equivalent Parallel Resistance","unit":"Ohms (Ω)"}]"""
                )
            )
            dao.insertFormulas(formulas)

            // Definitions
            val definitions = listOf(
                DefinitionEntity(
                    documentId = docId,
                    term = "Electric Current",
                    definition = "The rate of net flow of electric charge through a defined cross-sectional conductor area over time (I = Q/t)."
                ),
                DefinitionEntity(
                    documentId = docId,
                    term = "Potential Difference (Voltage)",
                    definition = "The work done per unit electric charge in moving a test charge between two points in an electric field."
                ),
                DefinitionEntity(
                    documentId = docId,
                    term = "Resistance",
                    definition = "The intrinsic property of a conductor to impede or oppose the movement of mobile electrons."
                )
            )
            dao.insertDefinitions(definitions)
        }
    }
}
