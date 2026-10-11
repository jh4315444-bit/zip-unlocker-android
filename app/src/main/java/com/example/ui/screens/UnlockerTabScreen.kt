package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SampleArchiveType
import com.example.model.ExtractionProgressState
import com.example.model.ZipArchiveInfo
import com.example.model.ZipEntryItem
import com.example.util.FileFormatUtils
import com.example.viewmodel.ZipUnlockerUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnlockerTabScreen(
    uiState: ZipUnlockerUiState,
    onPickZipClick: () -> Unit,
    onPickOutputFolderClick: () -> Unit,
    onResetOutputFolderClick: () -> Unit,
    onLoadSampleArchive: (SampleArchiveType) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onUnlockAndExtract: () -> Unit,
    onGoToRecovery: () -> Unit,
    onToggleEntry: (String) -> Unit,
    onSelectAllEntries: () -> Unit,
    onClearEntries: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSampleOptions by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("unlocker_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner Card
        item {
            HeroVaultCard(
                onPickZipClick = onPickZipClick,
                onToggleSampleMenu = { showSampleOptions = !showSampleOptions }
            )
        }

        // Expandable Sample Test Vault Selector
        item {
            AnimatedVisibility(visible = showSampleOptions) {
                SampleArchivePickerCard(
                    onSelectSample = { type ->
                        onLoadSampleArchive(type)
                        showSampleOptions = false
                    }
                )
            }
        }

        // Current Loaded Archive Inspector & Unlocker
        val archive = uiState.currentArchive
        if (archive != null) {
            item {
                ArchiveStatusOverviewCard(
                    archive = archive,
                    passwordInput = uiState.passwordInput,
                    isPasswordVisible = uiState.isPasswordVisible,
                    isProcessing = uiState.isProcessing,
                    extractionProgress = uiState.extractionProgress,
                    outputFolderLabel = uiState.customOutputFolderLabel,
                    hasCustomOutputFolder = uiState.customOutputTreeUri != null,
                    selectedCount = uiState.selectedEntries.size,
                    onPasswordChange = onPasswordChange,
                    onTogglePasswordVisibility = onTogglePasswordVisibility,
                    onPickOutputFolderClick = onPickOutputFolderClick,
                    onResetOutputFolderClick = onResetOutputFolderClick,
                    onUnlockAndExtract = onUnlockAndExtract,
                    onGoToRecovery = onGoToRecovery
                )
            }

            // Entry Selection Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ভেতরের ফাইল তালিকা (${archive.entries.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "নির্বাচিত: ${uiState.selectedEntries.size} টি ফাইল • মোট আকার: ${FileFormatUtils.formatBytes(archive.totalUncompressedSize)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = onSelectAllEntries,
                            modifier = Modifier.testTag("select_all_entries_button")
                        ) {
                            Text("সব সিলেক্ট")
                        }
                        TextButton(
                            onClick = onClearEntries,
                            modifier = Modifier.testTag("clear_entries_button")
                        ) {
                            Text("ক্লিয়ার")
                        }
                    }
                }
            }

            // Individual ZIP Entries
            items(archive.entries, key = { it.fileName }) { entry ->
                ZipEntryRowCard(
                    entry = entry,
                    isSelected = uiState.selectedEntries.contains(entry.fileName),
                    onToggle = { onToggleEntry(entry.fileName) }
                )
            }
        }
    }
}

