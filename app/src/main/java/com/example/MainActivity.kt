package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CreatorTabScreen
import com.example.ui.screens.RecoveryTabScreen
import com.example.ui.screens.UnlockerTabScreen
import com.example.ui.screens.VaultTabScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppTab
import com.example.viewmodel.ZipUnlockerUiState
import com.example.viewmodel.ZipUnlockerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                ZipUnlockerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZipUnlockerApp(
    viewModel: ZipUnlockerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.selectedTab != AppTab.UNLOCKER) {
        BackHandler {
            viewModel.selectTab(AppTab.UNLOCKER)
        }
    }

    // SAF Launcher for picking a .zip archive
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onZipUriPicked(uri)
        }
    }

    // SAF Launcher for choosing an output directory (OpenDocumentTree)
    val outputFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            viewModel.onOutputFolderPicked(treeUri)
        }
    }

    // SAF Launcher for picking multiple files to compress & encrypt
    val creatorFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onCreatorFilesPicked(uris)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Zip Unlocker",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "জিপ আনলকার, পাসওয়ার্ড রিকভারি ও ভল্ট",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    ZipUnlockerBottomBar(
                        selectedTab = uiState.selectedTab,
                        vaultCount = uiState.extractedBatches.size,
                        onSelectTab = viewModel::selectTab
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    ZipUnlockerNavigationRail(
                        selectedTab = uiState.selectedTab,
                        vaultCount = uiState.extractedBatches.size,
                        onSelectTab = viewModel::selectTab
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 760.dp)
                    ) {
                        AnimatedVisibility(
                            visible = uiState.statusBannerBn != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            uiState.statusBannerBn?.let { msg ->
                                StatusFeedbackBanner(
                                    message = msg,
                                    isError = uiState.isErrorBanner,
                                    onDismiss = viewModel::dismissBanner
                                )
                            }
                        }

                        ZipUnlockerTabContent(
                            uiState = uiState,
                            onPickZip = {
                                zipPickerLauncher.launch(
                                    arrayOf(
                                        "application/zip",
                                        "application/x-zip-compressed",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            },
                            onPickOutputFolder = {
                                outputFolderLauncher.launch(null)
                            },
                            onPickFilesForCreator = {
                                creatorFilesLauncher.launch(arrayOf("*/*"))
                            },
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZipUnlockerTabContent(
    uiState: ZipUnlockerUiState,
    onPickZip: () -> Unit,
    onPickOutputFolder: () -> Unit,
    onPickFilesForCreator: () -> Unit,
    viewModel: ZipUnlockerViewModel,
    modifier: Modifier = Modifier
) {
    when (uiState.selectedTab) {
        AppTab.UNLOCKER -> {
            UnlockerTabScreen(
                uiState = uiState,
                onPickZipClick = onPickZip,
                onPickOutputFolderClick = onPickOutputFolder,
                onResetOutputFolderClick = viewModel::resetOutputFolderToDefault,
                onLoadSampleArchive = { type -> viewModel.loadSampleArchive(type) },
                onPasswordChange = viewModel::updatePasswordInput,
                onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                onUnlockAndExtract = viewModel::unlockAndExtractCurrentArchive,
                onGoToRecovery = { viewModel.selectTab(AppTab.RECOVERY) },
                onToggleEntry = viewModel::toggleEntrySelection,
                onSelectAllEntries = viewModel::selectAllEntries,
                onClearEntries = viewModel::clearEntrySelection,
                modifier = modifier
            )
        }
        AppTab.RECOVERY -> {
            RecoveryTabScreen(
                uiState = uiState,
                onSelectMode = viewModel::setRecoveryMode,
                onUpdateHint = viewModel::updateRecoveryHint,
                onUpdateCustomCandidates = viewModel::updateCustomCandidatesText,
                onStartScan = viewModel::startPasswordRecovery,
                onCancelScan = viewModel::cancelPasswordRecovery,
                onApplyAndUnlock = viewModel::applyRecoveredPasswordAndExtract,
                modifier = modifier
            )
        }
        AppTab.CREATOR -> {
            CreatorTabScreen(
                uiState = uiState,
                onArchiveNameChange = viewModel::updateCreatorArchiveName,
                onPasswordChange = viewModel::updateCreatorPassword,
                onToggleAes256 = viewModel::toggleCreatorAes256,
                onPickFilesToZip = onPickFilesForCreator,
                onAddQuickNote = viewModel::addQuickNoteToCreator,
                onRemoveDraftFile = viewModel::removeCreatorDraftFile,
                onCreateZipClick = viewModel::buildEncryptedZipFromCreator,
                modifier = modifier
            )
        }
        AppTab.VAULT -> {
            VaultTabScreen(
                uiState = uiState,
                onPreviewFile = viewModel::openFilePreview,
                onClosePreview = viewModel::closeFilePreview,
                onDeleteBatch = viewModel::deleteBatch,
                onGoToUnlocker = { viewModel.selectTab(AppTab.UNLOCKER) },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun StatusFeedbackBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val bg = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
    val fg = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("status_feedback_banner"),
        color = bg,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = fg,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "বন্ধ করুন",
                    tint = fg,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ZipUnlockerBottomBar(
    selectedTab: AppTab,
    vaultCount: Int,
    onSelectTab: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        AppTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                modifier = Modifier.testTag("nav_tab_${tab.route}"),
                icon = {
                    if (tab == AppTab.VAULT && vaultCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge { Text(vaultCount.toString()) }
                            }
                        ) {
                            Icon(
                                imageVector = tabIcon(tab, selected),
                                contentDescription = tab.labelBn
                            )
                        }
                    } else {
                        Icon(
                            imageVector = tabIcon(tab, selected),
                            contentDescription = tab.labelBn
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.labelBn,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun ZipUnlockerNavigationRail(
    selectedTab: AppTab,
    vaultCount: Int,
    onSelectTab: (AppTab) -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.weight(1f))
        AppTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .testTag("rail_tab_${tab.route}"),
                icon = {
                    if (tab == AppTab.VAULT && vaultCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge { Text(vaultCount.toString()) }
                            }
                        ) {
                            Icon(
                                imageVector = tabIcon(tab, selected),
                                contentDescription = tab.labelBn
                            )
                        }
                    } else {
                        Icon(
                            imageVector = tabIcon(tab, selected),
                            contentDescription = tab.labelBn
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.labelBn,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun tabIcon(tab: AppTab, selected: Boolean): ImageVector {
    return when (tab) {
        AppTab.UNLOCKER -> if (selected) Icons.Filled.LockOpen else Icons.Outlined.LockOpen
        AppTab.RECOVERY -> if (selected) Icons.Filled.Radar else Icons.Outlined.Radar
        AppTab.CREATOR -> if (selected) Icons.Filled.Archive else Icons.Outlined.Archive
        AppTab.VAULT -> if (selected) Icons.Filled.FolderSpecial else Icons.Outlined.FolderSpecial
    }
}
