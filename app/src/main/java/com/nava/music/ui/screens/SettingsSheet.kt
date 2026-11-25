package com.nava.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nava.music.data.AiConfig
import com.nava.music.data.AiProvider
import com.nava.music.data.AppThemeMode
import com.nava.music.data.VisualizerStyle
import com.nava.music.ui.components.MeshBackground
import com.nava.music.ui.theme.LocalNavaPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    initialConfig: AiConfig,
    themeMode: AppThemeMode,
    visualizerStyle: VisualizerStyle,
    hasRecordPermission: Boolean,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectVisualizerStyle: (VisualizerStyle) -> Unit,
    onRequestRecordPermission: () -> Unit,
    onSaveConfig: (AiConfig) -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalNavaPalette.current

    var provider by remember { mutableStateOf(initialConfig.provider) }
    var apiKey by remember { mutableStateOf(initialConfig.apiKey) }
    var baseUrl by remember { mutableStateOf(initialConfig.baseUrl) }
    var model by remember { mutableStateOf(initialConfig.model) }
    var targetLanguage by remember { mutableStateOf(initialConfig.targetLanguage) }
    var showApiKey by remember { mutableStateOf(false) }

    val languagePresets = listOf("Persian (فارسی)", "English", "Arabic (العربية)", "Turkish (Türkçe)")

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = palette.primary,
        unfocusedBorderColor = palette.outline,
        focusedTextColor = palette.onBackground,
        unfocusedTextColor = palette.onBackground,
        focusedLabelColor = palette.primary,
        unfocusedLabelColor = palette.onBackgroundMuted,
        cursorColor = palette.primary
    )

    Box(modifier = Modifier.fillMaxSize()) {
        MeshBackground(palette = palette, animate = false, intensity = 0.45f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = palette.primary
                    )
                    Text(
                        text = "تنظیمات ظاهر و هوش مصنوعی",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.onBackground
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(palette.surface)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = palette.onBackground,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 0: Theme Mode (System Default / Light / Dark)
            Text(
                text = "تم برنامه",
                style = MaterialTheme.typography.titleMedium,
                color = palette.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.outline, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppThemeMode.entries.forEach { mode ->
                    val selected = themeMode == mode
                    val icon = when (mode) {
                        AppThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                        AppThemeMode.LIGHT -> Icons.Rounded.LightMode
                        AppThemeMode.DARK -> Icons.Rounded.DarkMode
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) palette.onBackground else palette.surface)
                            .clickable { onSelectThemeMode(mode) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) palette.background else palette.onBackgroundMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selected) palette.background else palette.onBackgroundMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Section 1: AI Lyrics & Translation Provider
            Text(
                text = "سرویس هوش مصنوعی (لیریک و ترجمه)",
                style = MaterialTheme.typography.titleMedium,
                color = palette.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AiProvider.entries.forEach { option ->
                    val selected = provider == option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) palette.primary else palette.surface)
                            .border(1.dp, palette.outline, RoundedCornerShape(14.dp))
                            .clickable {
                                provider = option
                                baseUrl = option.defaultBaseUrl
                                model = option.defaultModel
                            }
                            .padding(vertical = 12.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) palette.onPrimary else palette.onBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("کلید API (API Key)") },
                placeholder = { Text("AIza... یا sk-...") },
                singleLine = true,
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = "Toggle visibility",
                            tint = palette.onBackgroundMuted
                        )
                    }
                },
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("نام مدل (Model)") },
                placeholder = { Text("gemini-2.5-flash یا gpt-4o-mini") },
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("آدرس سرور (Base URL - برای OpenRouter یا پراکسی)") },
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "زبان مقصد ترجمه",
                style = MaterialTheme.typography.labelMedium,
                color = palette.onBackgroundMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                languagePresets.forEach { preset ->
                    val selected = targetLanguage == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) palette.onBackground else palette.surface)
                            .border(1.dp, palette.outline, RoundedCornerShape(50))
                            .clickable { targetLanguage = preset }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) palette.background else palette.onBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = targetLanguage,
                onValueChange = { targetLanguage = it },
                label = { Text("زبان دلخواه") },
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "حالت انیمیشن بیس در نمای مدرن",
                style = MaterialTheme.typography.titleMedium,
                color = palette.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisualizerStyle.entries.forEach { style ->
                    val selected = visualizerStyle == style
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) palette.onBackground else palette.surface)
                            .border(1.dp, palette.outline, RoundedCornerShape(12.dp))
                            .clickable { onSelectVisualizerStyle(style) }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = style.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) palette.background else palette.onBackground
                        )
                    }
                }
            }

            if (!hasRecordPermission) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequestRecordPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.surfaceElevated,
                        contentColor = palette.onBackground
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("فعال‌سازی آنالیز زنده فرکانس صدا (Visualizer)")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSaveConfig(
                        AiConfig(
                            provider = provider,
                            apiKey = apiKey,
                            baseUrl = baseUrl,
                            model = model,
                            targetLanguage = targetLanguage
                        )
                    )
                    onClose()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("ذخیره تنظیمات", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
