package com.example.data.ai

object StudyPrompts {

    fun buildExtractionPrompt(isHandwritten: Boolean): String = """
        You are an advanced OCR and document understanding specialist.
        Analyze the provided image(s) of ${if (isHandwritten) "handwritten study notes" else "printed study document/PDF"}.
        1. Extract all text faithfully and accurately.
        2. Preserve mathematical formulas, scientific equations, and technical symbols.
        3. Identify structural elements: chapter titles, main headings, sub-headings, bullet points, numbered lists, and tables.
        4. Transcribe accurately without introducing hallucinated facts.
        
        Return ONLY valid JSON in this structure:
        {
          "title": "Detected Title or Chapter",
          "subject": "Physics/Biology/Math/etc.",
          "extractedText": "Full formatted clean text...",
          "headings": ["Heading 1", "Heading 2"],
          "bulletPoints": ["Key point 1", "Key point 2"],
          "detectedFormulas": ["Formula 1", "Formula 2"],
          "confidencePercent": 95
        }
    """.trimIndent()

    fun buildFullStudyPackagePrompt(
        noteContent: String,
        selectedComponents: Set<String>, // "SUMMARY", "KEY_POINTS", "FLASHCARDS", "MCQS", "SHORT_QUESTIONS", "FORMULAS", "DEFINITIONS"
        difficulty: String,
        questionCount: Int
    ): String = """
        You are an elite educational AI tutor. Based ONLY on the provided study notes below, generate a comprehensive, structured study package.
        Do NOT invent outside facts unless necessary to explain context.
        Difficulty: $difficulty. Target question count: $questionCount.

        STUDY NOTES:
        $noteContent

        Produce ONLY a valid JSON object matching this schema:
        {
          "title": "Concise Descriptive Title",
          "subject": "Main Subject Category",
          "subTopic": "Specific Chapter/Unit",
          "summaryShort": "1-2 punchy sentences summarizing the core takeaway",
          "summaryMedium": "A balanced 1-2 paragraph summary preserving essential facts",
          "summaryDetailed": "A deep, comprehensive multi-paragraph explanation with structured flow",
          "keyPoints": [
            "Critical takeaway point 1",
            "Critical takeaway point 2"
          ],
          "definitions": [
            {
              "term": "Term Name",
              "definition": "Clear, precise academic definition grounded in the text"
            }
          ],
          "formulas": [
            {
              "formula": "Equation (e.g. V = I * R)",
              "meaning": "What this equation calculates or represents",
              "variables": [
                { "variable": "V", "name": "Voltage", "unit": "Volts" }
              ]
            }
          ],
          "flashcards": [
            {
              "front": "Focused question or concept prompt?",
              "back": "Clear, concise direct answer"
            }
          ],
          "mcqs": [
            {
              "question": "Clear multiple-choice question testing understanding?",
              "type": "MCQ",
              "options": ["Correct Answer", "Distractor 1", "Distractor 2", "Distractor 3"],
              "correctAnswer": "Correct Answer",
              "explanation": "Why this answer is correct and why others are wrong",
              "difficulty": "$difficulty",
              "topic": "Specific Topic"
            }
          ],
          "shortQuestions": [
            {
              "question": "Conceptual or analytical question requiring a short written response?",
              "type": "SHORT_ANSWER",
              "correctAnswer": "Model ideal answer detailing required points",
              "explanation": "Key concepts that must be included in the answer",
              "difficulty": "$difficulty",
              "topic": "Specific Topic"
            }
          ],
          "explanation": "An intuitive, student-friendly visual/analogical explanation of the most challenging concept."
        }
    """.trimIndent()

    fun buildEvaluateShortAnswerPrompt(
        question: String,
        modelAnswer: String,
        userAnswer: String,
        notesContext: String
    ): String = """
        You are a friendly, encouraging academic grader. Evaluate the student's short written answer against the question and study context.
        
        Question: $question
        Ideal Answer: $modelAnswer
        Student Answer: $userAnswer
        Study Context: $notesContext
        
        Rate as:
        - "CORRECT": Student grasped the core facts accurately.
        - "PARTIALLY_CORRECT": Partially accurate or missing a key detail.
        - "NEEDS_IMPROVEMENT": Misunderstood concept or missing crucial facts.
        
        Return ONLY valid JSON:
        {
          "status": "CORRECT" / "PARTIALLY_CORRECT" / "NEEDS_IMPROVEMENT",
          "feedback": "Encouraging constructive feedback (2-3 sentences)",
          "missingElements": ["Element 1 if any", "Element 2 if any"],
          "suggestedAnswer": "Complete polished answer"
        }
    """.trimIndent()

    fun buildExplainConceptPrompt(concept: String, context: String): String = """
        You are a world-class teacher. Explain the following concept from study notes in three distinct styles:
        Concept: $concept
        Context: $context

        Return ONLY valid JSON:
        {
          "term": "$concept",
          "simpleExplanation": "Explain as if to a 10-year old using everyday language and no heavy jargon.",
          "detailedExplanation": "Rigorous academic explanation with theoretical precision.",
          "exampleBasedExplanation": "A concrete real-world scenario or tangible analogy that makes it crystal clear."
        }
    """.trimIndent()
}
