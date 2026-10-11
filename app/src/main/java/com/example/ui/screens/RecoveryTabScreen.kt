package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.RecoveryPresetMode
import com.example.viewmodel.ZipUnlockerUiState

@Composable
fun RecoveryTabScreen(
    uiState: ZipUnlockerUiState,
    onSelectMode: (RecoveryPresetMode) -> Unit,
    onUpdateHint: (String) -> Unit,
    onUpdateCustomCandidates: (String) -> Unit,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onApplyAndUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val archive = uiState.currentArchive
    val progress = uiState.recoveryProgress

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("recovery_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "পাসওয়ার্ড টেস্টিং ও রিকভারি সহায়ক",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (archive != null) {
                                    "টার্গেট ফাইল: ${archive.displayName} (${archive.encryptionMethod.displayBn})"
                                } else {
                                    "কোনো জিপ ফাইল লোড করা নেই"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Text(
                        text = "আপনার নিজস্ব ফাইলগুলোর ভুলে যাওয়া পাসওয়ার্ড পরীক্ষার জন্য সাধারণ পিন, ডিকশনারি এবং হিন্ট ভেরিয়েশন স্বয়ংক্রিয়ভাবে টেস্ট করে।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Technical Scope & Ethical Notice Card
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("recovery_limitations_notice")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "প্রযুক্তিগত সীমাবদ্ধতা ও অনুমোদন বিজ্ঞপ্তি (Technical Limitations):",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "AES-256 বা শক্তিশালী র‍্যান্ডম পাসওয়ার্ড গাণিতিকভাবে গ্যারান্টিসহ ভাঙা সম্ভব নয়। এই টুলটি কেবল সাধারণ পিন, পরিচিত শব্দ এবং আপনার দেওয়া হিন্ট বা কাস্টম তালিকার পাসওয়ার্ড পরীক্ষা করে। শুধুমাত্র আপনার নিজের অনুমোদিত ফাইলের জন্য ব্যবহার করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Recovered Password Success Banner
        item {
            AnimatedVisibility(visible = progress.matchedPassword != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.tertiary,
                            RoundedCornerShape(18.dp)
                        )
                        .testTag("recovered_password_success_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "পাসওয়ার্ড সফলভাবে পাওয়া গেছে!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "পরীক্ষা করা হয়েছে: ${progress.testedCount} টি কম্বিনেশন (${progress.elapsedTimeMs} ms)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "শনাক্তকৃত পাসওয়ার্ড:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = progress.matchedPassword ?: "",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Button(
                            onClick = onApplyAndUnlock,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("apply_recovered_password_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "এই পাসওয়ার্ড দিয়ে এখনই আনলক ও এক্সট্রাক্ট করুন",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        // Live Radar Progress Card
        if (progress.isScanning || progress.testedCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (progress.isScanning) "স্ক্যান চলছে..." else "সর্বশেষ স্ক্যান রিপোর্ট",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "${progress.testedCount} / ${progress.totalCandidates}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        val fraction = if (progress.totalCandidates > 0) {
                            (progress.testedCount.toFloat() / progress.totalCandidates.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Text(
                            text = "পরীক্ষিত কম্বিনেশন: ${progress.testedCount} • সময়: ${progress.elapsedTimeMs} ms",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (progress.statusMessageBn.isNotEmpty()) {
                            Text(
                                text = progress.statusMessageBn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Recovery Strategy Selection
        item {
            Text(
                text = "১. পাসওয়ার্ড টেস্টিং মোড নির্বাচন করুন:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RecoveryPresetMode.entries.forEach { mode ->
                    val isSelected = uiState.recoveryMode == mode
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectMode(mode) }
                            .testTag("recovery_mode_${mode.name}"),
                        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.titleBn,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = mode.descriptionBn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Hint & Custom Wordlist Inputs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "২. পাসওয়ার্ড হিন্ট বা সম্ভাব্য শব্দ (ঐচ্ছিক):",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = uiState.recoveryHintInput,
                        onValueChange = onUpdateHint,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recovery_hint_input"),
                        label = { Text("সম্ভাব্য নাম, শহর বা শব্দ (উদাহরণ: dhaka, rahim)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = uiState.customCandidatesText,
                        onValueChange = onUpdateCustomCandidates,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_candidates_input"),
                        label = { Text("কাস্টম পাসওয়ার্ড তালিকা (কমা দিয়ে আলাদা করুন)") },
                        placeholder = { Text("উদাহরণ: secret99, myzip2026, 4455") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Start / Stop Recovery Action Button
        item {
            if (progress.isScanning) {
                OutlinedButton(
                    onClick = onCancelScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("stop_recovery_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "স্ক্যান থামান (Stop Scan)",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                Button(
                    onClick = onStartScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_recovery_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "পাসওয়ার্ড টেস্টিং শুরু করুন",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
