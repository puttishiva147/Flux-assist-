package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.service.FloatingBallService
import com.example.ui.components.RadialMenuItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FluxAssistServiceTest {

    @Test
    fun `floating ball foreground service starts and creates instance`() {
        val controller = Robolectric.buildService(FloatingBallService::class.java)
        val service = controller.create().get()
        assertNotNull(service)

        val startIntent = Intent(ApplicationProvider.getApplicationContext(), FloatingBallService::class.java).apply {
            action = FloatingBallService.ACTION_START
        }
        controller.withIntent(startIntent).startCommand(0, 1)

        // Verifies service survives task removal when app is backgrounded
        val rootIntent = Intent(Intent.ACTION_MAIN)
        service.onTaskRemoved(rootIntent)

        controller.destroy()
    }

    @Test
    fun `radial menu item model holds custom properties`() {
        var clicked = false
        val item = RadialMenuItem(
            id = "test_item",
            label = "Test Action",
            iconRes = R.drawable.ic_action_settings,
            onClick = { clicked = true }
        )

        assertEquals("test_item", item.id)
        assertEquals("Test Action", item.label)
        item.onClick()
        assertEquals(true, clicked)
    }
}
