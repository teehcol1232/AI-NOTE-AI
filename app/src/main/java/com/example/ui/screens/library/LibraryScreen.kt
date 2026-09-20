package com.example.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.ui.components.AppTopBar
import com.example.ui.components.EmptyStateView
import com.example.ui.screens.home.DocumentCard
import com.example.ui.viewmodel.MainViewModel

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onOpenDocument: (Long) -> Unit,
    onCreateNew: () -> Unit
) {
    val documents by viewModel.filteredDocuments.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedSubject by viewModel.selectedSubjectFilter.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var showFavoritesOnly by remember { mutableStateOf(false) }

    val displayedDocs = remember(documents, showFavoritesOnly) {
        if (showFavoritesOnly) documents.filter { it.isFavorite } else documents
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Study Library",
                subtitle = "${displayedDocs.size} study sets"
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search topics, subjects, notes...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            // Subject Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedSubject == null && !showFavoritesOnly,
                        onClick = {
                            viewModel.setSubjectFilter(null)
                            showFavoritesOnly = false
                        },
                        label = { Text("All") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    FilterChip(
                        selected = showFavoritesOnly,
                        onClick = { showFavoritesOnly = !showFavoritesOnly },
                        label = { Text("★ Starred") },
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        }
                    )
                }

                items(subjects) { subject ->
                    FilterChip(
                        selected = selectedSubject == subject.name,
                        onClick = {
                            if (selectedSubject == subject.name) {
                                viewModel.setSubjectFilter(null)
                            } else {
                                viewModel.setSubjectFilter(subject.name)
                            }
                        },
                        label = { Text(subject.name) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Document List
            if (displayedDocs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Outlined.FolderOpen,
                        title = "No study sets found",
                        subtitle = if (searchQuery.isNotBlank()) "No notes match '$searchQuery'." else "Upload notes or create a new set.",
                        actionText = "+ Create Study Material",
                        onAction = onCreateNew
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(displayedDocs, key = { it.id }) { doc ->
                        DocumentCard(
                            doc = doc,
                            onOpen = { onOpenDocument(doc.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(doc.id) }
                        )
                    }
                }
            }
        }
    }
}
