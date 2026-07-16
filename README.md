# Verses

A focused Bible reading tool for the Light Phone III, built with the official [Light SDK](https://github.com/lightphone/light-sdk).

---

<img src="assets/images/example.png" alt="Verses app screenshots">

## Features

* Engage with God's word with the Verse of the Day
* Full access to any verse or passage in the Bible
* Memorize: hide a few random words at a time, with forward/back buttons to gradually hide/reveal the text
* Multiple translations supported, with more on the way
* Add notes to any verse or passage, and reference them anytime


---

## Translations
*If you have a specific version you'd like to see in Verses, [open an issue](https://github.com/zacksimpson/verses-tool/issues?q=state%3Aopen%20label%3ATranslations) with the request and I will gladly look into it!*

**Currently Supported:**
* English Standard Version (ESV)
* New International Version (NIV)
* New American Standard Bible (NASB)
* King James Version (KJV)
* Berean Standard Bible (BSB)
* (More to come!)

### About Fallback Translations
Most Bible translations (eg ESV, NIV, NASB, etc) contain inherent copyright limitations or rules around their API. KJV and BSB are exceptions to this, because they are both in the public domain. Because of that, verse and passage lookups default to KJV, so browsing around doesn't eat into the shared API budget the copyrighted translations share.

> [!NOTE]
> **You're always welcome to change the Fallback Translation!** Head to **Settings → Fallback Translation** and pick whichever translation you'd like lookups to use instead.

At this time, to save on expenses, Verses uses a shared API as I determine the demand for the tool – so we're all contributing to the same rate limits :) to help with this, there is a soft per-device daily limit on lookups for copyrighted translations to honor their respective API limitations. In the unlikely event a user hits their device limit, lookups for that translation pause until the next day. You can switch to a public domain translation anytime in Settings to keep going.

---

## Installing on Light Phone III
* Highly recommend using [Obtainium](https://github.com/ImranR98/Obtainium) to ensure you receive future updates and new features automatically. Just add [the repo URL](https://github.com/zacksimpson/verses-tool/), make sure you're able to install apps from unknown sources, and you're all set.
* Alternatively, you can download the latest APK from the Releases tab.
* Once the official Light SDK matures and LightOS enables support for installing APKs from the Dashboard, installing there will eventually be an option as well – but not for now.
<details>
<summary><strong>Building from Source</strong></summary>

**ESV**

1. Get a free ESV API key at https://api.esv.org (sign up, create an API application).
2. Add it to this repo's root `local.properties` (gitignored, create the file if it
   doesn't exist):

   ```
   esvApiKey=your_key_here
   ```

**YouVersion Platform (NIV, NASB)**

1. Request an app key at https://developers.youversion.com (sign up, create an app).
2. Add it to the same `local.properties` file:

   ```
   youVersionAppKey=your_key_here
   ```

**Build**

Open in Android Studio, or build from the command line:

```
./gradlew :verses:assembleDebug
```

Without a key, the tool shows a message asking you to add one instead of making a
network call. Run on the [LightOS emulator](docs/system_app) for full compatibility,
or any Android emulator/device for everyday development – see `docs/` (inherited from
light-sdk) for details.

</details>

---

## Support

If any of my tools have been useful to you, I'd love to hear from you! Feel free to [email me](mailto:zacksimpson24@gmail.com), or [open an issue](https://github.com/zacksimpson/verses-tool/issues/) with your feedback.

---

## Credits

* [The Light Phone](https://www.thelightphone.com) – for building a phone worth making tools for
* [Crossway](https://www.crossway.org) – for the ESV® Bible text, made available via the [ESV API](https://api.esv.org)
* [YouVersion Platform](https://platform.youversion.com) – for the NIV® and NASB® Bible translations, made available via the [YouVersion API](https://developers.youversion.com/api/data-exchange)
