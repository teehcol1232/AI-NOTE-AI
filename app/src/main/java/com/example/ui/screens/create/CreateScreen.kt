package com.example.ui.screens.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppTopBar
import com.example.ui.viewmodel.MainViewModel

@Composable
fun CreateScreen(
    viewModel: MainViewModel,
    initialMode: String = "TEXT",
    onBack: () -> Unit
) {
    var selectedInputMode by remember { mutableStateOf(initialMode) } // "TEXT", "IMAGE", "CAMERA", "PDF"
    var enteredText by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isHandwritten by remember { mutableStateOf(true) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Zero-permission modern Android Photo Picker
    val pickMultipleMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = uris
        }
    }

    // Document/PDF Picker
    val pickDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedImageUris = listOf(uri)
            // Extract text from URI stream if text/plain or load content
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().readText()
                    if (text.isNotBlank()) {
                        enteredText = text
                        selectedInputMode = "TEXT"
                    }
                }
            } catch (e: Exception) {
                // If binary PDF, keep uri for AI vision / document processing
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Create Study Material",
                subtitle = "Upload or paste your notes",
                onBack = onBack
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (selectedInputMode == "TEXT") {
                                if (enteredText.isNotBlank()) {
                                    viewModel.startCreateWithText(enteredText)
                                }
                            } else {
                                if (selectedImageUris.isNotEmpty()) {
                                    viewModel.startCreateWithImages(
                                        selectedImageUris,
                                        isCamera = (selectedInputMode == "CAMERA"),
                                        isHandwritten = isHandwritten
                                    )
                                } else {
                                    // If no image chosen yet, trigger picker
                                    if (selectedInputMode == "PDF") {
                                        pickDocument.launch(arrayOf("application/pdf", "text/plain"))
                                    } else {
                                        pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = (selectedInputMode == "TEXT" && enteredText.isNotBlank()) ||
                                (selectedInputMode != "TEXT" && selectedImageUris.isNotEmpty()) ||
                                (selectedImageUris.isEmpty())
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedInputMode != "TEXT" && selectedImageUris.isEmpty()) "Select Files" else "Extract & Review Notes",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Choose Input Method",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Method Selector Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModeSelectorCard(
                    title = "Camera",
                    icon = Icons.Default.CameraAlt,
                    selected = selectedInputMode == "CAMERA",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedInputMode = "CAMERA"
                        pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
                ModeSelectorCard(
                    title = "Images",
                    icon = Icons.Default.Image,
                    selected = selectedInputMode == "IMAGE",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedInputMode = "IMAGE"
                        pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
                ModeSelectorCard(
                    title = "PDF",
                    icon = Icons.Default.PictureAsPdf,
                    selected = selectedInputMode == "PDF",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedInputMode = "PDF"
                        pickDocument.launch(arrayOf("application/pdf", "text/plain"))
                    }
                )
                ModeSelectorCard(
                    title = "Text",
                    icon = Icons.Default.EditNote,
                    selected = selectedInputMode == "TEXT",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedInputMode = "TEXT" }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Content Area depending on mode
            if (selectedInputMode == "TEXT") {
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
                                text = "Enter or Paste Notes",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            TextButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        enteredText = clip
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paste")
                            }
                        }

                        OutlinedTextField(
                            value = enteredText,
                            onValueChange = { enteredText = it },
                            placeholder = {
                                Text("Type or paste lecture notes, textbook chapters, formulas, or summaries here...")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 320.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${enteredText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size} words",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (enteredText.isBlank()) {
                                TextButton(
                                    onClick = {
                                        enteredText = """
                                            Physics Chapter 4: Newton's Laws of Motion
                                            1. First Law (Inertia): An object remains at rest or in uniform motion unless acted upon by a net external force.
                                            2. Second Law: F = m * a (Force = Mass * Acceleration). SI Unit: Newton (N).
                                            3. Third Law: For every action, there is an equal and opposite reaction (F_A = -F_B).
                                            4. Momentum: p = m * v. Conservation of momentum applies in closed systems without net external force.
                                            5. Friction: Static friction opposes incipient motion; kinetic friction opposes ongoing relative motion.
                                        """.trimIndent()
                                    }
                                ) {
                                    Text("Load Sample Note", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            } else {
                // File / Image selection area
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (selectedInputMode) {
                                    "CAMERA" -> Icons.Default.CameraAlt
                                    "PDF" -> Icons.Default.PictureAsPdf
                                    else -> Icons.Default.Image
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (selectedImageUris.isEmpty()) "No files selected yet" else "${selectedImageUris.size} page(s) ready for extraction",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when (selectedInputMode) {
                                "CAMERA" -> "Capture high-contrast photos of notebook pages or whiteboards."
                                "PDF" -> "Upload PDF chapters, handouts, or digital documents."
                                else -> "Select up to 15 pages of handwritten or typed notes."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                if (selectedInputMode == "PDF") {
                                    pickDocument.launch(arrayOf("application/pdf", "text/plain"))
                                } else {
                                    pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.UploadFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (selectedImageUris.isEmpty()) "Choose Files" else "Add / Change Files")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Handwriting Option
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                                text = "Handwritten Note Mode",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Optimizes AI vision OCR to detect cursive, pencil, and complex diagrams",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isHandwritten,
                            onCheckedChange = { isHandwritten = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModeSelectorCard(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            CardDefaults.outlinedCardBorder()
        }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
