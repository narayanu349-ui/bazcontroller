package com.baz.controller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baz.controller.BazApplication
import com.baz.controller.model.TemplateLayout
import com.baz.controller.ui.theme.AccentAmber
import com.baz.controller.ui.theme.AccentCyan
import com.baz.controller.ui.theme.DarkBg
import com.baz.controller.ui.theme.DarkBorder
import com.baz.controller.ui.theme.DarkSurface
import com.baz.controller.ui.theme.TextPrimary
import com.baz.controller.ui.theme.TextSecondary

@Composable
fun TemplateScreen(
    onBack: () -> Unit,
    onSelectTemplate: (TemplateLayout) -> Unit
) {
    val repo = BazApplication.instance.templateRepository
    val templates by repo.templates.collectAsState()
    val activeTemplate by repo.activeTemplate.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    Text("← BACK", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "CONTROLLER TEMPLATES LIBRARY",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Grid of templates
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(templates) { template ->
                val isActive = template.id == activeTemplate.id

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) Color(0xFF1B2538) else DarkSurface)
                        .border(
                            width = if (isActive) 2.dp else 1.dp,
                            color = if (isActive) AccentAmber else DarkBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            repo.setActiveTemplate(template)
                            onSelectTemplate(template)
                        }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = template.name,
                                color = if (isActive) AccentAmber else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            if (template.isDefault) {
                                Text(
                                    text = "PRESET",
                                    color = AccentCyan,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = template.description,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 3,
                            lineHeight = 15.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${template.elements.size} CONTROLS",
                            color = Color(0xFF6B7280),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        if (isActive) {
                            Text(
                                text = "ACTIVE ✓",
                                color = AccentAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Text(
                                text = "SELECT",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
