package com.baz.controller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baz.controller.BazApplication
import com.baz.controller.model.ControlType
import com.baz.controller.model.LayoutElement
import com.baz.controller.model.TemplateLayout
import com.baz.controller.ui.theme.AccentAmber
import com.baz.controller.ui.theme.AccentCyan
import com.baz.controller.ui.theme.DarkBg
import com.baz.controller.ui.theme.DarkBorder
import com.baz.controller.ui.theme.DarkSurface
import com.baz.controller.ui.theme.TextPrimary
import java.util.UUID

@Composable
fun LayoutEditorScreen(onBack: () -> Unit) {
    val app = BazApplication.instance
    val repo = app.templateRepository
    val activeTemplate by repo.activeTemplate.collectAsState()

    var elements by remember { mutableStateOf(activeTemplate.elements.map { it.copy() }.toMutableList()) }
    var selectedElementId by remember { mutableStateOf<String?>(null) }
    var templateTitle by remember { mutableStateOf(activeTemplate.name) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var jsonExportString by remember { mutableStateOf("") }
    var jsonImportInput by remember { mutableStateOf("") }

    val selectedElement = elements.find { it.id == selectedElementId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().height(42.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurface)
                        .clickable(onClick = onBack)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("← EXIT", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "LAYOUT STUDIO",
                    color = AccentCyan,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Action Buttons (Add Control, Save, Export, Import)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Add new widget button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurface)
                        .clickable {
                            val newElem = LayoutElement(
                                type = ControlType.CUSTOM_BUTTON_GROUP,
                                x = 0.5f,
                                y = 0.5f,
                                width = 0.15f,
                                height = 0.15f
                            )
                            val updated = elements.toMutableList()
                            updated.add(newElem)
                            elements = updated
                            selectedElementId = newElem.id
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text("+ ADD CONTROL", color = AccentAmber, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                // Export Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurface)
                        .clickable {
                            val t = activeTemplate.copy(elements = elements, name = templateTitle)
                            jsonExportString = repo.exportTemplateJson(t)
                            showExportDialog = true
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text("EXPORT JSON", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                // Import Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurface)
                        .clickable {
                            showImportDialog = true
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text("IMPORT JSON", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                // Save Template
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentAmber)
                        .clickable {
                            val newTemplate = TemplateLayout(
                                id = UUID.randomUUID().toString(),
                                name = if (templateTitle.endsWith("(Custom)")) templateTitle else "$templateTitle (Custom)",
                                description = "Customized user layout with ${elements.size} controls",
                                isDefault = false,
                                elements = elements
                            )
                            repo.saveTemplate(newTemplate)
                            onBack()
                        }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text("SAVE TEMPLATE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Center Work Area (Interactive Canvas + Inspector Drawer)
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Interactive 2D Layout Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F1420))
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            ) {
                val canvasWidth = maxWidth
                val canvasHeight = maxHeight

                // Render each layout element
                elements.forEach { elem ->
                    val isSelected = elem.id == selectedElementId

                    Box(
                        modifier = Modifier
                            .offset(
                                x = canvasWidth * elem.x,
                                y = canvasHeight * elem.y
                            )
                            .size(
                                width = canvasWidth * elem.width,
                                height = canvasHeight * elem.height
                            )
                            .rotate(elem.rotationDeg)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0xFF23304A) else DarkSurface)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) AccentAmber else DarkBorder,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedElementId = elem.id }
                            .pointerInput(elem.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    elem.x = (elem.x + (dragAmount.x / size.width.toFloat())).coerceIn(0f, 0.9f)
                                    elem.y = (elem.y + (dragAmount.y / size.height.toFloat())).coerceIn(0f, 0.9f)
                                    // Trigger recomposition
                                    elements = elements.map { it.copy() }.toMutableList()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = elem.type.name.replace("_", " "),
                            color = if (isSelected) AccentAmber else Color(0xFF9CA3AF),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right Inspector Panel: Resize, Rotate, Delete selected element
            Column(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "CONTROL INSPECTOR",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                if (selectedElement != null) {
                    Text(
                        text = "Type: ${selectedElement.type.name}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Width Slider
                    Text("Width: ${(selectedElement.width * 100).toInt()}%", color = Color(0xFF9CA3AF), fontSize = 10.sp)
                    Slider(
                        value = selectedElement.width,
                        onValueChange = {
                            selectedElement.width = it
                            elements = elements.map { elem -> elem.copy() }.toMutableList()
                        },
                        valueRange = 0.05f..0.60f
                    )

                    // Height Slider
                    Text("Height: ${(selectedElement.height * 100).toInt()}%", color = Color(0xFF9CA3AF), fontSize = 10.sp)
                    Slider(
                        value = selectedElement.height,
                        onValueChange = {
                            selectedElement.height = it
                            elements = elements.map { elem -> elem.copy() }.toMutableList()
                        },
                        valueRange = 0.05f..0.85f
                    )

                    // Rotation Slider
                    Text("Rotation: ${selectedElement.rotationDeg.toInt()}°", color = Color(0xFF9CA3AF), fontSize = 10.sp)
                    Slider(
                        value = selectedElement.rotationDeg,
                        onValueChange = {
                            selectedElement.rotationDeg = it
                            elements = elements.map { elem -> elem.copy() }.toMutableList()
                        },
                        valueRange = 0f..360f
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Delete Element
                    Button(
                        onClick = {
                            elements = elements.filter { it.id != selectedElement.id }.toMutableList()
                            selectedElementId = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("REMOVE CONTROL", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Select any control on the canvas to drag, resize, rotate, or delete.",
                            color = Color(0xFF6B7280),
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Template JSON") },
            text = {
                OutlinedTextField(
                    value = jsonExportString,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            },
            confirmButton = {
                Button(onClick = { showExportDialog = false }) { Text("DONE") }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Template JSON") },
            text = {
                OutlinedTextField(
                    value = jsonImportInput,
                    onValueChange = { jsonImportInput = it },
                    placeholder = { Text("Paste template JSON here...") },
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    val imported = repo.importTemplateJson(jsonImportInput)
                    if (imported != null) {
                        elements = imported.elements.map { it.copy() }.toMutableList()
                        templateTitle = imported.name
                    }
                    showImportDialog = false
                }) {
                    Text("IMPORT")
                }
            },
            dismissButton = {
                Button(onClick = { showImportDialog = false }) { Text("CANCEL") }
            }
        )
    }
}
