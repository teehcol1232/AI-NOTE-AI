package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ui.screens.create.CreateScreen
import com.example.ui.screens.create.ExtractReviewScreen
import com.example.ui.screens.create.GenerateConfigScreen
import com.example.ui.screens.create.ProcessingScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.quiz.QuizHubScreen
import com.example.ui.screens.study.DocumentDetailScreen
import com.example.ui.screens.study.FlashcardStudyScreen
import com.example.ui.screens.study.McqQuizScreen
import com.example.ui.screens.study.PracticeTestScreen
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

data class NavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    var createMode by remember { mutableStateOf("TEXT") }

    val navItems = listOf(
        NavItem(Screen.HOME, "Home", Icons.Default.Home, Icons.Outlined.Home),
        NavItem(Screen.LIBRARY, "Library", Icons.Default.Folder, Icons.Outlined.Folder),
        NavItem(Screen.CREATE, "Create", Icons.Default.AddCircle, Icons.Outlined.AddCircleOutline),
        NavItem(Screen.QUIZ_HUB, "Quiz Hub", Icons.Default.Quiz, Icons.Outlined.Quiz)
    )

    val isTopLevelScreen = currentScreen in listOf(
        Screen.HOME, Screen.LIBRARY, Screen.CREATE, Screen.QUIZ_HUB, Screen.PROFILE
    )

    // Handle Back Press
    BackHandler(enabled = !isTopLevelScreen || currentScreen != Screen.HOME) {
        when (currentScreen) {
            Screen.DOCUMENT_DETAIL -> viewModel.navigateTo(Screen.LIBRARY)
            Screen.FLASHCARD_STUDY, Screen.MCQ_QUIZ, Screen.PRACTICE_TEST -> viewModel.navigateTo(Screen.DOCUMENT_DETAIL)
            Screen.EXTRACT_REVIEW -> viewModel.navigateTo(Screen.CREATE)
            Screen.GENERATE_CONFIG -> viewModel.navigateTo(Screen.EXTRACT_REVIEW)
            Screen.PROCESSING -> viewModel.navigateTo(Screen.CREATE)
            Screen.MISTAKES_PRACTICE -> viewModel.navigateTo(Screen.QUIZ_HUB)
            Screen.LIBRARY, Screen.CREATE, Screen.QUIZ_HUB, Screen.PROFILE -> viewModel.navigateTo(Screen.HOME)
            Screen.HOME -> { /* Exit app */ }
        }
    }

    Scaffold(
        bottomBar = {
            if (isTopLevelScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentScreen == item.screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (item.screen == Screen.CREATE) {
                                    createMode = "TEXT"
                                }
                                viewModel.navigateTo(item.screen)
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isTopLevelScreen) innerPadding else PaddingValues(0.dp))
        ) {
            when (currentScreen) {
                Screen.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToCreate = { mode ->
                        createMode = mode
                        viewModel.navigateTo(Screen.CREATE)
                    }
                )

                Screen.LIBRARY -> LibraryScreen(
                    viewModel = viewModel,
                    onOpenDocument = { docId -> viewModel.openDocumentDetail(docId) },
                    onCreateNew = {
                        createMode = "TEXT"
                        viewModel.navigateTo(Screen.CREATE)
                    }
                )

                Screen.CREATE -> CreateScreen(
                    viewModel = viewModel,
                    initialMode = createMode,
                    onBack = { viewModel.navigateTo(Screen.HOME) }
                )

                Screen.EXTRACT_REVIEW -> ExtractReviewScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.CREATE) },
                    onContinue = { viewModel.navigateTo(Screen.GENERATE_CONFIG) }
                )

                Screen.GENERATE_CONFIG -> GenerateConfigScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.EXTRACT_REVIEW) },
                    onGenerate = { viewModel.executeGeneration() }
                )

                Screen.PROCESSING -> ProcessingScreen(
                    viewModel = viewModel,
                    onCancel = { viewModel.navigateTo(Screen.CREATE) }
                )

                Screen.DOCUMENT_DETAIL -> DocumentDetailScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.LIBRARY) },
                    onStartFlashcards = { viewModel.navigateTo(Screen.FLASHCARD_STUDY) },
                    onStartQuiz = { viewModel.navigateTo(Screen.MCQ_QUIZ) },
                    onStartPracticeTest = { viewModel.navigateTo(Screen.PRACTICE_TEST) }
                )

                Screen.FLASHCARD_STUDY -> FlashcardStudyScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.DOCUMENT_DETAIL) }
                )

                Screen.MCQ_QUIZ -> McqQuizScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.DOCUMENT_DETAIL) }
                )

                Screen.PRACTICE_TEST -> PracticeTestScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.DOCUMENT_DETAIL) }
                )

                Screen.QUIZ_HUB -> QuizHubScreen(
                    viewModel = viewModel,
                    onPracticeMistakes = {
                        // Open practice for mistakes if questions exist
                        viewModel.navigateTo(Screen.MCQ_QUIZ)
                    }
                )

                Screen.MISTAKES_PRACTICE -> McqQuizScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.QUIZ_HUB) }
                )

                Screen.PROFILE -> ProfileScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.HOME) }
                )
            }
        }
    }
}
