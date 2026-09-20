package com.example.ui.screens.study

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.*
import com.example.ui.components.AppTopBar
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen
import org.json.JSONArray

@Composable
fun DocumentDetailScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onStartFlashcards: () -> Unit,
    onStartQuiz: () -> Unit,
    onStartPracticeTest: () -> Unit
) {
    val document by viewModel.activeDocument.collectAsState()
    val material by viewModel.activeMaterial.collectAsState()
    val flashcards by viewModel.activeFlashcards.collectAsState()
    val questions by viewModel.activeQuestions.collectAsState()
    val formulas by viewModel.activeFormulas.collectAsState()
    val definitions by viewModel.activeDefinitions.collectAsState()
    val activeExplanation by viewModel.activeExplanation.collectAsState()
    val isExplaining by viewModel.isExplaining.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Study Hub, 1: Summary, 2: Key Points, 3: Formulas, 4: Definitions
    var summaryMode by remember { mutableIntStateOf(1) } // 0: Short, 1: Medium, 2: Detailed
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showExplainDialog by remember { mutableStateOf(false) }
    var conceptToExplain by remember { mutableStateOf("") }

    val context = LocalContext.current
    val tabs = listOf("Study Modes", "Summary", "Key Points", "Formulas", "Definitions")

    if (document == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val doc = document!!

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Study Set") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameDocument(doc.id, renameText)
                        }
                        showRenameDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Explain Concept Dialog
    if (showExplainDialog) {
        AlertDialog(
            onDismissRequest = {
                showExplainDialog = false
                viewModel.clearActiveExplanation()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Concept Tutor", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (activeExplanation == null && !isExplaining) {
                        Text(
                            text = "Enter any term, theorem, or idea from this study set to get simple, detailed, and analogy-based explanations:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = conceptToExplain,
                            onValueChange = { conceptToExplain = it },
                            placeholder = { Text("e.g. Ohm's Law, Conservation of Energy...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else if (isExplaining) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Synthesizing multi-level explanation...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else if (activeExplanation != null) {
                        val exp = activeExplanation!!
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = exp.term,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("🧒 In Simple Terms (Like I'm 10):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Text(exp.simpleExplanation, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("🎓 Academic Detail:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Text(exp.detailedExplanation, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("💡 Tangible Example:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Text(exp.exampleBasedExplanation, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                if (activeExplanation == null && !isExplaining) {
                    Button(
                        onClick = {
                            if (conceptToExplain.isNotBlank()) {
                                viewModel.explainConcept(conceptToExplain)
                            }
                        },
                        enabled = conceptToExplain.isNotBlank()
                    ) { Text("Explain") }
                } else {
                    Button(onClick = {
                        showExplainDialog = false
                        viewModel.clearActiveExplanation()
                    }) { Text("Done") }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExplainDialog = false
                    viewModel.clearActiveExplanation()
                }) { Text("Close") }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = doc.title,
                subtitle = doc.subject,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite(doc.id) }) {
                        Icon(
                            imageVector = if (doc.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (doc.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Share Study Sheet
                    IconButton(
                        onClick = {
                            val shareBody = buildString {
                                appendLine("📚 AI Study Notes: ${doc.title}")
                                appendLine("Subject: ${doc.subject}")
                                appendLine()
                                appendLine("SUMMARY:")
                                appendLine(material?.summaryMedium ?: "")
                                appendLine()
                                appendLine("FLASHCARDS:")
                                flashcards.forEachIndexed { i, f ->
                                    appendLine("${i + 1}. Q: ${f.front} | A: ${f.back}")
                                }
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareBody)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Study Notes"))
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                    }

                    // More options (Rename, Delete)
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    renameText = doc.title
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Explain a Concept", color = MaterialTheme.colorScheme.primary) },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showMenu = false
                                    conceptToExplain = ""
                                    showExplainDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    viewModel.deleteDocument(doc.id)
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    conceptToExplain = ""
                    showExplainDialog = true
                },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                text = { Text("Explain This") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Scrollable Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> StudyHubContent(
                    flashcardCount = flashcards.size,
                    questionCount = questions.size,
                    onStartFlashcards = onStartFlashcards,
                    onStartQuiz = onStartQuiz,
                    onStartPracticeTest = onStartPracticeTest
                )
                1 -> SummaryContent(
                    material = material,
                    summaryMode = summaryMode,
                    onSummaryModeChange = { summaryMode = it }
                )
                2 -> KeyPointsContent(material = material)
                3 -> FormulasContent(
                    formulas = formulas,
                    onToggleBookmark = { viewModel.toggleFormulaBookmark(it) }
                )
                4 -> DefinitionsContent(
                    definitions = definitions,
                    onToggleBookmark = { viewModel.toggleDefinitionBookmark(it) }
                )
            }
        }
    }
}

@Composable
fun StudyHubContent(
    flashcardCount: Int,
    questionCount: Int,
    onStartFlashcards: () -> Unit,
    onStartQuiz: () -> Unit,
    onStartPracticeTest: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Interactive Practice Modes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Master your material with active recall and self-testing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            StudyModeCard(
                title = "Flashcard Deck",
                subtitle = "$flashcardCount active recall cards with flip animation & spaced repetition",
                badge = "$flashcardCount Cards",
                icon = Icons.Default.Style,
                color = Color(0xFF6366F1),
                buttonText = "Study Flashcards",
                onClick = onStartFlashcards
            )
        }

        item {
            StudyModeCard(
                title = "MCQ Quiz",
                subtitle = "Practice test with instant correctness feedback & in-depth explanations",
                badge = "$questionCount Questions",
                icon = Icons.Default.Quiz,
                color = Color(0xFF0EA5E9),
                buttonText = "Start Quiz",
                onClick = onStartQuiz
            )
        }

        item {
            StudyModeCard(
                title = "Practice Exam",
                subtitle = "Timed test with mixed multiple-choice and short-answer questions with AI grading",
                badge = "Timed Mode",
                icon = Icons.Default.Timer,
                color = Color(0xFF10B981),
                buttonText = "Launch Exam",
                onClick = onStartPracticeTest
            )
        }
    }
}

@Composable
fun StudyModeCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    color: Color,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun SummaryContent(
    material: GeneratedMaterialEntity?,
    summaryMode: Int,
    onSummaryModeChange: (Int) -> Unit
) {
    if (material == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Summary loading or not generated.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp)
    ) {
        item {
            // Summary Mode Switcher Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Punchy Short", "Balanced Medium", "Deep Detailed").forEachIndexed { index, label ->
                    val isSelected = summaryMode == index
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSummaryModeChange(index) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
                    ) {
                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val currentText = when (summaryMode) {
                        0 -> material.summaryShort.ifBlank { material.summaryMedium }
                        2 -> material.summaryDetailed.ifBlank { material.summaryMedium }
                        else -> material.summaryMedium
                    }

                    Text(
                        text = currentText,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (material.explanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Analogy & Intuition",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = material.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeyPointsContent(material: GeneratedMaterialEntity?) {
    val keyPoints = remember(material) {
        if (material?.keyPointsJson.isNullOrBlank()) emptyList<String>()
        else {
            try {
                val arr = JSONArray(material?.keyPointsJson)
                List(arr.length()) { arr.getString(it) }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(keyPoints) { kp ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = kp,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun FormulasContent(
    formulas: List<FormulaEntity>,
    onToggleBookmark: (Long) -> Unit
) {
    if (formulas.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No formulas detected in this study material.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(formulas) { f ->
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
                        Text(
                            text = f.meaning,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        IconButton(onClick = { onToggleBookmark(f.id) }) {
                            Icon(
                                imageVector = if (f.isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (f.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = f.formula,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 22.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DefinitionsContent(
    definitions: List<DefinitionEntity>,
    onToggleBookmark: (Long) -> Unit
) {
    if (definitions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No definitions found in this study material.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(definitions) { d ->
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
                        Text(
                            text = d.term,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { onToggleBookmark(d.id) }) {
                            Icon(
                                imageVector = if (d.isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (d.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = d.definition,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
