package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SampleArchiveType
import com.example.data.ZipEngineRepository
import com.example.model.RecoveryPresetMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Zip Unlocker", appName)
    }

    @Test
    fun `create encrypted zip verify password recovery and extract files`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ZipEngineRepository(context)

        // 1. Generate real AES-256 PIN-locked archive (PIN: 1234)
        val archiveInfo = repository.generateSampleEncryptedArchive(SampleArchiveType.PIN_LOCKED_VAULT)
        assertTrue(archiveInfo.isValidZip)
        assertTrue(archiveInfo.isEncrypted)
        assertEquals(3, archiveInfo.fileCount)

        // 2. Verify wrong password fails and right password succeeds
        assertFalse(repository.verifyPasswordFast(archiveInfo.file, "9999"))
        assertTrue(repository.verifyPasswordFast(archiveInfo.file, "1234"))

        // 3. Test password recovery engine finds the PIN
        val recovered = repository.runSmartPasswordRecovery(
            archiveFile = archiveInfo.file,
            mode = RecoveryPresetMode.COMMON_PASSWORDS,
            userHint = "",
            customCandidates = emptyList()
        ) { _, _, _ -> }
        assertEquals("1234", recovered)

        // 4. Test real extraction with progress callback
        var progressCalled = false
        val extractResult = repository.unlockAndExtractAll(
            archiveFile = archiveInfo.file,
            password = recovered
        ) { processed, total, _ ->
            if (processed == total) progressCalled = true
        }
        assertTrue(extractResult.isSuccess)
        assertTrue(progressCalled)
        val batch = extractResult.getOrNull()
        assertNotNull(batch)
        assertEquals(3, batch!!.files.size)
    }
}
