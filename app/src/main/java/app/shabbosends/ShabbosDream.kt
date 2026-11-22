package app.shabbosends

import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Date
import java.util.TimeZone
import kotlin.random.Random

/**
 * A screen saver showing the time and the card's words, for phones where
 * power saving switches off the Always On Display. Android only runs screen
 * savers while the phone is charging or docked, so it is for leaving the
 * phone plugged in over Shabbos.
 *
 * Dim, white-on-black, and moved a little every minute so nothing burns in.
 */
class ShabbosDream : DreamService() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var root: FrameLayout
    private lateinit var block: LinearLayout
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var card: TextView
    private lateinit var next: TextView

    private val tick = object : Runnable {
        override fun run() {
            update()
            // Wake again just after the next minute starts.
            val now = System.currentTimeMillis()
            handler.postDelayed(this, MINUTE - now % MINUTE + 200)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Any touch ends the screen saver, as with Android's own clock.
        isInteractive = false
        isFullscreen = true
        isScreenBright = false

        clock = TextView(this).apply {
            textSize = 72f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(GREY)
        }
        date = TextView(this).apply {
            textSize = 18f
            setTextColor(DIM)
        }
        card = TextView(this).apply {
            textSize = 34f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(BRIGHT)
            setPadding(0, dp(28), 0, 0)
        }
        next = TextView(this).apply {
            textSize = 18f
            setTextColor(DIM)
            setPadding(0, dp(28), 0, 0)
        }
        block = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            listOf(clock, date, card, next).forEach { view ->
                view.gravity = Gravity.CENTER_HORIZONTAL
                addView(view)
            }
        }
        root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                block,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
        setContentView(root)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        handler.removeCallbacks(tick)
        tick.run()
    }

    override fun onDreamingStopped() {
        handler.removeCallbacks(tick)
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    private fun update() {
        val now = System.currentTimeMillis()
        val phoneTz = TimeZone.getDefault()
        clock.text = Engine.time(this, Date(now), phoneTz, false)
        date.text = Engine.date(Date(now), phoneTz)

        val prefs = Prefs(this)
        val shown = try {
            Engine.cardText(this, prefs, now)
        } catch (e: Exception) {
            null
        }
        if (shown != null) {
            card.text = shown
            card.visibility = View.VISIBLE
            next.visibility = View.GONE
        } else {
            // Between Shabbosim: the next times, quietly.
            card.visibility = View.GONE
            val span = try {
                ShabbosCalc.upcoming(prefs.place, Date(now), 1, prefs.candleMinutes).firstOrNull()
            } catch (e: Exception) {
                null
            }
            if (span == null) {
                next.visibility = View.GONE
            } else {
                val tz = prefs.place.timeZone
                next.text = "Candle lighting ${Engine.day(span.candleLighting, tz)} " +
                    Engine.time(this, span.candleLighting, tz, false)
                next.visibility = View.VISIBLE
            }
        }
        // Wait for the new text to be measured, then drift.
        block.post { drift() }
    }

    /** Puts the block somewhere new on the screen, so no pixel stays lit for long. */
    private fun drift() {
        val roomX = (root.width - block.width).coerceAtLeast(0)
        val roomY = (root.height - block.height).coerceAtLeast(0)
        block.translationX = if (roomX > 0) Random.nextInt(roomX).toFloat() else 0f
        block.translationY = if (roomY > 0) Random.nextInt(roomY).toFloat() else 0f
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val MINUTE = 60_000L
        // Kept below full white: dimmer on the eye and gentler on an OLED screen.
        val BRIGHT = Color.rgb(0xD0, 0xD0, 0xD0)
        val GREY = Color.rgb(0xA8, 0xA8, 0xA8)
        val DIM = Color.rgb(0x70, 0x70, 0x70)
    }
}
