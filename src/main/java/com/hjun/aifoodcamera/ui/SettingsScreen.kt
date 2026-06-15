package com.hjun.aifoodcamera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hjun.aifoodcamera.R
import com.hjun.aifoodcamera.ui.theme.AppDimens
import com.hjun.aifoodcamera.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var goalText by remember(uiState.dailyCalorieGoal) {
        mutableStateOf(uiState.dailyCalorieGoal.toString())
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.goalSaved) {
        if (uiState.goalSaved) {
            snackbarHostState.showSnackbar("目标已保存")
            viewModel.clearGoalSaved()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(AppDimens.screenPadding)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.cardRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(AppDimens.cardPadding),
                verticalArrangement = Arrangement.spacedBy(AppDimens.itemSpacing)
            ) {
                Text(
                    text = stringResource(R.string.daily_calorie_goal),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { input ->
                        goalText = input.filter { it.isDigit() }
                        goalText.toIntOrNull()?.let { viewModel.updateGoalInput(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    suffix = {
                        Text(
                            "千卡",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
                Button(
                    onClick = { viewModel.saveGoal() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        stringResource(R.string.save),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.sectionSpacing))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.cardRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.cardPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tts_mute_default),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.tts_mute_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.isTtsMuted,
                    onCheckedChange = { viewModel.setTtsMuted(it) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        SnackbarHost(hostState = snackbarHostState)
    }
}
