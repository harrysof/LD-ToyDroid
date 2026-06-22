# LD-ToyDroid

> **LD ToyPad Remote** — A native Android application that remotely controls the [LD-ToyPad-Emulator](https://github.com/Berny23/LD-ToyPad-Emulator) server over a local network.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Current Status](#current-status)
- [API Protocol](#api-protocol)
- [Domain Models](#domain-models)
- [Build Configuration](#build-configuration)
- [Development Roadmap](#development-roadmap)
- [Potential Issues](#potential-issues)
- [License](#license)

---

## Overview

LD-ToyDroid is a native Android client that replaces the emulator's browser-based interface. It does **not** emulate USB, communicate directly with RPCS3/Cemu, or implement the LEGO Dimensions USB protocol. The existing Node.js server remains responsible for:

- USB Toy Pad emulation
- Communication with the game
- Creating token UIDs
- Persisting vehicle upgrades
- Maintaining the authoritative list of created tokens

The Android app is responsible for:

- Connecting to the Node.js server over LAN via HTTP and Socket.IO
- Showing the seven Toy Pad positions across three physical zones
- Creating, placing, moving, and removing tokens
- Searching and filtering the character/vehicle catalogues
- Displaying Toy Pad light effects from the game
- Saving app-local favorites, recent items, connection profiles, and presets

---

## Architecture

The project follows **Clean Architecture** with a clear separation between remote DTOs, database entities, domain models, repositories, and UI.

```
app/src/main/java/com/ldtoypad/remote/
├── core/              # Result types, error handling
├── data/
│   ├── remote/        # HTTP client, Socket.IO client, DTOs
│   ├── local/         # Room database, DataStore, DAOs, entities
│   └── repository/    # Connection, Catalogue, ToyPad, Preset repositories
├── domain/
│   ├── model/         # Domain models (PadSlot, PadZone, RemoteToken, etc.)
│   └── usecase/       # Business logic use cases
├── feature/           # Jetpack Compose UI screens + ViewModels
│   ├── connection/    # Server connection screen
│   ├── pad/           # Toy Pad display screen
│   └── library/       # Character/vehicle catalogue screen
└── theme/             # Material 3 theming
```

**Dependency Injection**: Manual via `AppContainer` (no Hilt/Dagger).

---

## Tech Stack

| Layer | Technology |
|---|---|
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | Single-activity, MVVM with `StateFlow` |
| **Networking** | OkHttp + Kotlin Serialization + Socket.IO Java client |
| **Local Data** | Room (presets, favorites, recents) + DataStore (settings) |
| **Images** | Coil Compose |
| **Navigation** | Navigation Compose |
| **Min SDK** | API 24 |
| **Target/Compile SDK** | API 36 |
| **Build** | Gradle Kotlin DSL with version catalogs |

---

## Project Structure

### Key Files

| File | Purpose |
|---|---|
| `ANDROID_APP_IMPLEMENTATION_PLAN.md` | 40KB detailed specification covering API protocol, domain models, UI spec, testing plan, and 6 build milestones |
| `App.kt` | Application class with `AppContainer` (manual DI) |
| `MainActivity.kt` | Single activity hosting Compose `NavHost` |
| `EmulatorHttpClient.kt` | HTTP client for catalogue and token operations |
| `EmulatorSocketClient.kt` | Socket.IO client for live events |
| `ToyPadRepository.kt` | Core business logic for token mutations with state confirmation |
| `ConnectionRepository.kt` | Server connection lifecycle management |

---

## Current Status

### ✅ Implemented

- **Project bootstrap** (Milestone 0): Full project structure, Gradle config, dependencies
- **Core domain models**: `PadSlot`, `PadZone`, `RemoteToken`, `ToyPadState`, `ConnectionState`, `LightState`, `CatalogueItem`
- **Data layer skeleton**: HTTP client, Socket.IO client, Room database, DataStore
- **Use cases**: `ConnectToServer`, `CreateToken`, `PlaceToken`, `RemoveToken`, `MoveToken`, `DeleteToken`, `LoadPreset`, `SavePreset`
- **UI screens**: Connection, Toy Pad, Library
- **Navigation**: 3-screen flow (Connection → Pad → Library)

### ⚠️ Partially Implemented

- Socket.IO event handling (lighting, refresh)
- Mutation serialization mutex/queue
- State confirmation polling
- Local persistence wiring
- Test coverage

### ❌ Not Yet Implemented

- Presets screen UI
- Settings screen
- mDNS/NSD server discovery
- Full light animation rendering
- Comprehensive error handling
- Accessibility pass
- Release build optimization

---

## API Protocol

The server normally listens on HTTP port `80`. The client connects to the same scheme, host, and port for both HTTP and Socket.IO.

### HTTP Endpoints

| Endpoint | Method | Description |
|---|---|---|
| `/json/charactermap.json` | `GET` | Character catalogue (86 entries) |
| `/json/tokenmap.json` | `GET` | Vehicle catalogue (266 entries) |
| `/json/toytags.json` | `GET` | All created tokens and pad state |
| `/character` | `POST` | Create a character token |
| `/vehicle` | `POST` | Create a vehicle token |
| `/place` | `POST` | Place an existing token on the pad |
| `/remove` | `DELETE` | Remove a token from the pad |

### Socket.IO Events

**Incoming:**

| Event | Payload | Meaning |
|---|---|---|
| `refreshTokens` | none | Fetch `/json/toytags.json` |
| `Connection True` | none | Game has sent the Toy Pad wake command |
| `Color One` | `[padNumber, "#rrggbb"]` | Set one physical zone color |
| `Color All` | `["#rrggbb", "#rrggbb", "#rrggbb"]` | Set center, left, right colors |
| `Fade One` | `[zoneNumber, speed, cycles, "#rrggbb"]` | Animate one zone |
| `Fade All` | 9-element array | Animate all three zones |

**Outgoing:**

| Event | Payload | When |
|---|---|---|
| `connectionStatus` | none | Immediately after socket connection |
| `syncToyPad` | none | After initial connection or manual resync |
| `deleteToken` | UID string | Permanent deletion (only when off-pad) |

### Toy Pad Mapping

| Pad Index | Zone | Protocol Position |
|:---:|:---|:---:|
| 1 | LEFT | 2 |
| 2 | CENTER | 1 |
| 3 | RIGHT | 3 |
| 4 | LEFT | 2 |
| 5 | LEFT | 2 |
| 6 | RIGHT | 3 |
| 7 | RIGHT | 3 |

---

## Domain Models

### Catalogue Items

```kotlin
sealed interface CatalogueItem {
    val id: Int
    val name: String
    val world: String
    val abilities: List<String>
}

data class Character(
    override val id: Int,
    override val name: String,
    override val world: String,
    override val abilities: List<String>
) : CatalogueItem

data class Vehicle(
    override val id: Int,
    override val name: String,
    override val world: String,
    override val abilities: List<String>,
    val upgradeMap: Int,
    val rebuild: Int
) : CatalogueItem
```

### Remote Token

```kotlin
data class RemoteToken(
    val uid: String,              // Opaque string, never parsed as number
    val id: Int,
    val name: String,
    val type: TokenType,          // CHARACTER, VEHICLE, UNKNOWN
    val padIndex: Int?,           // null when off-pad (index == -1)
    val vehicleUpgradesP23: Long, // Stored as Long, may exceed Int.MAX_VALUE
    val vehicleUpgradesP25: Long
)
```

### Connection State

```kotlin
sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(
        val baseUrl: String,
        val socketConnected: Boolean,
        val gameDetected: Boolean
    ) : ConnectionState
    data class Reconnecting(val attempt: Int) : ConnectionState
    data class Failed(val error: AppError) : ConnectionState
}
```

---

## Build Configuration

```kotlin
// app/build.gradle.kts
android {
    namespace = "com.ldtoypad.remote"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.ldtoypad.remote"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

**Required manifest permissions:**

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

The emulator uses cleartext HTTP on a private LAN. A network security configuration permits cleartext traffic for this app.

---

## Development Roadmap

### Milestone 0 — Project Bootstrap ✅
- Create Compose Android project
- Configure package structure and dependencies
- Add cleartext network configuration

### Milestone 1 — Read-Only Connection 🔄
- URL normalization
- HTTP client and DTOs
- Connection screen
- Fetch all three JSON resources

### Milestone 2 — Read-Only Pad and Library
- Domain models and repositories
- Seven-slot pad UI with zone grouping
- Toy box list
- Library search/filter

### Milestone 3 — Socket.IO and Lights
- Socket wrapper and lifecycle
- Refresh and connection events
- Light-state rendering

### Milestone 4 — Token Mutations
- Create, place, remove, move
- Mutation mutex and confirmation polling
- Permanent deletion

### Milestone 5 — Local Features
- Room/DataStore persistence
- Favorites, recents, profiles
- Presets

### Milestone 6 — Hardening
- Full test suite
- Accessibility pass
- Release build and documentation

---

## Potential Issues

1. **Early Development** — Only 2 commits, created recently. Much of the spec is documented but not yet fully implemented.
2. **No Open-Source License** — No license is currently specified.
3. **Socket.IO Compatibility** — Must verify client version compatibility with server v4.8.x.
4. **Test Coverage** — Minimal at this stage; extensive testing is planned per the spec.
5. **Manual DI** — `AppContainer` works now but may need migration to Hilt/Koin as the app grows.
6. **Server API Limitations** — The emulator has informal APIs with race conditions; the client must work around them carefully.

---

## License

No license is currently specified for this project.

---

## Acknowledgments

- Based on the [LD-ToyPad-Emulator](https://github.com/Berny23/LD-ToyPad-Emulator) by Berny23
- Specification derived from emulator server commit `b04f2b4621a31d3c61eb33a53264f4a09a178a14` (2026-06-17)
