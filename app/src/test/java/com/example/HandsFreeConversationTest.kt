package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.CodeViewModel
import com.example.voice.SpeechManager
import com.example.voice.VoiceCommandManager
import com.example.voice.VoiceCommandResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HandsFreeConversationTest {

    private lateinit var application: Application
    private lateinit var viewModel: CodeViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = CodeViewModel(application)
    }

    @Test
    fun testVoiceCommandRecognitionForHandsFree() {
        // Test French voice command
        val frenchCmd = VoiceCommandManager.parse("active le mode mains libres", "Code")
        assertTrue("French command should match StartHandsFree", frenchCmd is VoiceCommandResult.StartHandsFree)

        // Test English voice command
        val englishCmd = VoiceCommandManager.parse("hands free English practice please", "Code")
        assertTrue("English command should match StartHandsFree", englishCmd is VoiceCommandResult.StartHandsFree)

        // Test stop command
        val stopCmd = VoiceCommandManager.parse("stop hands free", "Code")
        assertTrue("Stop command should match StopHandsFree", stopCmd is VoiceCommandResult.StopHandsFree)
    }

    @Test
    fun testSpeechManagerHandsFreeModeToggle() {
        val speechManager = SpeechManager(
            context = application,
            onPartialResult = {},
            onFinalResult = {},
            onRmsUpdated = {},
            onListeningStateChanged = {},
            onErrorOccurred = {}
        )

        assertFalse("Initially hands-free should be false", speechManager.isHandsFree())

        speechManager.setHandsFree(true)
        assertTrue("Hands-free should be enabled", speechManager.isHandsFree())

        speechManager.setHandsFree(false)
        assertFalse("Hands-free should be disabled", speechManager.isHandsFree())

        speechManager.destroy()
    }

    @Test
    fun testViewModelHandsFreeConversationLifecycle() = runTest {
        assertFalse("Initially isHandsFreeMode is false", viewModel.isHandsFreeMode.value)
        assertFalse("Initially isHandsFreeSheetVisible is false", viewModel.isHandsFreeSheetVisible.value)

        // Start hands-free English conversation
        viewModel.startHandsFreeConversation(introPrompt = "Hello! Let's practice English.")
        assertTrue("isHandsFreeMode should be true", viewModel.isHandsFreeMode.value)
        assertTrue("isHandsFreeSheetVisible should be true", viewModel.isHandsFreeSheetVisible.value)
        assertEquals("Hello! Let's practice English.", viewModel.liveCodeReply.value)

        // Pause listening
        viewModel.pauseHandsFreeListening()
        assertTrue("Should still be in hands-free mode", viewModel.isHandsFreeMode.value)

        // Stop hands-free conversation
        viewModel.stopHandsFreeConversation()
        assertFalse("isHandsFreeMode should be false after stop", viewModel.isHandsFreeMode.value)
        assertFalse("isHandsFreeSheetVisible should be false after stop", viewModel.isHandsFreeSheetVisible.value)
    }
}