@Composable
private fun HeroVaultCard(
    onPickZipClick: () -> Unit,
    onToggleSampleMenu: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_vault_1791675391814),
                contentDescription = "Zip Unlocker Vault Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x990B1120),
                                Color(0xEB0B1120)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "AES-256 ও ZipCrypto আনলকার ইঞ্জিন",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "পাসওয়ার্ড লকড জিপ আনলক ও ফাইল এক্সট্রাক্ট করুন",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "আপনার ফোনের যেকোনো .zip ফাইল খুলুন, আউটপুট ফোল্ডার নির্বাচন করুন অথবা পাসওয়ার্ড রিকভারি টেস্ট করুন।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onPickZipClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_zip_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "জিপ ফাইল খুলুন",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleSampleMenu,
                        modifier = Modifier.testTag("toggle_sample_archives_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "টেস্ট জিপ",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleArchivePickerCard(
    onSelectSample: (SampleArchiveType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "পরীক্ষার জন্য রেডিমেড জিপ ফাইল (১-ক্লিকে লোড করুন):",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            SampleArchiveType.entries.forEach { sample ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectSample(sample) }
                        .testTag("sample_option_${sample.name}"),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (sample == SampleArchiveType.STANDARD_UNLOCKED_BUNDLE)
                                Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (sample == SampleArchiveType.STANDARD_UNLOCKED_BUNDLE)
                                MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sample.titleBn,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = sample.hintTextBn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArchiveStatusOverviewCard(
    archive: ZipArchiveInfo,
    passwordInput: String,
    isPasswordVisible: Boolean,
    isProcessing: Boolean,
    extractionProgress: ExtractionProgressState,
    outputFolderLabel: String,
    hasCustomOutputFolder: Boolean,
    selectedCount: Int,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onPickOutputFolderClick: () -> Unit,
    onResetOutputFolderClick: () -> Unit,
    onUnlockAndExtract: () -> Unit,
    onGoToRecovery: () -> Unit
) {
    val badgeColor = if (archive.isEncrypted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    val badgeBg = if (archive.isEncrypted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("current_archive_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (archive.isEncrypted) Icons.Default.Lock else Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = archive.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${FileFormatUtils.formatBytes(archive.totalSize)} • ${archive.fileCount} ফাইল",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (archive.isEncrypted) "লকড (Locked)" else "আনলকড (Open)",
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Encryption & Archive Metadata Row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(archive.encryptionMethod.displayBn) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                AssistChip(
                    onClick = {},
                    label = { Text("আনকম্প্রেসড: ${FileFormatUtils.formatBytes(archive.totalUncompressedSize)}") }
                )
            }

            // Output Folder Selection Row (SAF OpenDocumentTree)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "আউটপুট ফোল্ডার (Output Folder):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = outputFolderLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (hasCustomOutputFolder) {
                        IconButton(
                            onClick = onResetOutputFolderClick,
                            modifier = Modifier.testTag("reset_output_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "ডিফল্ট ফোল্ডার রিসেট করুন",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onPickOutputFolderClick,
                        modifier = Modifier.testTag("pick_output_folder_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ফোল্ডার বদলান", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Password Input Section when archive is encrypted
            if (archive.isEncrypted) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = onPasswordChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("zip_password_input"),
                        label = { Text("জিপ পাসওয়ার্ড লিখুন (Enter ZIP Password)") },
                        placeholder = { Text("উদাহরণ: 1234 বা dhaka123") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = onTogglePasswordVisibility,
                                modifier = Modifier.testTag("toggle_password_visibility_button")
                            ) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "পাসওয়ার্ড দেখুন"
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(
                                onClick = { onPasswordChange("1234") },
                                label = { Text("PIN: 1234") },
                                modifier = Modifier.testTag("quick_pin_1234_chip")
                            )
                            AssistChip(
                                onClick = { onPasswordChange("dhaka123") },
                                label = { Text("dhaka123") },
                                modifier = Modifier.testTag("quick_pass_dhaka123_chip")
                            )
                        }

                        TextButton(
                            onClick = onGoToRecovery,
                            modifier = Modifier.testTag("forgot_password_recovery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Password,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পাসওয়ার্ড ভুলে গেছেন?")
                        }
                    }
                }
            }

            // Extraction Progress Indicator
            AnimatedVisibility(visible = extractionProgress.isExtracting) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("extraction_progress_box")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "এক্সট্রাক্ট হচ্ছে: ${extractionProgress.currentFileName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${extractionProgress.percentComplete}% (${extractionProgress.processedEntries}/${extractionProgress.totalEntries})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (extractionProgress.percentComplete / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Primary Unlock & Extract CTA
            Button(
                onClick = onUnlockAndExtract,
                enabled = !isProcessing && archive.isValidZip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("unlock_extract_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (archive.isEncrypted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    contentColor = if (archive.isEncrypted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (archive.isEncrypted) {
                        "আনলক ও এক্সট্রাক্ট করুন ($selectedCount টি ফাইল)"
                    } else {
                        "সরাসরি আনজিপ করুন ($selectedCount টি ফাইল)"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun ZipEntryRowCard(
    entry: ZipEntryItem,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggle() }
            .testTag("zip_entry_${entry.fileName}"),
        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = "নির্বাচন করুন",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (entry.isEncrypted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "আসল: ${FileFormatUtils.formatBytes(entry.uncompressedSize)} • সংকুচিত: ${FileFormatUtils.formatBytes(entry.compressedSize)} (-${entry.compressionRatioPercent}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (entry.isEncrypted) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Encrypted entry",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
