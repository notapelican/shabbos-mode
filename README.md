# Shabbos Mode

An Android app that puts the time Shabbos or Yom Tov ends on your lock screen
and Always On Display, so you can see it all day without touching the phone.
It can also switch the phone into Shabbos mode and back at the right times.

It works with the phone in flight mode. Built and tested on a Samsung Galaxy;
other Android 16 phones should work but are untested.

## What it does

- 🌙 **Shows when Shabbos ends.** A small card reading, for example,
  "Shabbos ends 5:17" appears at candle lighting and removes itself when
  Shabbos is over. On a Samsung it sits in the Now Bar.
- 🕯️ **Shows candle lighting first, if you like.** On Friday the card can start
  earlier, reading "Candle lighting 4:16", then change to the end time.
- 📅 **Covers Yom Tov.** A two-day Yom Tov, or Yom Tov running into Shabbos, is
  one card showing the final end time and its day, such as
  "Shabbos ends Sat 8:28".
- 🔕 **Stays quiet.** Nothing the app shows makes a sound or vibrates.
- 🔁 **Looks after itself.** Once set up it runs every week, and carries on after
  the phone restarts.
- ✈️ **Can switch your phone into Shabbos mode.** Optionally, it turns on
  Do Not Disturb, locks the screen orientation, or tells Samsung Routines when
  to switch on flight mode.

## Getting started

You need a phone running Android 16 or later.

1. Download the APK from this repo's **Releases** page and open it to
   install. Your phone will ask you to allow installing from the browser.
2. Open the app. The **Setup** box lists anything still to do, each with its
   own button. Work through it until it says "Ready".
3. Under **Location**, search for your suburb, town, address or postcode and
   tap the right result. The place and its time zone then show in that box.
4. Tap **Preview the card for 5 minutes**, lock the phone, and check it shows
   on the lock screen and Always On Display.

That's all. The card now appears by itself each week.

### If the preview doesn't show properly

- 🔔 **It only appears as an ordinary notification (Samsung):** turn on
  Settings > Developer options > **Live notifications for all apps**. If you
  can't see Developer options, go to Settings > About phone > Software
  information and tap **Build number** seven times.
- 📱 **It's missing from the Always On Display:** set the Always On Display to
  show **Always**, not "Tap to show".
- 🧹 **Something else is in the Now Bar (Samsung):** in Settings > Lock screen and
  AOD > Now bar, switch off the items you don't want competing with it.

## If power saving turns off your Always On Display

Some phones, Pixels among them, switch the Always On Display off when battery
saver is on. For those, the app has a screen saver: a dim black screen showing
the clock and the same words as the card, such as "Shabbos ends 5:17". Between
Shabbosim it shows the next candle lighting instead. It moves a little each
minute so nothing burns into the screen.

1. In the app, open **Optional: screen saver** and tap
   **Open screen saver settings** (or go to Settings > Display > Screen saver).
2. Choose **Shabbos times**, and set it to start **while charging**.
3. Leave the phone plugged in over Shabbos.

Android only runs screen savers while the phone is charging or docked, and
touching the screen ends it, just as with the phone's own clock screen saver.
Not every phone has screen saver settings.

## Travelling

The location is set by hand, because the phone may be offline on Shabbos.
When you travel, search for the new place before Shabbos. Search needs an
internet connection; nothing else in the app does.

If you'd rather type coordinates, tap **Enter coordinates by hand** in the
Location box. The **In Israel** option there gives one day of Yom Tov.

## Settings

All under **Timing** in the app. Changes save as you type.

| Setting | What it does | Default |
|---|---|---|
| Show "Shabbos ends" this many minutes before candle lighting | Brings the card forward. 0 means it appears at candle lighting. | 0 |
| Candle lighting, minutes before sunset | Change this if your community lights earlier or later. | 18 |
| Show candle lighting first | Shows "Candle lighting 4:16" on Friday before the end time takes over. | Off |
| Candle lighting card appears at | The time on Friday that card first shows. Only there when the option above is on. | 12:00 pm |

To check the times against your own calendar, tap **Coming up** at the bottom
of the app. It opens a list of the next several weeks.

## Optional: Shabbos automation

This is only for people who set their phone up for Shabbos automatically.
Leave it all off if that isn't you; the card works without it.

Everything here happens at the same two moments: a set time before candle
lighting, and a set time after Shabbos or Yom Tov ends. Both are adjustable,
and both default to 10 minutes. Over a two-day Yom Tov the phone stays in
Shabbos mode the whole time.

