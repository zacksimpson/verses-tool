# verses-tool

A "Verses" tool for the Light Phone III — shows one Bible verse per day (ESV translation),
built with the official [Light SDK](https://github.com/lightphone/light-sdk).

This repo is a fork of light-sdk's scaffolding (`sdk/`, `plugin/`, `lint-rules/`, etc.) with
the placeholder tool replaced by `verses/`, per Light's documented workflow for building
tools as of their July 2026 SDK preview.

## What it does

Picks a verse reference from a bundled list (`verses/.../VerseCatalog.kt`), cycling by
day-of-year, and fetches the passage text live from the official ESV API
(https://api.esv.org). No push notifications — it refreshes its cache silently
whenever the tool is opened, if the date has rolled over since the last fetch.

## Setup

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
network call.

## Testing

Run on the [LightOS emulator](docs/system_app) for full compatibility (push/permissions),
or any Android emulator/device for everyday development — see `docs/` (inherited from
light-sdk) for details.
