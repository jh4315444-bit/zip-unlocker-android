package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SampleArchiveType
import com.example.data.ZipEngineRepository
import com.example.model.CreatorDraftFile
import com.example.model.ExtractedVaultBatch
import com.example.model.ExtractionProgressState
import com.example.model.RecoveryPresetMode
import com.example.model.RecoveryScanProgress
import com.example.model.ZipArchiveInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class AppTab(val route: String, val labelBn: String, val labelEn: String) {
    UNLOCKER("unlocker", "আনলকার", "Unlocker"),
    RECOVERY("recovery", "রিকভারি", "Recovery"),
    CREATOR("creator", "জিপ তৈরি", "Create ZIP"),
    VAULT("vault", "ভল্ট ফাইল", "Vault")
}

data class ZipUnlockerUiState(
    val selectedTab: AppTab = AppTab.UNLOCKER,
    val currentArchive: ZipArchiveInfo? = null,
    val selectedEntries: Set<String> = emptySet(),
    val passwordInput: String = "",
    val isPasswordVisible: Boolean = false,
    val isProcessing: Boolean = false,
    val extractionProgress: ExtractionProgressState = ExtractionProgressState(),
    val customOutputTreeUri: Uri? = null,
    val customOutputFolderLabel: String = "ইন্টারনাল ভল্ট (Default App Vault)",
    val statusBannerBn: String? = null,
    val isErrorBanner: Boolean = false,
    val recoveryMode: RecoveryPresetMode = RecoveryPresetMode.COMMON_PASSWORDS,
    val recoveryHintInput: String = "dhaka",
    val customCandidatesText: String = "",
    val recoveryProgress: RecoveryScanProgress = RecoveryScanProgress(),
    val creatorArchiveName: String = "My_Protected_Vault",
    val creatorPassword: String = "",
    val creatorUseAes256: Boolean = true,
    val creatorDraftFiles: List<CreatorDraftFile> = emptyList(),
    val extractedBatches: List<ExtractedVaultBatch> = emptyList(),
    val previewingFile: File? = null,
    val previewFileContent: String? = null,
    val recentUnlockedCount: Int = 0
)

class ZipUnlockerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ZipEngineRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(ZipUnlockerUiState())
    val uiState: StateFlow<ZipUnlockerUiState> = _uiState.asStateFlow()

    private var recoveryJob: Job? = null

    init {
        refreshVaultBatches()
        loadSampleArchive(SampleArchiveType.PIN_LOCKED_VAULT, silent = true)
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab, statusBannerBn = null) }
        if (tab == AppTab.VAULT) {
            refreshVaultBatches()
        }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBannerBn = null, isErrorBanner = false) }
    }

    fun updatePasswordInput(value: String) {
        _uiState.update { it.copy(passwordInput = value, isErrorBanner = false) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onOutputFolderPicked(treeUri: Uri) {
        try {
            val contentResolver = getApplication<Application>().contentResolver
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(treeUri, takeFlags)
        } catch (_: Exception) {
            // Ignore if URI does not support persistable permission
        }
        val label = repository.resolveTreeDirectoryLabel(treeUri)
        _uiState.update {
            it.copy(
                customOutputTreeUri = treeUri,
                customOutputFolderLabel = label,
                statusBannerBn = "আউটপুট ফোল্ডার নির্বাচিত হয়েছে: $label",
                isErrorBanner = false
            )
        }
    }

    fun resetOutputFolderToDefault() {
        _uiState.update {
            it.copy(
                customOutputTreeUri = null,
                customOutputFolderLabel = "ইন্টারনাল ভল্ট (Default App Vault)"
            )
        }
    }

    fun toggleEntrySelection(fileName: String) {
        _uiState.update { state ->
            val updated = state.selectedEntries.toMutableSet()
            if (updated.contains(fileName)) {
                updated.remove(fileName)
            } else {
                updated.add(fileName)
            }
            state.copy(selectedEntries = updated)
        }
    }

    fun selectAllEntries() {
        _uiState.update { state ->
            val all = state.currentArchive?.entries?.map { it.fileName }?.toSet() ?: emptySet()
            state.copy(selectedEntries = all)
        }
    }

    fun clearEntrySelection() {
        _uiState.update { it.copy(selectedEntries = emptySet()) }
    }

    fun onZipUriPicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusBannerBn = null) }
            try {
                val localFile = repository.copyUriToWorkspace(uri)
                val info = repository.inspectZipFile(localFile)
                val allNames = info.entries.map { it.fileName }.toSet()
                val msg = if (!info.isValidZip) {
                    "নির্বাচিত ফাইলটি বৈধ জিপ (ZIP) ফাইল নয়!"
                } else if (info.isEncrypted) {
                    "পাসওয়ার্ড লকড জিপ শনাক্ত হয়েছে (${info.encryptionMethod.displayBn})। পাসওয়ার্ড দিন অথবা রিকভারি চালান।"
                } else {
                    "জিপ ফাইল লোড হয়েছে (${info.fileCount} টি ফাইল)। এখন আনজিপ করতে পারেন।"
                }
                _uiState.update {
                    it.copy(
                        currentArchive = info,
                        selectedEntries = allNames,
                        isProcessing = false,
                        statusBannerBn = msg,
                        isErrorBanner = !info.isValidZip,
                        recoveryProgress = RecoveryScanProgress()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusBannerBn = "ফাইল লোড করতে সমস্যা হয়েছে: ${e.localizedMessage}",
                        isErrorBanner = true
                    )
                }
            }
        }
    }

    fun loadSampleArchive(type: SampleArchiveType, silent: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val info = repository.generateSampleEncryptedArchive(type)
                val allNames = info.entries.map { it.fileName }.toSet()
                val defaultHint = when (type) {
                    SampleArchiveType.HINT_LOCKED_PROJECT -> "dhaka"
                    else -> "dhaka"
                }
                _uiState.update {
                    it.copy(
                        currentArchive = info,
                        selectedEntries = allNames,
                        passwordInput = "",
                        recoveryHintInput = defaultHint,
                        isProcessing = false,
                        statusBannerBn = if (silent) null else "লোড হয়েছে: ${type.titleBn} — ${type.hintTextBn}",
                        isErrorBanner = false,
                        recoveryProgress = RecoveryScanProgress()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusBannerBn = "ডেমো জিপ তৈরি করতে ত্রুটি: ${e.localizedMessage}",
                        isErrorBanner = true
                    )
                }
            }
        }
    }

    fun unlockAndExtractCurrentArchive() {
        val archive = _uiState.value.currentArchive ?: return
        val password = _uiState.value.passwordInput.trim()
        val selected = _uiState.value.selectedEntries
        val treeUri = _uiState.value.customOutputTreeUri

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    extractionProgress = ExtractionProgressState(
                        isExtracting = true,
                        currentFileName = archive.displayName,
                        processedEntries = 0,
                        totalEntries = selected.size.coerceAtLeast(1),
                        percentComplete = 0
                    ),
                    statusBannerBn = "জিপ আনলক ও ফাইল এক্সট্রাক্ট হচ্ছে..."
                )
            }
            val result = repository.unlockAndExtractAll(
                archiveFile = archive.file,
                password = password.ifEmpty { null },
                selectedEntryNames = selected.ifEmpty { null },
                customOutputTreeUri = treeUri
            ) { processed, total, currentEntry ->
                val pct = ((processed.toFloat() / total.toFloat()) * 100f).toInt().coerceIn(0, 100)
                _uiState.update { state ->
                    state.copy(
                        extractionProgress = ExtractionProgressState(
                            isExtracting = true,
                            currentFileName = currentEntry,
                            processedEntries = processed,
                            totalEntries = total,
                            percentComplete = pct
                        )
                    )
                }
            }
            result.fold(
                onSuccess = { batch ->
                    val batches = repository.loadExtractedVaultBatches()
                    val exportNote = batch.exportedToExternalUri?.let { " ও ফোল্ডারে ($it) সেভ হয়েছে" } ?: ""
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            extractionProgress = ExtractionProgressState(isExtracting = false, percentComplete = 100),
                            extractedBatches = batches,
                            recentUnlockedCount = it.recentUnlockedCount + 1,
                            selectedTab = AppTab.VAULT,
                            statusBannerBn = "সফলভাবে আনলক ও আনজিপ সম্পন্ন হয়েছে! (${batch.files.size} টি ফাইল ভল্টে$exportNote)",
                            isErrorBanner = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            extractionProgress = ExtractionProgressState(isExtracting = false),
                            statusBannerBn = err.message ?: "আনলক ব্যর্থ হয়েছে!",
                            isErrorBanner = true
                        )
                    }
                }
            )
        }
    }

    fun setRecoveryMode(mode: RecoveryPresetMode) {
        _uiState.update { it.copy(recoveryMode = mode) }
    }

    fun updateRecoveryHint(hint: String) {
        _uiState.update { it.copy(recoveryHintInput = hint) }
    }

    fun updateCustomCandidatesText(text: String) {
        _uiState.update { it.copy(customCandidatesText = text) }
    }

    fun startPasswordRecovery() {
        val archive = _uiState.value.currentArchive
        if (archive == null) {
            _uiState.update {
                it.copy(
                    statusBannerBn = "প্রথমে আনলকার ট্যাব থেকে একটি লকড জিপ ফাইল নির্বাচন করুন।",
                    isErrorBanner = true
                )
            }
            return
        }
        if (!archive.isEncrypted) {
            _uiState.update {
                it.copy(
                    statusBannerBn = "এই জিপ ফাইলটিতে কোনো পাসওয়ার্ড লক নেই! সরাসরি এক্সট্রাক্ট করুন।",
                    isErrorBanner = false
                )
            }
            return
        }

        recoveryJob?.cancel()
        val mode = _uiState.value.recoveryMode
        val hint = _uiState.value.recoveryHintInput
        val customList = _uiState.value.customCandidatesText
            .split("\n", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val startTime = System.currentTimeMillis()
        recoveryJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    recoveryProgress = RecoveryScanProgress(
                        isScanning = true,
                        statusMessageBn = "ডিকশনারি ও হিন্ট কম্বিনেশন পরীক্ষা করা হচ্ছে...",
                        statusMessageEn = "Testing password permutations..."
                    ),
                    statusBannerBn = null
                )
            }

            val matched = repository.runSmartPasswordRecovery(
                archiveFile = archive.file,
                mode = mode,
                userHint = hint,
                customCandidates = customList
            ) { tested, total, currentWord ->
                val elapsed = System.currentTimeMillis() - startTime
                _uiState.update { state ->
                    state.copy(
                        recoveryProgress = state.recoveryProgress.copy(
                            isScanning = true,
                            currentCandidate = currentWord,
                            testedCount = tested,
                            totalCandidates = total,
                            elapsedTimeMs = elapsed
                        )
                    )
                }
            }

            val totalElapsed = System.currentTimeMillis() - startTime
            if (matched != null) {
                _uiState.update { state ->
                    state.copy(
                        passwordInput = matched,
                        recoveryProgress = state.recoveryProgress.copy(
                            isScanning = false,
                            matchedPassword = matched,
                            elapsedTimeMs = totalElapsed,
                            statusMessageBn = "পাসওয়ার্ড পাওয়া গেছে!"
                        ),
                        statusBannerBn = "পাসওয়ার্ড ম্যাচ করেছে! এখন ১-ক্লিকে ফাইল আনলক ও এক্সট্রাক্ট করুন।",
                        isErrorBanner = false
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        recoveryProgress = state.recoveryProgress.copy(
                            isScanning = false,
                            matchedPassword = null,
                            elapsedTimeMs = totalElapsed,
                            statusMessageBn = "এই তালিকায় পাসওয়ার্ড মেলেনি। নতুন হিন্ট বা কাস্টম শব্দ তালিকা দিয়ে চেষ্টা করুন।"
                        ),
                        statusBannerBn = "স্ক্যান সম্পন্ন হয়েছে কিন্তু পাসওয়ার্ড মেলেনি। জটিল পাসওয়ার্ড উদ্ধারের জন্য সঠিক হিন্ট বা ওয়ার্ডলিস্ট প্রয়োজন।",
                        isErrorBanner = true
                    )
                }
            }
        }
    }

    fun cancelPasswordRecovery() {
        recoveryJob?.cancel()
        _uiState.update { state ->
            state.copy(
                recoveryProgress = state.recoveryProgress.copy(
                    isScanning = false,
                    statusMessageBn = "স্ক্যান বাতিল করা হয়েছে।"
                )
            )
        }
    }

    fun applyRecoveredPasswordAndExtract() {
        val matched = _uiState.value.recoveryProgress.matchedPassword ?: return
        _uiState.update { it.copy(passwordInput = matched) }
        unlockAndExtractCurrentArchive()
    }

    fun updateCreatorArchiveName(name: String) {
        _uiState.update { it.copy(creatorArchiveName = name) }
    }

    fun updateCreatorPassword(password: String) {
        _uiState.update { it.copy(creatorPassword = password) }
    }

    fun toggleCreatorAes256(useAes: Boolean) {
        _uiState.update { it.copy(creatorUseAes256 = useAes) }
    }

    fun onCreatorFilesPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val staged = uris.map { repository.stageUriForCreator(it) }
                _uiState.update {
                    it.copy(
                        creatorDraftFiles = it.creatorDraftFiles + staged,
                        isProcessing = false,
                        statusBannerBn = "${staged.size} টি ফাইল জিপ তৈরির তালিকায় যুক্ত হয়েছে।"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusBannerBn = "ফাইল যোগ করতে সমস্যা: ${e.localizedMessage}",
                        isErrorBanner = true
                    )
                }
            }
        }
    }

    fun addQuickNoteToCreator(title: String, body: String) {
        viewModelScope.launch {
            val draft = repository.createQuickTextFileForDraft(title, body)
            _uiState.update {
                it.copy(
                    creatorDraftFiles = it.creatorDraftFiles + draft,
                    statusBannerBn = "\"${draft.fileName}\" তালিকায় যোগ করা হয়েছে।",
                    isErrorBanner = false
                )
            }
        }
    }

    fun removeCreatorDraftFile(id: String) {
        _uiState.update { state ->
            state.copy(creatorDraftFiles = state.creatorDraftFiles.filterNot { it.id == id })
        }
    }

    fun buildEncryptedZipFromCreator() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusBannerBn = "জিপ আর্কাইভ তৈরি ও এনক্রিপ্ট করা হচ্ছে...") }
            val result = repository.createCustomZipArchive(
                archiveName = state.creatorArchiveName,
                draftFiles = state.creatorDraftFiles,
                password = state.creatorPassword.trim().ifEmpty { null },
                useAes256 = state.creatorUseAes256
            )
            result.fold(
                onSuccess = { archiveInfo ->
                    val allNames = archiveInfo.entries.map { it.fileName }.toSet()
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            currentArchive = archiveInfo,
                            selectedEntries = allNames,
                            passwordInput = state.creatorPassword.trim(),
                            selectedTab = AppTab.UNLOCKER,
                            statusBannerBn = "নতুন জিপ \"${archiveInfo.displayName}\" সফলভাবে তৈরি হয়েছে এবং আনলকারে লোড করা হয়েছে!",
                            isErrorBanner = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            statusBannerBn = err.message ?: "জিপ তৈরি ব্যর্থ হয়েছে।",
                            isErrorBanner = true
                        )
                    }
                }
            )
        }
    }

    fun refreshVaultBatches() {
        viewModelScope.launch {
            val batches = repository.loadExtractedVaultBatches()
            _uiState.update { it.copy(extractedBatches = batches) }
        }
    }

    fun deleteBatch(batch: ExtractedVaultBatch) {
        viewModelScope.launch {
            repository.deleteVaultBatch(batch)
            val batches = repository.loadExtractedVaultBatches()
            _uiState.update {
                it.copy(
                    extractedBatches = batches,
                    statusBannerBn = "\"${batch.sourceArchiveName}\" ভল্ট থেকে মুছে ফেলা হয়েছে।"
                )
            }
        }
    }

    fun openFilePreview(file: File) {
        viewModelScope.launch {
            val content = try {
                if (file.length() > 150 * 1024) {
                    file.inputStream().bufferedReader().use { reader ->
                        val buffer = CharArray(8192)
                        val read = reader.read(buffer)
                        String(buffer, 0, read.coerceAtLeast(0)) + "\n\n... [ফাইলটি অনেক বড়, প্রথম অংশ দেখানো হচ্ছে]"
                    }
                } else {
                    file.readText()
                }
            } catch (e: Exception) {
                "এই ফাইলটি বাইনারি বা টেক্সট হিসেবে প্রিভিউ করা সম্ভব নয়। শেয়ার বাটনে ক্লিক করে অন্য অ্যাপে খুলুন।"
            }
            _uiState.update {
                it.copy(
                    previewingFile = file,
                    previewFileContent = content
                )
            }
        }
    }

    fun closeFilePreview() {
        _uiState.update { it.copy(previewingFile = null, previewFileContent = null) }
    }
}
