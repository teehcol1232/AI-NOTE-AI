package com.example.data.ai

object StudyPrompts {

    fun buildExtractionPrompt(isHandwritten: Boolean): String = """
        You are an advanced OCR, computer vision, and document analysis specialist.
        Examine the provided image(s) carefully.
        
        Step 1 - LEGIBILITY & RELEVANCE CHECK:
        - Determine if the image contains readable study material, handwritten notes, printed text, diagrams, or formulas.
        - If the image is completely blank, dark, blurry beyond recognition, or contains completely unrelated objects (such as pets, scenery, food, or random objects without text), set "isLegible": false and provide an informative "rejectionReason".
        
        Step 2 - FIDELITY EXTRACTION (if legible):
        - Transcribe all text faithfully without hallucinating outside facts.
        - Preserve scientific equations, mathematical formulas, and chemical notations accurately.
        - Identify structural elements: chapter titles, main headings, sub-headings, bullet points, and numbered lists.
        - Detect the subject domain (e.g. Chemistry, Biology, Physics, Mathematics, History, Literature, Computer Science) directly from the text.
        
        Return ONLY valid JSON matching this schema:
        {
          "isLegible": true,
          "rejectionReason": null,
          "title": "Detected Chapter or Topic Title",
          "subject": "Detected Subject (e.g. Chemistry)",
          "extractedText": "Full formatted text transcribed from the image(s)...",
          "headings": ["Section Heading 1", "Section Heading 2"],
          "bulletPoints": ["Key point 1", "Key point 2"],
          "detectedFormulas": ["Formula or reaction 1", "Formula or reaction 2"],
          "confidencePercent": 95
        }
        If the image is not legible or has no study text, return:
        {
          "isLegible": false,
          "rejectionReason": "The image does not contain readable study notes. Please capture a clear, well-lit photo of your written or printed notes.",
          "title": "Unreadable Image",
          "subject": "General",
          "extractedText": "",
          "headings": [],
          "bulletPoints": [],
          "detectedFormulas": [],
          "confidencePercent": 0
        }
    """.trimIndent()

    fun buildTextStructurePrompt(rawText: String): String = """
        You are an educational document analysis specialist.
        Analyze the student's study text below and extract its core structural components faithfully.
        
        STUDY TEXT:
        $rawText
        
        Return ONLY valid JSON:
        {
          "title": "Descriptive Topic Title based strictly on the text",
          "subject": "Subject Category (e.g. Chemistry, Biology, Physics, Computer Science, Literature, etc.)",
          "headings": ["Key Topic 1", "Key Topic 2"],
          "bulletPoints": ["Summary bullet 1", "Summary bullet 2", "Summary bullet 3"],
          "detectedFormulas": ["Formula 1 if any", "Formula 2 if any"],
          "confidencePercent": 98
        }
    """.trimIndent()

