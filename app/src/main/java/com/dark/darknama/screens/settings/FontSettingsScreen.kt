package com.dark.darknama.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dark.darknama.R
import com.dark.darknama.components.SettingsOptionRow
import com.dark.darknama.components.SettingsSectionTitle
import com.dark.darknama.components.SettingsSubPage
import com.dark.darknama.data.model.AppFont
import com.dark.darknama.data.model.EnglishFontType
import com.dark.darknama.data.model.FontSettings
import com.dark.darknama.data.model.FontType
import com.dark.darknama.ui.theme.FontManager
import com.dark.darknama.utils.LanguageUtils
import com.dark.darknama.utils.StorageUtils

/** Two independent saved choices, using the existing font manager and typography. */
@Composable
fun FontSettingsScreen(
    navController: NavController?,
    onFontSettingsChanged: (FontSettings) -> Unit = {}
) {
    val context = LocalContext.current
    var fontSettings by remember { mutableStateOf(StorageUtils.loadFontSettings(context)) }
    val isPersian = fontSettings.forLanguage(LanguageUtils.getSavedLanguage(context)) is AppFont.Persian

    fun update(newSettings: FontSettings) {
        fontSettings = newSettings
        StorageUtils.saveFontSettings(context, newSettings)
        onFontSettingsChanged(newSettings)
    }

    SettingsSubPage(
        title = stringResource(R.string.font_settings),
        navController = navController,
        icon = Icons.Default.TextFields
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.font_active_language,
                    stringResource(if (isPersian) R.string.font_language_persian
                        else R.string.font_language_english)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            SettingsSectionTitle(stringResource(R.string.font_persian_section) +
                if (isPersian) " · ${stringResource(R.string.font_active_badge)}" else "")
            val persianSample = stringResource(R.string.font_preview_fa)
            FontType.entries.forEach { type ->
                val name = stringResource(when (type) {
                    FontType.DEFAULT -> R.string.font_system_default
                    FontType.VAZIRMATN -> R.string.font_vazirmatn
                })
                SettingsOptionRow(
                    label = name,
                    description = if (type == FontType.VAZIRMATN)
                        stringResource(R.string.font_vazirmatn_desc) else null,
                    isSelected = fontSettings.fontType == type,
                    onSelect = { update(fontSettings.copy(fontType = type)) },
                    previewText = persianSample,
                    previewFontFamily = FontManager.loadFontFamily(context, type),
                    focusKey = "font-fa-${type.name}"
                )
            }

            Spacer(Modifier.height(24.dp))
            SettingsSectionTitle(stringResource(R.string.font_english_section) +
                if (!isPersian) " · ${stringResource(R.string.font_active_badge)}" else "")
            val englishSample = stringResource(R.string.font_preview_en)
            // There are no English TTFs in a normal checkout. Hide unavailable
            // families rather than implying they can be rendered as previews.
            EnglishFontType.entries.filter { FontManager.isAvailable(context, it) }.forEach { type ->
                val name = stringResource(when (type) {
                    EnglishFontType.DEFAULT -> R.string.font_system_default
                    EnglishFontType.INTER -> R.string.font_inter
                    EnglishFontType.POPPINS -> R.string.font_poppins
                    EnglishFontType.MONTSERRAT -> R.string.font_montserrat
                    EnglishFontType.NUNITO -> R.string.font_nunito
                    EnglishFontType.LATO -> R.string.font_lato
                })
                SettingsOptionRow(
                    label = name,
                    isSelected = fontSettings.englishFontType == type,
                    onSelect = { update(fontSettings.copy(englishFontType = type)) },
                    previewText = englishSample,
                    previewFontFamily = FontManager.loadFontFamily(context, type),
                    focusKey = "font-en-${type.name}"
                )
            }
            if (!FontManager.isAvailable(context, fontSettings.englishFontType)) {
                Text(stringResource(R.string.font_english_missing_selection),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(R.string.font_optional_english),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp))

            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionTitle(stringResource(R.string.font_preview))
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)
            ) {
                Text(persianSample, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontManager.loadFontFamily(context, fontSettings.fontType)
                        ?: FontFamily.Default)
                Spacer(modifier = Modifier.height(8.dp))
                Text(englishSample, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontManager.loadFontFamily(context, fontSettings.englishFontType)
                        ?: FontFamily.Default)
                Spacer(modifier = Modifier.height(8.dp))
                Text("0 1 2 3 4 5 6 7 8 9  ۰ ۱ ۲ ۳ ۴ ۵ ۶ ۷ ۸ ۹",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontManager.loadFontFamily(context, fontSettings,
                        if (isPersian) LanguageUtils.LANGUAGE_FARSI else LanguageUtils.LANGUAGE_ENGLISH)
                        ?: FontFamily.Default)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
