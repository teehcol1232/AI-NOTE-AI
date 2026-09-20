package com.example.ui.screens.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MistakeEntity
import com.example.data.local.entity.QuestionEntity
import com.example.domain.model.EvaluationResult
import com.example.ui.components.AppTopBar
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

@Composable
fun PracticeTestScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val document by viewModel.activeDocument.collectAsState()
    val questions by viewModel.activeQuestions.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var currentIndex by remember { mutableIntStateOf(0) }
    val userMcqAnswers = remember { mutableStateMapOf<Int, String>() }
    val userShortAnswers = remember { mutableStateMapOf<Int, String>() }
    var markedForReview by remember { mutableStateOf(setOf<Int>()) }
    val shortAnswerEvaluations = remember { mutableStateMapOf<Int, EvaluationResult>() }
    val isEvaluatingMap = remember { mutableStateMapOf<Int, Boolean>() }

    var isExamFinished by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableIntStateOf(600) } // 10 minute exam timer

    LaunchedEffect(isExamFinished) {
        while (!isExamFinished && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
            if (remainingSeconds <= 0) {
                isExamFinished = true
            }
        }
    }

    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No test questions generated for this set.")
        }
        return
    }

    val currentQ = questions.getOrNull(currentIndex) ?: questions.first()
    val isMcq = currentQ.type == "MCQ"
    val options = remember(currentQ) {
        if (isMcq) {
            try {
                val arr = JSONArray(currentQ.optionsJson)
                List(arr.length()) { arr.getString(it) }
            } catch (e: Exception) {
                listOf(currentQ.correctAnswer)
            }
        } else emptyList()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Practice Exam",
                subtitle = "Q ${currentIndex + 1} of ${questions.size}",
                onBack = onBack,
                actions = {
                    // Countdown clock badge
                    val mins = remainingSeconds / 60
                    val secs = remainingSeconds % 60
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (remainingSeconds < 120) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (remainingSeconds < 120) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%02d:%02d", mins, secs),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (remainingSeconds < 120) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!isExamFinished) {
                Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                markedForReview = if (markedForReview.contains(currentIndex)) {
                                    markedForReview - currentIndex
                                } else {
                                    markedForReview + currentIndex
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (markedForReview.contains(currentIndex)) Icons.Default.Flag else Icons.Default.OutlinedFlag,
                                contentDescription = null,
                                tint = if (markedForReview.contains(currentIndex)) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (markedForReview.contains(currentIndex)) "Flagged" else "Flag")
                        }

                        Button(
                            onClick = {
                                if (currentIndex < questions.size - 1) {
                                    currentIndex++
                                } else {
                                    isExamFinished = true
                                }
                            },
                            modifier = Modifier.weight(1.4f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (currentIndex < questions.size - 1) "Next Q" else "Submit Exam", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isExamFinished) {
            // Exam Report Card
            var mcqCorrect = 0
            val mistakes = mutableListOf<MistakeEntity>()

            questions.forEachIndexed { i, q ->
                if (q.type == "MCQ") {
                    val ans = userMcqAnswers[i]
                    if (ans == q.correctAnswer) {
                        mcqCorrect++
                    } else {
                        mistakes.add(
                            MistakeEntity(
                                questionId = q.id,
                                documentId = q.documentId,
                                questionText = q.questionText,
                                userAnswer = ans ?: "Unanswered",
                                correctAnswer = q.correctAnswer,
                                explanation = q.explanation,
                                subject = document?.subject ?: "General",
                                topic = q.topic.ifBlank { "General" }
                            )
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Exam Diagnostic Report",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${document?.title ?: "Study Exam"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Performance Breakdown", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("MCQ Score:", style = MaterialTheme.typography.bodyMedium)
                            Text("$mcqCorrect / ${questions.count { it.type == "MCQ" }}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Short Answer Responses:", style = MaterialTheme.typography.bodyMedium)
                            Text("${questions.count { it.type != "MCQ" }} completed", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Questions Flagged for Review:", style = MaterialTheme.typography.bodyMedium)
                            Text("${markedForReview.size}", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Return to Study Set", fontWeight = FontWeight.Bold)
                }
            }
            return@Scaffold
        }

        // Active Exam Question View
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Horizontal Question Navigation Strip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(questions) { idx, _ ->
                    val isCurrent = idx == currentIndex
                    val isAnswered = (userMcqAnswers[idx] != null) || (!userShortAnswers[idx].isNullOrBlank())
                    val isFlagged = markedForReview.contains(idx)

                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable { currentIndex = idx },
                        shape = CircleShape,
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isFlagged -> Color(0xFFF59E0B)
                            isAnswered -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = if (isCurrent) null else CardDefaults.outlinedCardBorder()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    isCurrent -> MaterialTheme.colorScheme.onPrimary
                                    isFlagged -> Color.White
                                    isAnswered -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Question Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (isMcq) "Multiple Choice" else "Short Answer",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (markedForReview.contains(currentIndex)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFFBEB)
                            ) {
                                Text(
                                    text = "FLAGGED",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = currentQ.questionText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 28.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Answer Input
            if (isMcq) {
                // MCQs
                options.forEach { option ->
                    val isSelected = userMcqAnswers[currentIndex] == option
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { userMcqAnswers[currentIndex] = option },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            CardDefaults.outlinedCardBorder()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { userMcqAnswers[currentIndex] = option }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                // Short Answer Written Response
                val currentText = userShortAnswers[currentIndex] ?: ""
                val eval = shortAnswerEvaluations[currentIndex]
                val isEvaluating = isEvaluatingMap[currentIndex] == true

                Column {
                    OutlinedTextField(
                        value = currentText,
                        onValueChange = { userShortAnswers[currentIndex] = it },
                        placeholder = { Text("Write your explanation or answer in your own words...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 240.dp),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isEvaluatingMap[currentIndex] = true
                                    val result = viewModel.evaluateWrittenAnswer(
                                        question = currentQ.questionText,
                                        modelAnswer = currentQ.correctAnswer,
                                        userAnswer = currentText
                                    )
                                    shortAnswerEvaluations[currentIndex] = result
                                    isEvaluatingMap[currentIndex] = false
                                }
                            },
                            enabled = currentText.isNotBlank() && !isEvaluating,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isEvaluating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Grading...")
                            } else {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check with AI Grader")
                            }
                        }
                    }

                    // Evaluation feedback card
                    AnimatedVisibility(visible = eval != null) {
                        eval?.let { res ->
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (res.status) {
                                        "CORRECT" -> Color(0xFFECFDF5)
                                        "PARTIALLY_CORRECT" -> Color(0xFFFFFBEB)
                                        else -> Color(0xFFFEF2F2)
                                    }
                                ),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Grade: ${res.status.replace("_", " ")}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = when (res.status) {
                                            "CORRECT" -> Color(0xFF065F46)
                                            "PARTIALLY_CORRECT" -> Color(0xFF92400E)
                                            else -> Color(0xFF991B1B)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = res.feedback, style = MaterialTheme.typography.bodySmall)

                                    if (res.missingElements.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Missing elements: ${res.missingElements.joinToString(", ")}", style = MaterialTheme.typography.labelSmall)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Ideal Model Answer:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(res.suggestedAnswer, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
