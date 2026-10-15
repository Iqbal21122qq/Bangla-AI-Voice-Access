package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ActionType
import com.example.service.GeminiService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Voice Access বাংলা", appName)
    }

    @Test
    fun `test youtube command parsing`() {
        val geminiService = GeminiService()
        val result = geminiService.fallbackLocalInterpreter("YouTube kholo", false)
        assertTrue(result.actions.any { it.type == ActionType.OPEN_YOUTUBE })
    }

    @Test
    fun `test play song command parsing`() {
        val geminiService = GeminiService()
        val result = geminiService.fallbackLocalInterpreter("রবীন্দ্রনাথের গান বাজাও", false)
        assertTrue(result.actions.any { it.type == ActionType.PLAY_SONG })
    }

    @Test
    fun `test screenshot and whatsapp share command parsing`() {
        val geminiService = GeminiService()
        val result = geminiService.fallbackLocalInterpreter("Screen shot ti WhatsApp e friend k pathaw", false)
        assertTrue(result.actions.any { it.type == ActionType.TAKE_SCREENSHOT })
        assertTrue(result.actions.any { it.type == ActionType.SHARE_WHATSAPP })
    }

    @Test
    fun `test phone call command parsing`() {
        val geminiService = GeminiService()
        val result = geminiService.fallbackLocalInterpreter("Phone e call korbe", false)
        assertTrue(result.actions.any { it.type == ActionType.PHONE_CALL })
    }
}