The options are in two submenus. The one for your phone starts open.

### Pixel and other phones

Done by the app itself. These work on a Samsung too.

- 🤫 **Do Not Disturb during Shabbos.** The app asks for Do Not Disturb access
  when you switch this on. Afterwards it only turns Do Not Disturb off if it
  was the one that turned it on. To change who can still get through, look for
  the app's name under Settings > Modes.
- 🔒 **Lock the screen orientation during Shabbos.** Choose portrait, landscape
  or flipped landscape. The app asks for permission to change system settings.
  Your phone's own setting is put back afterwards.

### Samsung

An app can't switch on flight mode, but Samsung Routines can, and this app can
tell it when.

1. In the app, turn on **Signal Samsung Routines**.
2. In Samsung's Modes and Routines, make a routine. For **If**, choose
   Notification received, pick Shabbos Mode, then Notification with specific
   keyword, and type `Shabbos mode starting`. For **Then**, add flight mode on
   and anything else you want.
3. Make a second routine the same way with the keyword `Shabbos mode ending`,
   and have it put everything back.

There is also an optional third signal, **Send a later follow-up signal**,
sent a while after Shabbos ends (30 minutes by default). Its keyword is
`Shabbos mode follow-up`. Use it for anything that should happen later than
the end routine, such as turning the Always On Display off.

Give each routine only the notification as its trigger, with no time
condition, and switch off any older Shabbos schedule so the two don't clash.

### Trying it out

The **Try it** buttons start and end Shabbos mode on the spot, so you can
check your setup without waiting for Friday.

## Good to know

- ⛔ Don't force-stop the app in Settings. That cancels its weekly alarm until
  you next open it.
- 🔋 Leave notifications allowed and battery restrictions off for the app. The
  Setup box tells you if either changes.
- ⏱️ The card shows the final end time only. It doesn't show candle lighting for
  the second night of Yom Tov.
- 🚫 Fast days other than Yom Kippur aren't covered.

---

## Technical notes

### How the times are calculated

- 🕯️ Candle lighting: sea-level sunset minus 18 minutes (adjustable).
- 🌙 Shabbos or Yom Tov ends: sun 8.5 degrees below the horizon.
- ⏱️ Both are rounded to the nearest minute.

These were checked against chabad.org for Crown Heights and Bondi and matched
to the minute. Calculation is by the KosherJava zmanim library and runs
entirely on the phone. The logic is in `ShabbosCalc.kt`, which has no Android
code in it.

### How the card works

The card is an Android 16 Live Update: an ongoing notification that asks the
system to promote it. Samsung shows promoted notifications in the Now Bar;
stock Android shows them on the lock screen, Always On Display and as a status
bar chip. Its channel has normal importance with sound and vibration off, and
is marked to show through Do Not Disturb.

One exact alarm is set for the next moment anything changes. It is reset after
a reboot, a clock or time zone change, and an app update.

### How the screen saver works

It is an Android `DreamService` (`ShabbosDream.kt`). Each minute it asks the
same code that builds the card what the card would say right now, so the two
always agree. It asks for a dim screen and moves its text to a random spot
each minute.

### How the Samsung signals work

Each signal is a silent notification on its own channel that removes itself
after two minutes. Samsung Routines matches on the title. The end and
follow-up signals are only sent for a Shabbos or Yom Tov the app sent a start
signal for.

### Place search

Search uses OpenStreetMap's Nominatim service. The time zone for the chosen
place comes from Open-Meteo. If OpenStreetMap can't be reached, search falls
back to Open-Meteo's own place list, which is built on GeoNames.

### Building

Every push to `main` runs the **Build APK** workflow in GitHub Actions, which
builds the app and publishes the APK as a release called "Latest build",
replacing the previous one. The APK file is still named `shabbos-ends.apk`,
and the app's internal ID is still `app.shabbosends`, both from the app's
earlier name. Keeping the ID is what lets an update install over the old app.

Builds are signed with the key in `app/sideload.jks`, so a new APK installs
over the old one and keeps its settings. Because that key is in the repo, keep
the repo private.

To change the app through the GitHub website, use **Add file > Upload files**
and drag in the `app` folder as a single item, so the file paths are kept.

Requirements: Android 16 (API 36) or later. Built with Gradle 8.14.3, the
Android Gradle Plugin 8.13.0 and Kotlin 2.2.20.
