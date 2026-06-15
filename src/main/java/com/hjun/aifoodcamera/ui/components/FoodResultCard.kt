package com.hjun.aifoodcamera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hjun.aifoodcamera.R
import com.hjun.aifoodcamera.data.model.FoodAnalysisResult
import com.hjun.aifoodcamera.ui.theme.AppDimens
import java.io.File

@Composable
fun MacroNutrientBars(
    protein: String,
    fat: String,
    carbs: String,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple("蛋白质", protein, MaterialTheme.colorScheme.primary),
        Triple("脂肪", fat, MaterialTheme.colorScheme.secondary),
        Triple("碳水", carbs, MaterialTheme.colorScheme.tertiary)
    )
    val values = items.map { parseGrams(it.second) }
    val maxValue = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.itemSpacing)
    ) {
        Text(
            text = stringResource(R.string.macro_nutrients),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        items.forEachIndexed { index, (label, value, color) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    modifier = Modifier.width(AppDimens.macroLabelWidth),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(AppDimens.macroBarHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(values[index] / maxValue)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = value.ifBlank { "--" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

private fun parseGrams(text: String): Float {
    val digits = text.filter { it.isDigit() || it == '.' }
    return digits.toFloatOrNull() ?: 0f
}

@Composable
fun FoodResultCard(
    result: FoodAnalysisResult,
    imagePath: String?,
    dailyCalorieGoal: Int,
    analysisFailed: Boolean,
    isTtsMuted: Boolean,
    isSpeaking: Boolean,
    onEditName: () -> Unit,
    onToggleTts: () -> Unit,
    onRetake: () -> Unit,
    onPickGallery: () -> Unit,
    onRetry: () -> Unit,
    onFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
        shape = RoundedCornerShape(AppDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        )
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(AppDimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.sectionSpacing)
        ) {
            if (analysisFailed) {
                FailedResultContent(onRetry = onRetry, onEditName = onEditName)
            } else {
                SuccessResultContent(
                    result = result,
                    imagePath = imagePath,
                    dailyCalorieGoal = dailyCalorieGoal,
                    onEditName = onEditName
                )
            }

            ActionButtons(
                isTtsMuted = isTtsMuted,
                isSpeaking = isSpeaking,
                onToggleTts = onToggleTts,
                onRetake = onRetake,
                onPickGallery = onPickGallery
            )

            OutlinedButton(
                onClick = onFeedback,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Icon(
                    Icons.Default.Feedback,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.feedback),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    isTtsMuted: Boolean,
    isSpeaking: Boolean,
    onToggleTts: () -> Unit,
    onRetake: () -> Unit,
    onPickGallery: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.itemSpacing)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleTts,
                modifier = Modifier.size(AppDimens.actionIconButton)
            ) {
                Icon(
                    imageVector = if (isTtsMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = stringResource(R.string.tts_toggle),
                    modifier = Modifier.size(AppDimens.actionIconSize),
                    tint = if (isSpeaking) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = stringResource(R.string.tts_toggle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.itemSpacing)
        ) {
            FilledTonalButton(
                onClick = onRetake,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.retake_photo),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FilledTonalButton(
                onClick = onPickGallery,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.analyze_another),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SuccessResultContent(
    result: FoodAnalysisResult,
    imagePath: String?,
    dailyCalorieGoal: Int,
    onEditName: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        imagePath?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = null,
                modifier = Modifier
                    .size(AppDimens.resultThumbnail)
                    .clip(RoundedCornerShape(AppDimens.resultThumbnailRadius)),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = result.foodName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(
                    onClick = onEditName,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_name),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.calories_format, result.calories),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        CalorieRingChart(
            calories = result.caloriesValue(),
            dailyGoal = dailyCalorieGoal,
            modifier = Modifier.size(AppDimens.calorieRing)
        )
    }

    Text(
        text = result.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
    )

    if (result.protein.isNotBlank() || result.fat.isNotBlank() || result.carbs.isNotBlank()) {
        MacroNutrientBars(
            protein = result.protein,
            fat = result.fat,
            carbs = result.carbs
        )
    }
}

@Composable
private fun FailedResultContent(
    onRetry: () -> Unit,
    onEditName: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.itemSpacing)
    ) {
        Text(
            text = stringResource(R.string.analysis_failed_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.height(40.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.retry), style = MaterialTheme.typography.labelLarge)
        }
        TextButton(onClick = onEditName) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.manual_input), style = MaterialTheme.typography.labelLarge)
        }
    }
}
