package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.model.CreatorDraftFile
import com.example.model.EncryptionMethodLabel
import com.example.model.ExtractedVaultBatch
import com.example.model.RecoveryPresetMode
import com.example.model.ZipArchiveInfo
import com.example.model.ZipEntryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.FileHeader
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.coroutineContext

class ZipEngineRepository(private val context: Context) {

    private val workSpaceDir: File
        get() = File(context.filesDir, "zip_workspace").apply { if (!exists()) mkdirs() }

    private val extractedVaultRoot: File
        get() = File(context.filesDir, "unlocked_vault").apply { if (!exists()) mkdirs() }

    private val createdArchivesDir: File
        get() = File(context.filesDir, "created_zips").apply { if (!exists()) mkdirs() }

    private val draftStagingDir: File
        get() = File(context.cacheDir, "creator_staging").apply { if (!exists()) mkdirs() }

    suspend fun copyUriToWorkspace(uri: Uri): File = withContext(Dispatchers.IO) {
        val displayName = resolveDisplayName(uri) ?: "archive_${System.currentTimeMillis()}.zip"
        val safeName = displayName.replace(Regex("[^a-zA-Z0-9._\\-\\u0980-\\u09FF ]"), "_")
        val destFile = File(workSpaceDir, safeName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("ফাইলটি খোলা সম্ভব হয়নি (Unable to open URI)")
        destFile
    }

    fun resolveDisplayName(uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name ?: uri.lastPathSegment
    }

    fun resolveTreeDirectoryLabel(treeUri: Uri): String {
        val docId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (e: Exception) {
            null
        }
        return docId?.substringAfterLast(':')?.ifBlank { "Device Storage" }
            ?: treeUri.lastPathSegment
            ?: "Selected Output Folder"
    }

    suspend fun inspectZipFile(file: File): ZipArchiveInfo = withContext(Dispatchers.IO) {
        val zipFile = ZipFile(file)
        val isValid = zipFile.isValidZipFile
        if (!isValid) {
            return@withContext ZipArchiveInfo(
                file = file,
                displayName = file.name,
                totalSize = file.length(),
                isEncrypted = false,
                encryptionMethod = EncryptionMethodLabel.NONE,
                entries = emptyList(),
                isValidZip = false
            )
        }

        val headers: List<FileHeader> = try {
            zipFile.fileHeaders
        } catch (e: Exception) {
            emptyList()
        }

        val entries = headers.map { header ->
            val encMethod = mapEncryptionLabel(header)
            ZipEntryItem(
                fileName = header.fileName,
                isDirectory = header.isDirectory,
                uncompressedSize = header.uncompressedSize.coerceAtLeast(0L),
                compressedSize = header.compressedSize.coerceAtLeast(0L),
                isEncrypted = header.isEncrypted,
                encryptionType = encMethod,
                crc = header.crc,
                lastModifiedTime = header.lastModifiedTimeEpoch
            )
        }

        val archiveEncrypted = try {
            zipFile.isEncrypted
        } catch (e: Exception) {
            entries.any { it.isEncrypted }
        }

        val dominantEnc = entries.firstOrNull { it.isEncrypted }?.encryptionType
            ?: if (archiveEncrypted) EncryptionMethodLabel.AES_256 else EncryptionMethodLabel.NONE

        val comment = try {
            zipFile.comment
        } catch (e: Exception) {
            null
        }

        ZipArchiveInfo(
            file = file,
            displayName = file.name,
            totalSize = file.length(),
            isEncrypted = archiveEncrypted,
            encryptionMethod = dominantEnc,
            entries = entries,
            isValidZip = true,
            comment = comment
        )
    }

    private fun mapEncryptionLabel(header: FileHeader): EncryptionMethodLabel {
        if (!header.isEncrypted) return EncryptionMethodLabel.NONE
        return when (header.encryptionMethod) {
            EncryptionMethod.AES -> {
                val strength = header.aesExtraDataRecord?.aesKeyStrength
                if (strength == AesKeyStrength.KEY_STRENGTH_128) {
                    EncryptionMethodLabel.AES_128
                } else {
                    EncryptionMethodLabel.AES_256
                }
            }
            EncryptionMethod.ZIP_STANDARD, EncryptionMethod.ZIP_STANDARD_VARIANT_STRONG -> {
                EncryptionMethodLabel.ZIP_STANDARD
            }
            else -> EncryptionMethodLabel.ZIP_STANDARD
        }
    }

    /**
     * Verifies if a candidate password can decrypt and pass CRC/MAC verification on the first
     * non-directory entry in the archive. Passwords are never logged.
     */
    fun verifyPasswordFast(file: File, candidatePassword: String): Boolean {
        return try {
            val zipFile = ZipFile(file)
            if (!zipFile.isEncrypted) return true
            zipFile.setPassword(candidatePassword.toCharArray())
            val targetHeader = zipFile.fileHeaders.firstOrNull { !it.isDirectory }
                ?: zipFile.fileHeaders.firstOrNull()
                ?: return false

            val buffer = ByteArray(512)
            zipFile.getInputStream(targetHeader).use { stream ->
                while (stream.read(buffer) != -1) {
                    // Read stream to verify both header and CRC/MAC integrity
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Unlocks and extracts selected entries with entry-by-entry progress reporting and optional
     * export to a user-chosen Android SAF directory (OpenDocumentTree).
     */
    suspend fun unlockAndExtractAll(
        archiveFile: File,
        password: String?,
        selectedEntryNames: Set<String>? = null,
        customOutputTreeUri: Uri? = null,
        onProgress: (processed: Int, total: Int, currentEntry: String) -> Unit = { _, _, _ -> }
    ): Result<ExtractedVaultBatch> = withContext(Dispatchers.IO) {
        try {
            val zipFile = ZipFile(archiveFile)
            if (zipFile.isEncrypted) {
                if (password.isNullOrEmpty()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("এই জিপ ফাইলটি পাসওয়ার্ড দিয়ে লক করা। অনুগ্রহ করে পাসওয়ার্ড দিন বা পাসওয়ার্ড রিকভারি টুল ব্যবহার করুন।")
                    )
                }
                zipFile.setPassword(password.toCharArray())
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val baseName = archiveFile.nameWithoutExtension.ifBlank { "unlocked" }
            val destFolder = File(extractedVaultRoot, "${baseName}_$timeStamp").apply { mkdirs() }

            val headersToExtract = if (selectedEntryNames.isNullOrEmpty()) {
                zipFile.fileHeaders
            } else {
                zipFile.fileHeaders.filter { selectedEntryNames.contains(it.fileName) }
            }

            val total = headersToExtract.size.coerceAtLeast(1)
            for ((index, header) in headersToExtract.withIndex()) {
                coroutineContext.ensureActive()
                onProgress(index, total, header.fileName)
                zipFile.extractFile(header, destFolder.absolutePath)
                onProgress(index + 1, total, header.fileName)
            }

            val extractedFiles = destFolder.walkTopDown().filter { it.isFile }.toList()
            if (extractedFiles.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("কোনো ফাইল এক্সট্রাক্ট হয়নি বা পাসওয়ার্ড সঠিক নয়।")
                )
            }

            // If user selected a custom output folder via SAF OpenDocumentTree, copy extracted files there as well
            var exportedLabel: String? = null
            if (customOutputTreeUri != null) {
                exportedLabel = copyExtractedFilesToSafTree(customOutputTreeUri, baseName, extractedFiles)
            }

            Result.success(
                ExtractedVaultBatch(
                    id = "${baseName}_$timeStamp",
                    sourceArchiveName = archiveFile.name,
                    outputDir = destFolder,
                    timestampMillis = System.currentTimeMillis(),
                    files = extractedFiles,
                    exportedToExternalUri = exportedLabel
                )
            )
        } catch (e: ZipException) {
            val msg = if (e.message?.contains("Wrong password", ignoreCase = true) == true ||
                e.type == ZipException.Type.WRONG_PASSWORD
            ) {
                "ভুল পাসওয়ার্ড (Wrong Password)! সঠিক পাসওয়ার্ড দিন অথবা 'রিকভারি' ট্যাব থেকে পাসওয়ার্ড টেস্টিং চালান।"
            } else {
                "জিপ আনলক ত্রুটি: ${e.localizedMessage ?: "অজানা সমস্যা"}"
            }
            Result.failure(IllegalArgumentException(msg, e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun copyExtractedFilesToSafTree(
        treeUri: Uri,
        subFolderName: String,
        files: List<File>
    ): String? {
        return try {
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
            val subFolderUri = DocumentsContract.createDocument(
                context.contentResolver,
                parentDocumentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                subFolderName
            ) ?: parentDocumentUri

            for (file in files) {
                val fileUri = DocumentsContract.createDocument(
                    context.contentResolver,
                    subFolderUri,
                    "application/octet-stream",
                    file.name
                )
                if (fileUri != null) {
                    context.contentResolver.openOutputStream(fileUri)?.use { out ->
                        file.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                }
            }
            resolveTreeDirectoryLabel(treeUri) + "/$subFolderName"
        } catch (e: Exception) {
            null
        }
    }

    suspend fun runSmartPasswordRecovery(
        archiveFile: File,
        mode: RecoveryPresetMode,
        userHint: String,
        customCandidates: List<String>,
        onProgress: (tested: Int, total: Int, currentWord: String) -> Unit
    ): String? = withContext(Dispatchers.Default) {
        val candidates = buildCandidateDictionary(mode, userHint, customCandidates)
        val total = candidates.size
        for ((index, candidate) in candidates.withIndex()) {
            coroutineContext.ensureActive()
            if (index % 5 == 0 || index == total - 1) {
                onProgress(index + 1, total, candidate)
            }
            if (verifyPasswordFast(archiveFile, candidate)) {
                onProgress(index + 1, total, candidate)
                return@withContext candidate
            }
        }
        null
    }

    fun buildCandidateDictionary(
        mode: RecoveryPresetMode,
        userHint: String,
        customCandidates: List<String>
    ): List<String> {
        val set = LinkedHashSet<String>()

        customCandidates.map { it.trim() }.filter { it.isNotEmpty() }.forEach { set.add(it) }
        val cleanHint = userHint.trim()
        if (cleanHint.isNotEmpty()) {
            set.add(cleanHint)
            set.add(cleanHint.lowercase(Locale.ROOT))
            set.add(cleanHint.uppercase(Locale.ROOT))
            set.add(cleanHint.replaceFirstChar { it.uppercase(Locale.ROOT) })
        }

        val commonArchivePasswords = listOf(
            "1234", "12345", "123456", "12345678", "123456789", "0000", "1111", "2222",
            "password", "admin", "123123", "qwerty", "abc123", "zip123", "unlock",
            "secret", "vault2026", "bangladesh", "dhaka", "bd1234", "786786",
            "2024", "2025", "2026", "pass123", "test123", "root", "archive",
            "admin123", "welcome", "iloveyou", "112233", "01711", "01819", "9999"
        )

        when (mode) {
            RecoveryPresetMode.COMMON_PASSWORDS -> {
                set.addAll(commonArchivePasswords)
                if (cleanHint.isNotEmpty()) {
                    appendHintMutations(set, cleanHint)
                }
                for (year in 1970..2030) {
                    set.add(year.toString())
                }
            }
            RecoveryPresetMode.CUSTOM_HINT_MUTATION -> {
                if (cleanHint.isNotEmpty()) {
                    appendHintMutations(set, cleanHint)
                }
                set.addAll(commonArchivePasswords)
            }
            RecoveryPresetMode.NUMERIC_PIN_4 -> {
                for (year in 1980..2030) {
                    set.add(year.toString())
                }
                set.addAll(listOf("0000", "1234", "1111", "2222", "3333", "4444", "5555", "6666", "7777", "8888", "9999", "4321", "2580"))
                for (pin in 0..9999) {
                    set.add(String.format(Locale.US, "%04d", pin))
                }
            }
        }

        return set.toList()
    }

    private fun appendHintMutations(target: MutableSet<String>, rawHint: String) {
        val baseWords = rawHint.split(" ", ",", "_", "-")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(rawHint) }

        val suffixes = listOf(
            "", "1", "12", "123", "1234", "12345", "007", "786",
            "2023", "2024", "2025", "2026", "@123", "#123", "!", "@",
            "_123", "99", "00", "bd", "BD"
        )
        val prefixes = listOf("", "my", "the", "bd", "zip_", "@")

        for (word in baseWords) {
            val variants = listOf(
                word,
                word.lowercase(Locale.ROOT),
                word.uppercase(Locale.ROOT),
                word.replaceFirstChar { it.uppercase(Locale.ROOT) }
            ).distinct()

            for (v in variants) {
                for (suffix in suffixes) {
                    target.add("$v$suffix")
                }
                for (prefix in prefixes) {
                    if (prefix.isNotEmpty()) {
                        target.add("$prefix$v")
                        target.add("$prefix${v}123")
                    }
                }
            }
        }
    }

    suspend fun generateSampleEncryptedArchive(
        presetType: SampleArchiveType
    ): ZipArchiveInfo = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "sample_gen_${System.currentTimeMillis()}").apply { mkdirs() }

        val zipFileName: String
        val password: String?
        val encryptionMethod: EncryptionMethod

        when (presetType) {
            SampleArchiveType.PIN_LOCKED_VAULT -> {
                zipFileName = "গোপন_ডকুমেন্ট_ভল্ট_PIN.zip"
                password = "1234"
                encryptionMethod = EncryptionMethod.AES
            }
            SampleArchiveType.HINT_LOCKED_PROJECT -> {
                zipFileName = "Dhaka_Project_Backup_Locked.zip"
                password = "dhaka123"
                encryptionMethod = EncryptionMethod.ZIP_STANDARD
            }
            SampleArchiveType.STANDARD_UNLOCKED_BUNDLE -> {
                zipFileName = "Official_Forms_Bundle_Open.zip"
                password = null
                encryptionMethod = EncryptionMethod.NONE
            }
        }

        val file1 = File(tempDir, "গোপন_নোট_Secret_Notes.txt").apply {
            writeText(
                """
                =========================================
                ZIP UNLOCKER VAULT - গোপন ডকুমেন্ট
                =========================================
                অভিনন্দন! আপনি সফলভাবে জিপ ফাইলটি আনলক ও এক্সট্রাক্ট করেছেন।
                
                ফাইল স্ট্যাটাস: ডিক্রিপ্টেড ও ভেরিফাইড (CRC-32 Checked)
                সিকিউরিটি প্রোটোকল: ${if (password != null) "Password Protected (${encryptionMethod.name})" else "Standard Archive"}
                তারিখ: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())}
                
                এই ফাইলটি এখন আপনি সরাসরি অ্যাপের ভেতর পড়তে, কপি করতে কিংবা অন্য অ্যাপে শেয়ার করতে পারবেন।
                """.trimIndent()
            )
        }

        val file2 = File(tempDir, "আর্থিক_হিসাব_Budget_2026.csv").apply {
            writeText(
                """
                ক্রমিক,বিবরণ (Item),পরিমাণ (BDT),স্ট্যাটাস
                01,ক্লাউড সার্ভার ব্যাকআপ,12500,পরিশোধিত (Paid)
                02,সাইবার সিকিউরিটি অডিট,28000,পরিশোধিত (Paid)
                03,অ্যাপ ডেভেলপমেন্ট ফান্ড,45000,সংরক্ষিত (Reserved)
                04,জরুরি ভল্ট রিজার্ভ,19500,সক্রিয় (Active)
                """.trimIndent()
            )
        }

        val file3 = File(tempDir, "System_Recovery_Key.json").apply {
            writeText(
                """
                {
                  "vault_name": "Cyber-Vault-BD",
                  "encryption": "${encryptionMethod.name}",
                  "recovery_verified": true,
                  "access_code": "ZV-9981-BD-2026",
                  "owner_note": "Keep this recovery manifest safe after unzipping."
                }
                """.trimIndent()
            )
        }

        val targetZipFile = File(workSpaceDir, zipFileName)
        if (targetZipFile.exists()) targetZipFile.delete()

        val zipParams = ZipParameters().apply {
            compressionMethod = CompressionMethod.DEFLATE
            compressionLevel = CompressionLevel.NORMAL
            if (!password.isNullOrEmpty()) {
                isEncryptFiles = true
                this.encryptionMethod = encryptionMethod
                if (encryptionMethod == EncryptionMethod.AES) {
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }
            }
        }

        val zip = if (!password.isNullOrEmpty()) {
            ZipFile(targetZipFile, password.toCharArray())
        } else {
            ZipFile(targetZipFile)
        }

        zip.addFiles(listOf(file1, file2, file3), zipParams)
        tempDir.deleteRecursively()

        inspectZipFile(targetZipFile)
    }

    suspend fun createCustomZipArchive(
        archiveName: String,
        draftFiles: List<CreatorDraftFile>,
        password: String?,
        useAes256: Boolean
    ): Result<ZipArchiveInfo> = withContext(Dispatchers.IO) {
        try {
            if (draftFiles.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("অন্তত একটি ফাইল বা টেক্সট নোট যোগ করুন।"))
            }
            val cleanName = archiveName.trim().removeSuffix(".zip").ifBlank {
                "Vault_Archive_${System.currentTimeMillis() / 1000}"
            }
            val outZipFile = File(createdArchivesDir, "$cleanName.zip")
            if (outZipFile.exists()) outZipFile.delete()

            val params = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
                compressionLevel = CompressionLevel.NORMAL
                if (!password.isNullOrBlank()) {
                    isEncryptFiles = true
                    encryptionMethod = if (useAes256) EncryptionMethod.AES else EncryptionMethod.ZIP_STANDARD
                    if (useAes256) {
                        aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                    }
                }
            }

            val zipFile = if (!password.isNullOrBlank()) {
                ZipFile(outZipFile, password.toCharArray())
            } else {
                ZipFile(outZipFile)
            }

            val validFiles = draftFiles.map { it.localFile }.filter { it.exists() }
            zipFile.addFiles(validFiles, params)

            val workspaceCopy = File(workSpaceDir, outZipFile.name)
            outZipFile.copyTo(workspaceCopy, overwrite = true)

            Result.success(inspectZipFile(workspaceCopy))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stageUriForCreator(uri: Uri): CreatorDraftFile = withContext(Dispatchers.IO) {
        val displayName = resolveDisplayName(uri) ?: "file_${System.currentTimeMillis()}"
        val safeName = displayName.replace(Regex("[^a-zA-Z0-9._\\-\\u0980-\\u09FF ]"), "_")
        val destFile = File(draftStagingDir, safeName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        CreatorDraftFile(
            id = "${safeName}_${System.currentTimeMillis()}",
            fileName = safeName,
            sizeBytes = destFile.length(),
            localFile = destFile,
            isGeneratedNote = false
        )
    }

    suspend fun createQuickTextFileForDraft(title: String, content: String): CreatorDraftFile =
        withContext(Dispatchers.IO) {
            val cleanTitle = title.trim().removeSuffix(".txt").ifBlank { "Secret_Note_${System.currentTimeMillis() % 1000}" }
            val safeName = "${cleanTitle.replace(Regex("[^a-zA-Z0-9._\\-\\u0980-\\u09FF ]"), "_")}.txt"
            val destFile = File(draftStagingDir, safeName)
            destFile.writeText(content)
            CreatorDraftFile(
                id = "${safeName}_${System.currentTimeMillis()}",
                fileName = safeName,
                sizeBytes = destFile.length(),
                localFile = destFile,
                isGeneratedNote = true
            )
        }

    suspend fun loadExtractedVaultBatches(): List<ExtractedVaultBatch> = withContext(Dispatchers.IO) {
        val dirs = extractedVaultRoot.listFiles()?.filter { it.isDirectory } ?: emptyList()
        dirs.sortedByDescending { it.lastModified() }.mapNotNull { folder ->
            val files = folder.walkTopDown().filter { it.isFile }.toList()
            if (files.isEmpty()) null
            else ExtractedVaultBatch(
                id = folder.name,
                sourceArchiveName = folder.name.substringBeforeLast("_"),
                outputDir = folder,
                timestampMillis = folder.lastModified(),
                files = files
            )
        }
    }

    suspend fun deleteVaultBatch(batch: ExtractedVaultBatch) = withContext(Dispatchers.IO) {
        batch.outputDir.deleteRecursively()
    }
}

enum class SampleArchiveType(
    val titleBn: String,
    val subtitleBn: String,
    val hintTextBn: String
) {
    PIN_LOCKED_VAULT(
        "AES-256 পিন লকড জিপ (PIN: 1234)",
        "৪-ডিজিট পিন দিয়ে লক করা গোপন ডকুমেন্ট ভল্ট",
        "পাসওয়ার্ড: 1234 (পিন রিকভারি বা সরাসরি আনলক টেস্টের জন্য)"
    ),
    HINT_LOCKED_PROJECT(
        "পাসওয়ার্ড লকড প্রজেক্ট (Hint: dhaka)",
        "ZipCrypto পাসওয়ার্ড লকড প্রজেক্ট ব্যাকআপ",
        "হিন্ট: 'dhaka' লিখে স্মার্ট পাসওয়ার্ড রিকভারি টেস্ট করুন (আসল পাসওয়ার্ড: dhaka123)"
    ),
    STANDARD_UNLOCKED_BUNDLE(
        "সাধারণ আনলকড জিপ (পাসওয়ার্ড ছাড়া)",
        "সরাসরি ১-ক্লিকে আনজিপ করার জন্য নমুনা ফাইল",
        "কোনো পাসওয়ার্ড লাগবে না"
    )
}
