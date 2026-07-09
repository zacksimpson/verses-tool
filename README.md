# Verses

A daily Bible verse tool for the Light Phone III, built with the official [Light SDK](https://github.com/lightphone/light-sdk).

Shows one verse a day, refreshed automatically each morning. Look up the verse for any past date, jot a note against any verse, and spend focused time memorizing God's word.

---

## Features

* One Bible verse a day, cycling through a bundled reference list by day-of-year
* Look up the verse for any past date
* Add free-text notes to any verse, and edit them anytime
* All your notes in one place, most recent first
* Memorize mode: hide a few random words at a time, with forward/back buttons to gradually hide/reveal the text

---

## Translations
*If you have a specific version you'd like to see in Verses, [open an issue](https://github.com/zacksimpson/verses-tool/issues?q=state%3Aopen%20label%3ATranslations) with the request and I will gladly look into it!*

**Currently Supported:**
* English Standard Version
* (More to come!)

---

## Installing on Light Phone III

Not yet published – no APK release exists yet. In the meantime, build from source:

1. Get a free ESV API key at https://api.esv.org (sign up, create an API application).
2. Add it to this repo's root `local.properties` (gitignored, create the file if it
   doesn't exist):

   ```
   esvApiKey=your_key_here
   ```

3. Open in Android Studio, or build from the command line:

   ```
   ./gradlew :verses:assembleDebug
   ```

Without a key, the tool shows a message asking you to add one instead of making a
network call. Run on the [LightOS emulator](docs/system_app) for full compatibility,
or any Android emulator/device for everyday development – see `docs/` (inherited from
light-sdk) for details.

---

## Support

If any of my tools have been useful to you, I'd love to hear from you! Feel free to reach out [here](mailto:zacksimpson24@gmail.com).

---

## Credits

* [The Light Phone](https://www.thelightphone.com) – for building a phone worth making apps for
* [Crossway](https://www.crossway.org) – for the ESV® Bible text, made available via the [ESV API](https://api.esv.org)
