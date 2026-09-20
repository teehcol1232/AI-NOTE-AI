package com.example.ui.screens.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.local.entity.QuizAttemptEntity
import com.example.ui.components.AppTopBar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatCard
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun QuizHubScreen(
    viewModel: MainViewModel,
    onPracticeMistakes: () -> Unit
) {
    val quizAttempts by viewModel.quizAttempts.collectAsState()
    val unresolvedMistakes by viewModel.unresolvedMistakes.collectAsState()
    val totalQuizzes by viewModel.totalQuizzesCount.collectAsState()
    val avgScore by viewModel.averageQuizScore.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: History, 1: Mistakes Bank

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Quiz Hub",
                subtitle = "Track performance & fix mistakes"
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Quizzes Taken",
                    value = "$totalQuizzes",
                    icon = Icons.Default.Quiz,
                    modifier = Modifier.weight(1f)
                )

                val avgDisplay = if (avgScore != null) "${avgScore?.toInt()}%" else "—"
                StatCard(
                    title = "Average Score",
                    value = avgDisplay,
                    icon = Icons.Default.TrendingUp,
                    containerColor = Color(0xFFECFDF5),
                    iconTint = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }

            // Tab Switcher (History vs Mistakes Bank)
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("History (${quizAttempts.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Mistakes Bank (${unresolvedMistakes.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            if (selectedTab == 0) {
                // Quiz Attempts History
                if (quizAttempts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateView(
                            icon = Icons.Outlined.Assignment,
                            title = "No quiz attempts yet",
                            subtitle = "Complete an MCQ quiz or practice exam to see your history."
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(quizAttempts, key = { it.id }) { attempt ->
                            QuizAttemptCard(attempt = attempt)
                        }
                    }
                }
            } else {
                // Mistakes Bank
                if (unresolvedMistakes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateView(
                            icon = Icons.Outlined.CheckCircle,
                            title = "Mistakes Bank is clear! 🎉",
                            subtitle = "You have no unresolved mistakes. Keep up the high accuracy!"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Targeted Mistakes Review",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Turn your weaknesses into strengths",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }

                                    Button(
                                        onClick = onPracticeMistakes,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Practice", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        items(unresolvedMistakes, key = { it.id }) { mistake ->
                            MistakeCard(
                                mistake = mistake,
                                onResolve = { viewModel.markMistakeResolved(mistake.id) },
                                onDelete = { viewModel.deleteMistake(mistake.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizAttemptCard(attempt: QuizAttemptEntity) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val accuracy = attempt.accuracyPercent

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (accuracy >= 70) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$accuracy%",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (accuracy >= 70) Color(0xFF047857) else Color(0xFFB45309)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attempt.documentTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${attempt.subject} • ${attempt.score}/${attempt.totalQuestions} correct",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormat.format(Date(attempt.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun MistakeCard(
    mistake: MistakeEntity,
    onResolve: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEF2F2)
                ) {
                    Text(
                        text = mistake.topic,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFB91C1C)
                    )
                }

                Row {
                    IconButton(onClick = onResolve, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Resolve", tint = Color(0xFF10B981))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = mistake.questionText,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEF2F2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Your Answer: ${mistake.userAnswer}",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF991B1B)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFECFDF5),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Correct Answer: ${mistake.correctAnswer}",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF065F46)
                )
            }

            if (mistake.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mistake.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
