# `:sdk:client`

The client library that every Light tool compiles against. It re-maps/simplifies the default Android app lifecycles, hands you a Compose-friendly screen/view-model framework, and brokers communication with LightOS (or the [emulator](../emulator)) over the SDK service binding.


## Building blocks

### Screens

A screen is a piece of UI plus its lifecycle hooks. Most tools subclass `LightScreen<R, VM>`, which pairs a `SimpleLightScreen<R>` with a `LightViewModel<R>`:

```kotlin
@InitialScreen
class HomeScreen(activity: SealedLightActivity)
    : LightScreen<Unit, HomeScreenViewModel>(activity) {

    override val viewModelClass = HomeScreenViewModel::class.java
    override fun createViewModel() = HomeScreenViewModel(fileShare)

    @Composable
    override fun Content() {
        // your Compose UI
    }
}
```

- `R` is the *result type* the screen can return to whoever opened it (`Unit` if it doesn't return anything).
- The class annotated with `@InitialScreen` is the boot screen. The SDK scans for it at startup; excluding it (or having more than one) will fail the build.
- `SimpleLightScreen` is the no-view-model variant if you don't need one.
- Override `willShow`, `willHide`, `onAppPause`, `onScreenDestroy` for lifecycle hooks.

### View models

Model-View-ViewModel architecture is relatively popular for standard Android application development, so we included some classes that wrap standard Android MVVM APIs. You do not have to use them! Have your tool's Screens extend `SimpleLightScreen` if you want to avoid MVVM. Otherwise extend `LightScreen` and specify your `LightViewModel` class.

`LightViewModel<R>` extends [`androidx.lifecycle.ViewModel`](https://developer.android.com/topic/libraries/architecture/viewmodel) and adds Light-specific hooks:

```kotlin
class HomeScreenViewModel(private val fileShare: LightFileShare)
    : LightViewModel<Unit>() {

    val items = MutableStateFlow<List<String>>(emptyList())

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        items.value = fileShare.list("ringtones")
    }

    override fun onBackPressed(): Boolean = false  // true to consume the back press
}
```

### Navigation

From any screen:

```kotlin
navigateTo(::DetailScreen) { result ->
    // called when DetailScreen.goBack(result) fires
}
```

`navigateTo` takes a `(SealedLightActivity) -> SimpleLightScreen<R>` factory and an optional result callback. To return a value, call `goBack(result)` on the child screen.

The SDK manages its own back stack and renders a back bar at the bottom of the screen. The system back gesture is wired to the same logic; you don't need to handle it yourself.

### Per-screen storage and files

Every screen gets:

- `dataStore: DataStore<Preferences>` — a shared Preferences DataStore (named `DEFAULT_DATASTORE`) for the whole tool.
- `filesDir: File` — the standard app private files directory.
- `fileShare: LightFileShare` — files written here can be read by LightOS via a content provider (e.g., ringtones, wallpapers).

### Talking to LightOS

`callRemoteServiceMethod(method, payload)` sends a typed request to the LightOS server (or to `:sdk:emulator` in dev) and returns a `LightResult<Response>`. The set of available methods lives in `:sdk:shared`'s `LightServiceMethod`. Example:

```kotlin
val result = callRemoteServiceMethod(
    LightServiceMethod.SetRingtone,
    LightServiceMethod.SetRingtone.Request(type = 1, uri = uri),
)
result.error?.let { Log.e(TAG, "code=${it.code}") }
```

### Tool entry point (optional)

If your tool needs to do work outside the scope of a specific screen, write a Kotlin `object` that implements `LightEntryPoint` and annotate the class with `@EntryPoint`:

```kotlin
@EntryPoint
object ToolEntryPoint : LightEntryPoint {
    override suspend fun onToolCreate(serverData: StateFlow<LightServerData?>) {
        serverData.collect { /* observe push credentials, etc. */ }
    }

    // if your app is registered to handle push notifications, they'll all come in here
    override suspend fun onPushNotification(data: ByteArray) { /* ... */ }
}
```

`onToolCreate` is called once from the SDK `Application`. `onPushNotification` is dispatched when UnifiedPush delivers a message via `LightPushService`.

### Background jobs

`LightWork` lets you run code in the background. These jobs can run even when your tool isn't on screen, and across reboots. The system decides when to run them (it may wait until the device is idle, charging, or on Wi-Fi), so they aren't great for anything time-sensitive.

Declare a job as a top-level `val` annotated with `@LightJob`. The key you pass is the string you'll use to refer to the job elsewhere — it must be unique within your tool:

```kotlin
@LightJob("sync-contacts")
val syncContacts: LightJobHandler = { ctx, input ->
    // ...do the work...
    LightJobResult.Success()
}
```

The handler receives a `SealedLightContext` (use it for DataStore, files, etc.) and an input bundle. Return one of:

- `LightJobResult.Success(outputData)` — finished cleanly. `outputData` is optional and made available to any code watching the job.
- `LightJobResult.Retry` — something transient went wrong (a flaky network, etc.); the system will try again later, waiting longer between each attempt.
- `LightJobResult.Error(outputData)` — failed permanently; don't run again.

Schedule it from any screen:

```kotlin
LightWork.enqueue(lightContext, "sync-contacts", mapOf("force" to "true"))
```

The `inputData` map is forwarded straight to the handler. Values must be strings — encode richer types yourself.

For repeating work, use `enqueuePeriodic` with an interval. The minimum is 15 minutes; shorter intervals are rounded up:

```kotlin
LightWork.enqueuePeriodic(lightContext, "sync-contacts", repeatInterval = 30.minutes)
```

Cancel a scheduled or running job by key:

```kotlin
LightWork.cancel(lightContext, "sync-contacts")
```

Watch state changes (great inside a `LaunchedEffect` or view model):

```kotlin
LightWork.observe(lightContext, "sync-contacts").collect { state ->
    when (state) {
        LightJobState.Running -> // show a spinner
        is LightJobState.Succeeded -> // state.outputData is what the handler returned
        is LightJobState.Failed -> // ditto
        else -> Unit
    }
}
```

For a one-shot read use `LightWork.getState(...)`; to suspend until the job reaches a terminal state use `LightWork.awaitCompletion(...)`.

If you need multiple concurrent runs of the same job, pass a `tag` to `enqueue` / `enqueuePeriodic` and use that same `tag` with `cancel` / `observe`.

### Push notifications

// TODO

## Restricted dependencies

This module is wired up with the [`:plugin`](../../plugin) build plugin, which restricts which third-party libraries can appear on your tool's classpath. If you try to add a dependency that isn't allow-listed, Gradle will fail at configuration time. See [`LightSdkPlugin.kt`](../../plugin/src/main/kotlin/com/thelightphone/plugin/LightSdkPlugin.kt) for the current allow-list, and the [top-level README](../../README.md) for why this exists.

## Related

- [`:tool`](../../tool) — the scaffold module you actually edit when building a tool.
- [`:sdk:ui`](../ui) — Compose components and theme tokens (`LightText`, `LightTheme`, etc.).
- [`:sdk:shared`](../shared) — constants and serializable data models shared with `:sdk:server`.
- [`:sdk:server`](../server) — the LightOS side of the connection that `:sdk:client` talks to.
