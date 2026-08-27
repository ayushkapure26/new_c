package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.EmeraldGreen
import com.example.util.AppLocalization
import com.example.util.LocaleManager

/**
 * A dedicated settings UI component that exposes [LocaleManager],
 * allowing users to toggle between available languages in the app.
 *
 * It provides:
 * 1. Quick-toggle chips for immediate language selection.
 * 2. Visual active indicator and locale badge.
 * 3. Full-featured language selection dialog for all supported Indian & global languages.
 * 4. Reactive synchronization with DataStore via [LocaleManager].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocaleSettingsComponent(
    modifier: Modifier = Modifier,
    localeManager: LocaleManager = runCatching { LocaleManager.LocalLocaleManager.current }
        .getOrElse { LocaleManager.getInstance(LocalContext.current) },
    onLanguageChanged: ((String) -> Unit)? = null
) {
    val currentLanguage by localeManager.selectedLanguageFlow.collectAsState(
        initial = localeManager.selectedLanguageState.value
    )
    val localizedStrings = remember(currentLanguage) {
        AppLocalization.getStrings(currentLanguage)
    }
    var showFullLanguageDialog by rememberSaveable { mutableStateOf(false) }

    // Primary quick-selection languages
    val quickLanguages = listOf(
        "English" to "EN",
        "Hindi (हिंदी)" to "हिं",
        "Marathi (मराठी)" to "म",
        "Gujarati (ગુજરાતી)" to "ગુ",
        "Punjabi (ਪੰਜਾਬੀ)" to "ਪੰ",
        "Tamil (தமிழ்)" to "த",
        "Telugu (తెలుగు)" to "తె",
        "Bengali (বাংলা)" to "বা",
        "Kannada (ಕನ್ನಡ)" to "ಕ",
        "Spanish (Español)" to "ES"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("locale_settings_component"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkTeal.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language & Locale",
                            tint = DarkTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = localizedStrings.selectLanguage,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "LocaleManager DataStore Sync",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Active Language Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkTeal.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, DarkTeal.copy(alpha = 0.3f)),
                    modifier = Modifier.testTag("active_language_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreen)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = currentLanguage.substringBefore(" ("),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkTeal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = localizedStrings.languageModuleDesc,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Toggle Chips
            Text(
                text = "Quick Toggle",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickLanguages.forEach { (langName, shortCode) ->
                    val isSelected = currentLanguage == langName ||
                            (langName.startsWith("English") && currentLanguage == "English")

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            localeManager.setLanguageAsync(langName)
                            onLanguageChanged?.invoke(langName)
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = langName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DarkTeal,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("lang_chip_${shortCode.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Full Dialog Trigger Button
            OutlinedButton(
                onClick = { showFullLanguageDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_all_languages_dialog_btn"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DarkTeal.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = DarkTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View All Languages & Regional Details",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkTeal
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = DarkTeal,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    // Modal popup showing all options
    if (showFullLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { selectedLang ->
                localeManager.setLanguageAsync(selectedLang)
                onLanguageChanged?.invoke(selectedLang)
                showFullLanguageDialog = false
            },
            onDismiss = { showFullLanguageDialog = false }
        )
    }
}
