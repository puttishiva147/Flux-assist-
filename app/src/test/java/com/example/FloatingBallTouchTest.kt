package com.example

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.example.data.models.BallSettings
import com.example.overlay.FloatingBallView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FloatingBallTouchTest {

    private fun obtainDown(x: Float, y: Float, downTime: Long = SystemClock.uptimeMillis()): MotionEvent {
        return MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
    }

    private fun obtainMove(downTime: Long, x: Float, y: Float, eventTime: Long = SystemClock.uptimeMillis()): MotionEvent {
        return MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x, y, 0)
    }

    private fun obtainUp(downTime: Long, x: Float, y: Float, eventTime: Long = SystemClock.uptimeMillis()): MotionEvent {
        return MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0)
    }

    @Test
    fun `simple tap triggers onBallTapped immediately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(100, 100).apply {
            x = 200
            y = 300
        }
        var tapCount = 0

        val ballView = FloatingBallView(
            context = context,
            windowManager = wm,
            layoutParams = params,
            settings = BallSettings(),
            onBallTapped = { tapCount++ },
            onBallLongPressed = {},
            onPositionChanged = { _, _ -> }
        )

        val downTime = SystemClock.uptimeMillis()
        ballView.dispatchTouchEvent(obtainDown(50f, 50f, downTime))
        assertEquals(0, tapCount) // down should not trigger tap yet (waiting for release or drag)

        ballView.dispatchTouchEvent(obtainUp(downTime, 50f, 50f, downTime + 40))
        assertEquals(1, tapCount)
    }

    @Test
    fun `repeated rapid taps trigger repeatedly without delay`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(100, 100).apply {
            x = 200
            y = 300
        }
        var tapCount = 0

        val ballView = FloatingBallView(
            context = context,
            windowManager = wm,
            layoutParams = params,
            settings = BallSettings(),
            onBallTapped = { tapCount++ },
            onBallLongPressed = {},
            onPositionChanged = { _, _ -> }
        )

        // Simulate 5 rapid taps
        for (i in 0 until 5) {
            val downTime = SystemClock.uptimeMillis() + (i * 100L)
            ballView.dispatchTouchEvent(obtainDown(50f, 50f, downTime))
            ballView.dispatchTouchEvent(obtainUp(downTime, 50f, 50f, downTime + 30))
        }

        assertEquals(5, tapCount)
    }

    @Test
    fun `dragging ball updates position and does not trigger tap`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(100, 100).apply {
            x = 200
            y = 300
        }
        var tapCount = 0
        var positionChangedCalled = false

        val ballView = FloatingBallView(
            context = context,
            windowManager = wm,
            layoutParams = params,
            settings = BallSettings(edgeSnapping = false),
            onBallTapped = { tapCount++ },
            onBallLongPressed = {},
            onPositionChanged = { _, _ -> positionChangedCalled = true }
        )

        val downTime = SystemClock.uptimeMillis()
        ballView.dispatchTouchEvent(obtainDown(50f, 50f, downTime))
        // Drag 100px away (well beyond touch slop)
        ballView.dispatchTouchEvent(obtainMove(downTime, 150f, 150f, downTime + 100))
        ballView.dispatchTouchEvent(obtainUp(downTime, 150f, 150f, downTime + 200))

        assertEquals(0, tapCount) // Must NOT count as a tap
        assertTrue(positionChangedCalled)
    }

    @Test
    fun `tap immediately after dragging triggers tap cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(100, 100).apply {
            x = 200
            y = 300
        }
        var tapCount = 0

        val ballView = FloatingBallView(
            context = context,
            windowManager = wm,
            layoutParams = params,
            settings = BallSettings(edgeSnapping = false),
            onBallTapped = { tapCount++ },
            onBallLongPressed = {},
            onPositionChanged = { _, _ -> }
        )

        // 1. Drag
        val dragStart = SystemClock.uptimeMillis()
        ballView.dispatchTouchEvent(obtainDown(50f, 50f, dragStart))
        ballView.dispatchTouchEvent(obtainMove(dragStart, 120f, 120f, dragStart + 80))
        ballView.dispatchTouchEvent(obtainUp(dragStart, 120f, 120f, dragStart + 150))
        assertEquals(0, tapCount)

        // 2. Immediate Tap right after drag release
        val tapStart = dragStart + 160
        ballView.dispatchTouchEvent(obtainDown(50f, 50f, tapStart))
        ballView.dispatchTouchEvent(obtainUp(tapStart, 50f, 50f, tapStart + 40))

        assertEquals(1, tapCount)
    }
}