    fun buildFullStudyPackagePrompt(
        noteContent: String,
        selectedComponents: Set<String>,
        difficulty: String,
        questionCount: Int,
        avoidQuestions: List<String> = emptyList()
    ): String {
        val antiRepetitionClause = if (avoidQuestions.isNotEmpty()) {
            """
            ANTI-REPETITION MANDATE:
            The user has already practiced the following questions:
            ${avoidQuestions.take(15).joinToString("\n") { "- $it" }}
            You MUST generate COMPLETELY FRESH questions testing different concepts, deeper mechanisms, or alternate applications from the notes.
            """.trimIndent()
        } else ""

        return """
        You are an elite educational AI tutor and exam creator.
        Generate a high-yield study package based EXCLUSIVELY on the provided study notes.
        
        STRICT GROUNDING DIRECTIVES (CRITICAL):
        1. GROUNDING: Every question, option, flashcard, definition, and summary MUST be derived strictly from the provided notes.
        2. NO HALLUCINATIONS: Do NOT invent unrelated concepts. For example, if the notes are about Chemistry (e.g., Acids & Bases, Organic Chemistry), DO NOT generate questions about Physics, E = mc², velocity, or unrelated generic trivia.
        3. NO FORMULAS UNLESS IN NOTES: If the provided notes do not contain mathematical or scientific equations (e.g. history, literature, or conceptual notes), the "formulas" array MUST BE EMPTY ([]). NEVER insert "E = mc²" or dummy equations!
        4. DYNAMIC MULTIPLE CHOICE QUESTIONS (MCQs):
           - Generate $questionCount distinct MCQs testing deep understanding.
           - Every MCQ must have exactly 4 unique, plausible options.
           - All 4 options must be subject-appropriate (e.g. if the question is about Bronsted-Lowry acids, all options should be chemical species or chemical definitions, not random physics terms).
           - Exactly one option is the correct answer matching the notes.
           - Randomly place the correct answer among options (do not always place it first).
           - Provide a thorough explanation citing the rationale from the notes.
        5. FLASHCARDS: High-impact active recall cards testing key definitions, mechanisms, and factual relationships from the notes.
        6. SHORT QUESTIONS: Thought-provoking conceptual questions with comprehensive reference answers.
        
        $antiRepetitionClause
        
        Target Difficulty: $difficulty. Question Count: $questionCount.
        
        STUDY NOTES CONTENT:
        $noteContent

        Return ONLY a valid JSON object matching this schema:
        {
          "title": "Descriptive Title Grounded in Notes",
          "subject": "Detected Subject",
          "subTopic": "Specific Topic",
          "summaryShort": "1-2 punchy sentences summarizing the core takeaway",
          "summaryMedium": "A balanced 1-2 paragraph summary preserving essential facts",
          "summaryDetailed": "A deep, comprehensive multi-paragraph explanation with structured flow",
          "keyPoints": [
            "Critical takeaway point 1 grounded in notes",
            "Critical takeaway point 2 grounded in notes"
          ],
          "definitions": [
            {
              "term": "Term Name",
              "definition": "Precise definition directly from or based on the notes"
            }
          ],
          "formulas": [
            {
              "formula": "Equation that appears in the notes (leave array empty if notes contain no formulas)",
              "meaning": "What this equation calculates or represents",
              "variables": [
                { "variable": "V", "name": "Variable Name", "unit": "Unit" }
              ]
            }
          ],
          "flashcards": [
            {
              "front": "Specific question or concept prompt?",
              "back": "Accurate, concise answer grounded in notes"
            }
          ],
          "mcqs": [
            {
              "question": "Clear multiple-choice question testing the notes?",
              "type": "MCQ",
              "options": ["Plausible Option A", "Plausible Option B", "Plausible Option C", "Plausible Option D"],
              "correctAnswer": "The Exact Matching Correct Option",
              "explanation": "Why this answer is correct based on the notes",
              "difficulty": "$difficulty",
              "topic": "Specific Topic"
            }
          ],
          "shortQuestions": [
            {
              "question": "Conceptual question requiring short analytical response?",
              "type": "SHORT_ANSWER",
              "correctAnswer": "Model ideal answer detailing required points",
              "explanation": "Key concepts that must be included",
              "difficulty": "$difficulty",
              "topic": "Specific Topic"
            }
          ],
          "explanation": "An intuitive, student-friendly visual/analogical explanation of the central concept in the notes."
        }
        """.trimIndent()
    }

    fun buildEvaluateShortAnswerPrompt(
        question: String,
        modelAnswer: String,
        userAnswer: String,
        notesContext: String
    ): String = """
        You are a friendly, rigorous academic grader. Evaluate the student's short written answer against the question, ideal answer, and study context.
        
        Question: $question
        Ideal Reference Answer: $modelAnswer
        Student Answer: $userAnswer
        Study Context: $notesContext
        
        Evaluation Standards:
        - "CORRECT": Student grasped the core facts accurately.
        - "PARTIALLY_CORRECT": Partially accurate or missing a key conceptual detail or term.
        - "NEEDS_IMPROVEMENT": Misunderstood concept, factually wrong, or missing essential points.
        
        Return ONLY valid JSON:
        {
          "status": "CORRECT" or "PARTIALLY_CORRECT" or "NEEDS_IMPROVEMENT",
          "feedback": "Encouraging, constructive feedback pointing out strengths and specific corrections (2-3 sentences)",
          "missingElements": ["Specific missing concept or keyword 1", "Specific missing concept or keyword 2"],
          "suggestedAnswer": "Complete polished answer"
        }
    """.trimIndent()

    fun buildExplainConceptPrompt(concept: String, context: String): String = """
        You are an award-winning teacher and tutor. Explain the following concept from the student's study notes in three distinct pedagogical styles:
        
        Concept: $concept
        Notes Context: $context

        Return ONLY valid JSON:
        {
          "term": "$concept",
          "simpleExplanation": "Explain as if to a beginner using simple, clear everyday language without overwhelming jargon.",
          "detailedExplanation": "Rigorous academic explanation with theoretical precision and exact principles.",
          "exampleBasedExplanation": "A concrete real-world scenario, relatable visual analogy, or practical application that makes the concept vivid and intuitive."
        }
    """.trimIndent()
}
