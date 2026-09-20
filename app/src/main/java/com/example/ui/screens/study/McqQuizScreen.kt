package com.example.ui.screens.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.ui.components.AppTopBar
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import org.json.JSONArray

@Composable
fun McqQuizScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val document by viewModel.activeDocument.collectAsState()
    val allQuestions by viewModel.activeQuestions.collectAsState()
    val mcqs = remember(allQuestions) { allQuestions.filter { it.type == "MCQ" } }

    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var isQuizCompleted by remember { mutableStateOf(false) }

    val mistakesList = remember { mutableStateListOf<MistakeEntity>() }
    var elapsedTimeSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isQuizCompleted) {
        while (!isQuizCompleted) {
            delay(1000)
            elapsedTimeSeconds++
        }
    }

    if (mcqs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No multiple choice questions available for this study set.")
        }
        return
    }

    val currentQ = mcqs.getOrNull(currentIndex) ?: mcqs.first()
    val options = remember(currentQ) {
        try {
            val arr = JSONArray(currentQ.optionsJson)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            listOf(currentQ.correctAnswer)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Quiz Practice",
                subtitle = "Question ${currentIndex + 1} of ${mcqs.size}",
                onBack = onBack
            )
        },
        bottomBar = {
            if (!isQuizCompleted && selectedOption != null) {
                Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                if (currentIndex < mcqs.size - 1) {
                                    currentIndex++
                                    selectedOption = null
                                } else {
                                    isQuizCompleted = true
                                    // Submit result
                                    document?.let { doc ->
                                        viewModel.submitQuizResult(
                                            documentId = doc.id,
                                            documentTitle = doc.title,
                                            subject = doc.subject,
                                            score = score,
                                            total = mcqs.size,
                                            timeTakenSeconds = elapsedTimeSeconds,
                                            mistakes = mistakesList.toList(),
                                            weakTopics = mistakesList.map { it.topic }.distinct()
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = if (currentIndex < mcqs.size - 1) "Next Question" else "Finish & View Results",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isQuizCompleted) {
            // Results Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val accuracy = ((score.toFloat() / mcqs.size.toFloat()) * 100).toInt()

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(if (accuracy >= 70) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (accuracy >= 70) Icons.Default.EmojiEvents else Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = if (accuracy >= 70) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (accuracy >= 80) "Outstanding Result!" else if (accuracy >= 60) "Good Effort!" else "Keep Practicing!",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You scored $score out of ${mcqs.size} questions ($accuracy% accuracy)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$score", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF065F46))
                            Text("Correct", style = MaterialTheme.typography.bodySmall, color = Color(0xFF047857))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${mcqs.size - score}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF991B1B))
                            Text("Incorrect", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB91C1C))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            val mins = elapsedTimeSeconds / 60
                            val secs = elapsedTimeSeconds % 60
                            Text(text = "${mins}m ${secs}s", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text("Time Taken", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        currentIndex = 0
                        selectedOption = null
                        score = 0
                        mistakesList.clear()
                        elapsedTimeSeconds = 0
                        isQuizCompleted = false
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Retake Quiz", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Return to Study Set")
                }
            }
            return@Scaffold
        }

        // Active Quiz Question View
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Linear Progress
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / mcqs.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Question Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                text = "Score: $score / ${currentIndex + (if (selectedOption != null) 1 else 0)}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = currentQ.difficulty,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
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

            // Options List
            val optionLabels = listOf("A", "B", "C", "D")
            options.forEachIndexed { index, option ->
                val label = optionLabels.getOrNull(index) ?: "${index + 1}"
                val isSelected = selectedOption == option
                val isAnswerSubmitted = selectedOption != null
                val isCorrect = option == currentQ.correctAnswer

                val backgroundColor = when {
                    !isAnswerSubmitted -> MaterialTheme.colorScheme.surface
                    isCorrect -> Color(0xFFECFDF5)
                    isSelected -> Color(0xFFFEF2F2)
                    else -> MaterialTheme.colorScheme.surface
                }

                val borderColor = when {
                    !isAnswerSubmitted && isSelected -> MaterialTheme.colorScheme.primary
                    isAnswerSubmitted && isCorrect -> Color(0xFF10B981)
                    isAnswerSubmitted && isSelected -> Color(0xFFEF4444)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !isAnswerSubmitted) {
                            selectedOption = option
                            if (option == currentQ.correctAnswer) {
                                score++
                            } else {
                                mistakesList.add(
                                    MistakeEntity(
                                        questionId = currentQ.id,
                                        documentId = currentQ.documentId,
                                        questionText = currentQ.questionText,
                                        userAnswer = option,
                                        correctAnswer = currentQ.correctAnswer,
                                        explanation = currentQ.explanation,
                                        subject = document?.subject ?: "General",
                                        topic = currentQ.topic.ifBlank { "General" }
                                    )
                                )
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = backgroundColor,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isAnswerSubmitted && isCorrect -> Color(0xFF10B981)
                                        isAnswerSubmitted && isSelected -> Color(0xFFEF4444)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isAnswerSubmitted && (isCorrect || isSelected)) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        if (isAnswerSubmitted) {
                            if (isCorrect) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                            } else if (isSelected) {
                                Icon(imageVector = Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }

            // In-depth Explanation Card after answer submission
            AnimatedVisibility(visible = selectedOption != null) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedOption == currentQ.correctAnswer) Color(0xFFECFDF5) else Color(0xFFFFFBEB)
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (selectedOption == currentQ.correctAnswer) Color(0xFF047857) else Color(0xFFB45309),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedOption == currentQ.correctAnswer) "Correct Explanation" else "Why this answer is correct",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedOption == currentQ.correctAnswer) Color(0xFF065F46) else Color(0xFF92400E)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentQ.explanation,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = if (selectedOption == currentQ.correctAnswer) Color(0xFF065F46) else Color(0xFF92400E)
                            )
                        }
                    }
                }
            }
        }
    }
}
