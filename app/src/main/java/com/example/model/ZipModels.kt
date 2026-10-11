package com.example.model

import java.io.File

enum class EncryptionMethodLabel(val displayBn: String, val displayEn: String) {
    NONE("আনলকড (সাধারণ জিপ)", "Unlocked (Standard ZIP)"),
    ZIP_STANDARD("স্ট্যান্ডার্ড জিপ ক্রিপ্টো", "ZipCrypto Standard"),
    AES_128("AES-128 এনক্রিপশন", "AES-128 Bit Encryption"),
    AES_256("AES-256 মিলিটারি গ্রেড", "AES-256 Bit Encryption")
}

data class ZipEntryItem(
    val fileName: String,
    val isDirectory: Boolean,
    val uncompressedSize: Long,
    val compressedSize: Long,
    val isEncrypted: Boolean,
    val encryptionType: EncryptionMethodLabel,
    val crc: Long,
    val lastModifiedTime: Long
) {
    val compressionRatioPercent: Int
        get() = if (uncompressedSize > 0L && compressedSize <= uncompressedSize) {
            (((uncompressedSize - compressedSize).toDouble() / uncompressedSize.toDouble()) * 100.0).toInt()
        } else {
            0
        }
}

data class ZipArchiveInfo(
    val file: File,
    val displayName: String,
    val totalSize: Long,
    val isEncrypted: Boolean,
    val encryptionMethod: EncryptionMethodLabel,
    val entries: List<ZipEntryItem>,
    val isValidZip: Boolean,
    val comment: String? = null
) {
    val fileCount: Int get() = entries.count { !it.isDirectory }
    val folderCount: Int get() = entries.count { it.isDirectory }
    val totalUncompressedSize: Long get() = entries.sumOf { it.uncompressedSize }
}

data class ExtractedVaultBatch(
    val id: String,
    val sourceArchiveName: String,
    val outputDir: File,
    val timestampMillis: Long,
    val files: List<File>,
    val exportedToExternalUri: String? = null
)

data class ExtractionProgressState(
    val isExtracting: Boolean = false,
    val currentFileName: String = "",
    val processedEntries: Int = 0,
    val totalEntries: Int = 0,
    val percentComplete: Int = 0
)

data class RecoveryScanProgress(
    val isScanning: Boolean = false,
    val currentCandidate: String = "",
    val testedCount: Int = 0,
    val totalCandidates: Int = 0,
    val matchedPassword: String? = null,
    val statusMessageBn: String = "",
    val statusMessageEn: String = "",
    val elapsedTimeMs: Long = 0L
)

enum class RecoveryPresetMode(
    val titleBn: String,
    val titleEn: String,
    val descriptionBn: String
) {
    COMMON_PASSWORDS(
        "সাধারণ ও জনপ্রিয় পাসওয়ার্ড",
        "Common Archive Passwords",
        "সবচেয়ে বেশি ব্যবহৃত পাসওয়ার্ড, সংখ্যা ও পিন (যেমন 1234, 123456, admin, password, bangladesh)"
    ),
    NUMERIC_PIN_4(
        "৪-ডিজিট পিন স্ক্যান (0000-9999)",
        "4-Digit PIN Sweep (0000-9999)",
        "১৯০০-২০৩০ সাল এবং সকল ৪ সংখ্যার পিন দ্রুত পরীক্ষা করে"
    ),
    CUSTOM_HINT_MUTATION(
        "স্মার্ট হিন্ট ও শব্দ ভেরিয়েশন",
        "Smart Hint Permutations",
        "আপনার দেওয়া নামের সাথে সাল, @, #, 123, ! যোগ করে শত শত কম্বিনেশন তৈরি করে"
    )
}

data class CreatorDraftFile(
    val id: String,
    val fileName: String,
    val sizeBytes: Long,
    val localFile: File,
    val isGeneratedNote: Boolean = false
)
