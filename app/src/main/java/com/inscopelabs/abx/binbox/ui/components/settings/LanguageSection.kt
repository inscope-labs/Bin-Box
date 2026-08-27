package com.inscopelabs.abx.binbox.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.ui.i18n.AppLanguage
import com.inscopelabs.abx.binbox.ui.i18n.AppStrings
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel

/**
 * Language & localization picker. Module component: renders the language
 * grid and reports selection to [viewModel]; holds no state of its own.
 */
@Composable
fun LanguageSection(
    strings: AppStrings,
    appLanguage: AppLanguage,
    viewModel: BinBoxViewModel
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ImmersivePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Language, null, tint = ImmersivePrimary, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(
                        text = strings.languageSectionTitle,
                        color = ImmersiveTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.languageSectionSubtitle,
                        color = ImmersiveTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Grid of Language Selection Cards
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.chunked(2).forEach { rowLanguages ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowLanguages.forEach { lang ->
                            val isSelected = lang == appLanguage
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) ImmersivePrimary.copy(alpha = 0.15f) else ImmersiveComponent,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) ImmersivePrimary else ImmersiveBorderVerySubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setLanguage(lang) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = lang.flagEmoji, fontSize = 18.sp)
                                        Column {
                                            Text(
                                                text = lang.nativeName,
                                                color = if (isSelected) ImmersivePrimary else ImmersiveTextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                            if (lang.displayName != lang.nativeName && lang != AppLanguage.SYSTEM) {
                                                Text(
                                                    text = lang.displayName,
                                                    color = ImmersiveTextMuted,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = ImmersivePrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (rowLanguages.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
