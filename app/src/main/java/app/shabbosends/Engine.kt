package app.shabbosends

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Decides what should be on screen right now, puts it there, and sets one
 * alarm for the next moment something changes. Everything runs offline.
 */
object Engine {
    const val ACTION_SYNC = "app.shabbosends.SYNC"

    /** Samsung Routines watches for these titles. None is contained in another. */
    const val START_TITLE = "Shabbos mode starting"
    const val END_TITLE = "Shabbos mode ending"
    const val FOLLOW_TITLE = "Shabbos mode follow-up"

    // A new id, because Android won't let an app change an existing channel's importance.
    private const val CARD_CHANNEL = "shabbos_card"
    private const val OLD_CARD_CHANNEL = "shabbos_ends"
    private const val TRIGGER_CHANNEL = "routine_triggers"
    private const val CARD_ID = 1
    private const val TEST_ID = 2
    private const val START_ID = 3
    private const val END_ID = 4
    private const val FOLLOW_ID = 5
    private const val MINUTE = 60_000L
    private const val DAY = 24 * 60 * MINUTE

    fun sync(context: Context) {
        val prefs = Prefs(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        ensureChannels(nm)

        val now = System.currentTimeMillis()
        // Look back a week so a Shabbos that has just ended is still in the list,
        // even if the phone was switched off when it ended.
        val spans = ShabbosCalc.upcoming(prefs.place, Date(now - 7 * DAY), 5, prefs.candleMinutes)

        var next = Long.MAX_VALUE
        fun wakeAt(time: Long) {
            if (time > now && time < next) next = time
        }

        // The card.
        val current = spans.firstOrNull { it.ends.time > now }
        if (current == null) {
            nm.cancel(CARD_ID)
        } else {
            val place = prefs.place
            val life = current.ends.time - now
            val showAt = showAt(prefs, current)
            val candleAt = candleAt(prefs, current)
            when {
                now >= showAt -> nm.notify(CARD_ID, buildCard(context, place, current, life))
                // Same id as the end card, so the changeover updates it in place.
                now >= candleAt -> nm.notify(CARD_ID, buildCard(context, place, current, life, candle = true))
                else -> nm.cancel(CARD_ID)
            }
            if (candleAt != Long.MAX_VALUE) wakeAt(candleAt)
            wakeAt(showAt)
            wakeAt(current.ends.time + 1000)
        }

        // Shabbos automation, each step done once per Shabbos or Yom Tov.
        if (prefs.routinesEnabled || prefs.dndEnabled || prefs.rotationEnabled) {
            for (span in spans) {
                val id = span.ends.time
                val startAt = span.candleLighting.time - prefs.modeOnMinutes * MINUTE
                val endAt = span.ends.time + prefs.modeOffMinutes * MINUTE
                if (now >= startAt && now < span.ends.time && prefs.lastOn != id) {
                    startMode(context, prefs)
                    prefs.lastOn = id
                }
                // Only undo a mode this app switched on.
                if (now >= endAt && prefs.lastOn == id && prefs.lastOff != id) {
                    endMode(context, prefs)
                    prefs.lastOff = id
                }
                wakeAt(startAt)
                wakeAt(endAt)

                // Optional later signal, for things to do once Shabbos is well over.
                if (prefs.routinesEnabled && prefs.followEnabled) {
                    val followAt = span.ends.time + prefs.followMinutes * MINUTE
                    if (now >= followAt && prefs.lastOn == id && prefs.lastFollow != id) {
                        postTrigger(context, FOLLOW_TITLE)
                        prefs.lastFollow = id
                    }
                    wakeAt(followAt)
                }
            }
        }

        if (next != Long.MAX_VALUE) schedule(context, next)
    }

    /** When the "ends" card goes up for a span. */
    private fun showAt(prefs: Prefs, span: Span): Long =
        span.candleLighting.time - prefs.leadMinutes * MINUTE

    /** Optional: from a chosen time on the eve, the same card shows candle lighting first. */
    private fun candleAt(prefs: Prefs, span: Span): Long =
        if (prefs.candleCardEnabled) {
            timeOnDayOf(span.candleLighting, prefs.place.timeZone, prefs.candleCardMinutes)
        } else {
            Long.MAX_VALUE
        }

    /**
     * The words on the card at this moment, or null when no card is up.
     * The screen saver uses this, so it always agrees with the card.
     */
    fun cardText(context: Context, prefs: Prefs, now: Long = System.currentTimeMillis()): String? {
        val spans = ShabbosCalc.upcoming(prefs.place, Date(now - 7 * DAY), 5, prefs.candleMinutes)
        val current = spans.firstOrNull { it.ends.time > now } ?: return null
        return when {
            now >= showAt(prefs, current) -> cardTitle(context, prefs.place, current, candle = false)
            now >= candleAt(prefs, current) -> cardTitle(context, prefs.place, current, candle = true)
            else -> null
        }
    }

    /** "Candle lighting 4:16", "Shabbos ends 5:22" or "Yom Tov ends Sun 8:28". */
    fun cardTitle(context: Context, place: Place, span: Span, candle: Boolean): String {
        val tz = place.timeZone
        // Name the day whenever it is not simply "tomorrow night".
        val endDay = if (span.days > 1 || !span.endsOnShabbos) day(span.ends, tz) + " " else ""
        return if (candle) {
            "Candle lighting ${time(context, span.candleLighting, tz, false)}"
        } else {
            "${span.label} $endDay${time(context, span.ends, tz, false)}"
        }
    }

    /** Puts the phone into Shabbos mode: the built-in actions, and the Samsung signal if used. */
    fun startMode(context: Context, prefs: Prefs) {
        if (prefs.routinesEnabled) postTrigger(context, START_TITLE)
        PhoneMode.start(context, prefs)
    }

    /** Takes the phone back out of Shabbos mode. */
    fun endMode(context: Context, prefs: Prefs) {
        if (prefs.routinesEnabled) postTrigger(context, END_TITLE)
        PhoneMode.end(context, prefs)
    }

    /** Posts the next card for five minutes, to see how the phone displays it. */
    fun postTest(context: Context, candle: Boolean = false): Boolean {
        val prefs = Prefs(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        ensureChannels(nm)
        val span = ShabbosCalc.upcoming(prefs.place, Date(), 1, prefs.candleMinutes).firstOrNull()
            ?: return false
        nm.notify(TEST_ID, buildCard(context, prefs.place, span, 5 * MINUTE, candle))
        return true
    }

    /** A short-lived, silent notification whose only job is to set off a Samsung routine. */
    fun postTrigger(context: Context, title: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        ensureChannels(nm)
        val notification = Notification.Builder(context, TRIGGER_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_stars)
            .setContentTitle(title)
            .setContentText("Signal for Samsung Routines")
            .setShowWhen(false)
            .setTimeoutAfter(2 * MINUTE)
            .build()
        val id = when (title) {
            START_TITLE -> START_ID
            END_TITLE -> END_ID
            else -> FOLLOW_ID
        }
        nm.notify(id, notification)
    }

    fun buildCard(
        context: Context,
        place: Place,
        span: Span,
        lifetimeMillis: Long,
        candle: Boolean = false,
    ): Notification {
        val tz = place.timeZone
        // One short line and nothing else, so the Now Bar shows all of it.
        val shown = if (candle) span.candleLighting else span.ends
        val title = cardTitle(context, place, span, candle)

        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(context, CARD_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_stars)
            .setContentTitle(title)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_EVENT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setTimeoutAfter(lifetimeMillis.coerceAtLeast(MINUTE))
            // These two make it an Android 16 Live Update. The promotion request
            // only became a named method in API 36.1, so it is set by its key.
            .setShortCriticalText(time(context, shown, tz, false))
            .addExtras(Bundle().apply { putBoolean("android.requestPromotedOngoing", true) })
            .build()
    }

    private fun ensureChannels(nm: NotificationManager) {
        // Normal importance, so Android ranks the card with ordinary notifications
        // and not in the "silent" group below them. Sound and vibration are
        // switched off on the channel itself, so it never buzzes or rings.
        val isNew = nm.getNotificationChannel(CARD_CHANNEL) == null
        val card = NotificationChannel(CARD_CHANNEL, "Shabbos card", NotificationManager.IMPORTANCE_DEFAULT)
        card.description = "The card showing candle lighting and when Shabbos or Yom Tov ends"
        card.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        card.setSound(null, null)
        card.enableVibration(false)
        card.enableLights(false)
        card.setShowBadge(false)
        // Keeps the card visible while Do Not Disturb is on. Android only honours
        // this once the app has Do Not Disturb access, so it is re-applied each time.
        card.setBypassDnd(true)
        nm.createNotificationChannel(card)
        if (isNew) nm.deleteNotificationChannel(OLD_CARD_CHANNEL)
        if (nm.getNotificationChannel(TRIGGER_CHANNEL) == null) {
            val trigger = NotificationChannel(
                TRIGGER_CHANNEL, "Routine triggers", NotificationManager.IMPORTANCE_LOW,
            )
            trigger.description = "Brief signals that start and stop a Samsung routine"
            trigger.setShowBadge(false)
            nm.createNotificationChannel(trigger)
        }
    }

    /** A clock time, in the place's own time zone, on the day candles are lit. */
    private fun timeOnDayOf(date: Date, tz: TimeZone, minutesAfterMidnight: Int): Long =
        Calendar.getInstance(tz).apply {
            time = date
            set(Calendar.HOUR_OF_DAY, minutesAfterMidnight / 60)
            set(Calendar.MINUTE, minutesAfterMidnight % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun schedule(context: Context, atMillis: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, AlarmReceiver::class.java).setAction(ACTION_SYNC),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    fun time(context: Context, date: Date, tz: TimeZone, amPm: Boolean): String {
        val pattern = when {
            DateFormat.is24HourFormat(context) -> "HH:mm"
            amPm -> "h:mm a"
            else -> "h:mm"
        }
        return SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = tz }.format(date)
    }

    fun day(date: Date, tz: TimeZone): String =
        SimpleDateFormat("EEE", Locale.getDefault()).apply { timeZone = tz }.format(date)

    fun date(date: Date, tz: TimeZone): String =
        SimpleDateFormat("EEE d MMM", Locale.getDefault()).apply { timeZone = tz }.format(date)
}
