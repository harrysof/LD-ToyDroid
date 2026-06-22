# LD Toy Pad Remote for Android — Implementation Specification

## 1. Purpose

Build a native Android application that remotely controls the existing
`Berny23/LD-ToyPad-Emulator` server over a local network.

The Android app is a replacement for the emulator's browser interface. It does
not emulate USB, communicate directly with RPCS3/Cemu, or implement the LEGO
Dimensions USB protocol. The existing Node.js server remains responsible for:

- USB Toy Pad emulation
- Communication with the game
- Creating token UIDs
- Persisting vehicle upgrades
- Maintaining the authoritative list of created tokens

The Android app is responsible for:

- Connecting to the Node.js server over LAN
- Showing the seven Toy Pad positions
- Showing created characters and vehicles
- Creating, placing, moving, and removing tokens
- Searching and filtering the character/vehicle catalogues
- Displaying Toy Pad light effects
- Saving app-local favorites, recent items, connection profiles, and presets

This specification is based on the repository state at commit:

```text
b04f2b4621a31d3c61eb33a53264f4a09a178a14
2026-06-17
```

Do not invent `/api/*` endpoints. They do not exist in the current server.

---

## 2. Feasibility decision

The app is feasible without changing the emulator server.

The server already exposes:

- HTTP routes for creating, placing, and removing tokens
- Static JSON files containing catalogues and current token state
- Socket.IO events for refresh notifications, game connection status, and
  Toy Pad lighting

The current API is informal and has several race conditions. The Android client
must work around them as described in this document.

---

## 3. Scope

### 3.1 MVP scope

The first usable release must include:

1. Manual server connection by hostname or IP address and port
2. Connection test and useful error messages
3. Character and vehicle catalogue download
4. Current token-state download
5. Seven-position Toy Pad screen
6. Create character
7. Create vehicle
8. Place token
9. Remove token from the pad
10. Move token between pad positions
11. Search by name
12. Filter by token type, world, and ability
13. Favorites and recent tokens
14. Live Socket.IO connection
15. Live Toy Pad color events
16. Automatic state resynchronization
17. App-local presets

### 3.2 Later scope

These features should be implemented only after the MVP is stable:

- mDNS/NSD server discovery
- Optional emulator API improvements
- Server authentication
- Import/export of presets
- Tablet-specific two-pane layout
- Custom token image upload
- Multiple visual themes

### 3.3 Explicit non-goals

Do not implement any of the following in the Android app:

- USB HID emulation
- Android USB gadget mode
- Direct RPCS3 or Cemu integration
- NFC tag writing
- LEGO Dimensions game files
- Editing vehicle upgrade bytes
- Replacing `node-ld`
- Cloud accounts or Internet relay
- Remote access outside the user's LAN

---

## 4. Verified emulator protocol

The server normally listens on HTTP port `80`.

Example base URLs:

```text
http://192.168.1.25
http://debian
http://192.168.1.25:8080
```

The client must accept a configurable port because container installations may
map server port 80 to another host port.

### 4.1 HTTP endpoints

#### Fetch character catalogue

```http
GET /json/charactermap.json
```

Successful response: JSON array.

```json
[
  {
    "id": 1,
    "name": "Batman",
    "world": "DC Comics",
    "abilities": "Grapple,Boomerang,Stealth,Rope Swing,Glide,Detective Mode,Master Build"
  }
]
```

Verified catalogue size at the referenced commit: 86 entries.

#### Fetch vehicle catalogue

```http
GET /json/tokenmap.json
```

Successful response: JSON array.

```json
[
  {
    "id": 1000,
    "upgrademap": 0,
    "rebuild": 0,
    "name": "Police Car",
    "world": "The LEGO Movie",
    "abilities": "Accelerator Switches,Tow Bar"
  }
]
```

Verified catalogue size at the referenced commit: 266 entries.

Some rebuild names begin with `"* "`. Preserve the server-provided name, but
the UI may remove the prefix for sorting if it still displays a rebuild badge.

#### Fetch all created tokens and pad state

```http
GET /json/toytags.json
```

Successful response: JSON array.

```json
[
  {
    "name": "Batman",
    "id": 1,
    "uid": "01020304050607",
    "index": "-1",
    "type": "character",
    "vehicleUpgradesP23": 0,
    "vehicleUpgradesP25": 0
  },
  {
    "name": "Police Car",
    "id": 1000,
    "uid": "11121314151617",
    "index": 3,
    "type": "vehicle",
    "vehicleUpgradesP23": 4026531839,
    "vehicleUpgradesP25": 4026531839
  }
]
```

Important parsing requirements:

- `index` may be the string `"-1"` or a JSON number.
- Parse `index` with a custom serializer into an integer.
- `-1` means the token exists in the toy box but is not on the pad.
- Values `1` through `7` mean the token is occupying that pad index.
- Treat any other value as invalid remote data.
- Treat `uid` as an opaque string. Never parse it as a number.
- Upgrade values may exceed signed 32-bit range. Store them as Kotlin `Long`.
- Unknown fields must not break deserialization.
- Missing optional upgrade fields should default to zero.

#### Create a character token

```http
POST /character
Content-Type: application/json

{
  "id": 1
}
```

Expected success: HTTP 2xx with an empty response body.

The server generates the UID. It does not return the created character.

The server responds before its asynchronous `toytags.json` write is guaranteed
to be complete. The app must use the creation workflow in section 8.4.

#### Create a vehicle token

```http
POST /vehicle
Content-Type: application/json

{
  "id": 1000
}
```

Expected success: HTTP 2xx. The current server sends the generated UID as the
response body, but the app must not assume the corresponding JSON file write
has finished.

Use the same polling workflow as character creation.

#### Place an existing token

```http
POST /place
Content-Type: application/json

{
  "uid": "01020304050607",
  "id": 1,
  "position": 2,
  "index": 1
}
```

Expected success: HTTP 2xx with an empty body.

Requirements:

- The UID must already exist in `toytags.json`.
- `id` must match the existing token.
- `position` is the physical zone number.
- `index` is the unique pad index from 1 through 7.
- Never place a token into an occupied index.
- Never issue two pad mutations concurrently.

The server does not emit `refreshTokens` after this request. The app must fetch
`toytags.json` after the request succeeds.

#### Remove a token from the Toy Pad

```http
DELETE /remove
Content-Type: application/json

{
  "index": 1,
  "uid": "01020304050607"
}
```

Expected success: HTTP 2xx with body `true`.

The UID must be the token currently believed to occupy the supplied index. A
wrong UID can make server state inconsistent because the server removes by
index but updates persisted state by UID.

The server does not emit `refreshTokens` after this request. Fetch state after
the request succeeds.

### 4.2 Socket.IO connection

The server uses Socket.IO server `4.8.x`.

Connect Socket.IO to the same scheme, host, and port as HTTP. Use the default
`/socket.io` path. Do not connect to a separate WebSocket URL.

The app must listen for:

| Event | Payload | Meaning |
|---|---|---|
| `refreshTokens` | none | Fetch `/json/toytags.json` |
| `Connection True` | none | The game has sent the Toy Pad wake command |
| `Color One` | `[padNumber, "#rrggbb"]` | Set one physical zone color |
| `Color All` | `["#rrggbb", "#rrggbb", "#rrggbb"]` | Set center, left, right colors |
| `Fade One` | `[padNumber, speed, cycles, "#rrggbb"]` | Temporarily animate one zone |
| `Fade All` | nine-element array | Animate center, left, and right zones |

The app must emit:

| Event | Payload | When |
|---|---|---|
| `connectionStatus` | none | Immediately after socket connection |
| `syncToyPad` | none | Once after initial connection or manual resync |
| `deleteToken` | UID string | Permanent deletion, only when token is off-pad |

`syncToyPad` causes the server to reconstruct all persisted token indexes from
the in-memory Toy Pad state and then emit `refreshTokens`.

Do not emit `syncToyPad` repeatedly. Use it:

- once after a new connection is established;
- after the user presses a manual resync button;
- after an operation fails with suspected state disagreement.

### 4.3 Lighting payload rules

Physical zone numbers:

- `1`: center
- `2`: left
- `3`: right
- `0`: all zones for some underlying USB commands

`Color One` payload:

```json
[2, "#ff0000"]
```

`Color All` array order:

```text
[centerColor, leftColor, rightColor]
```

`Fade One` payload order:

```text
[zoneNumber, speed, cycles, color]
```

`Fade All` payload order:

```text
[
  centerSpeed,
  centerCycles,
  centerColor,
  leftSpeed,
  leftCycles,
  leftColor,
  rightSpeed,
  rightCycles,
  rightColor
]
```

For the initial release:

- Save each zone's steady color.
- On a fade event, display the fade color temporarily.
- Restore the saved steady color after `speed * 100` milliseconds to mirror the
  browser client.
- Do not attempt to loop by `cycles` until behavior is verified against a game.
- If a color cannot be parsed, ignore that event without disconnecting.

---

## 5. Exact Toy Pad mapping

There are seven unique positions and three physical zones.

| Pad index | Zone enum | Protocol position | Display placement |
|---:|---|---:|---|
| 1 | LEFT | 2 | Left zone, primary |
| 2 | CENTER | 1 | Center |
| 3 | RIGHT | 3 | Right zone, primary |
| 4 | LEFT | 2 | Left zone, secondary |
| 5 | LEFT | 2 | Left zone, tertiary |
| 6 | RIGHT | 3 | Right zone, secondary |
| 7 | RIGHT | 3 | Right zone, tertiary |

Model this mapping as a hard-coded domain function. Do not derive it from UI
position or array order.

```kotlin
enum class PadZone(val protocolPosition: Int) {
    CENTER(1),
    LEFT(2),
    RIGHT(3)
}

fun zoneForIndex(index: Int): PadZone = when (index) {
    2 -> PadZone.CENTER
    1, 4, 5 -> PadZone.LEFT
    3, 6, 7 -> PadZone.RIGHT
    else -> error("Invalid pad index: $index")
}
```

---

## 6. Android technical requirements

### 6.1 Platform

- Kotlin
- Jetpack Compose
- Material 3
- Single-activity architecture
- Coroutines and `StateFlow`
- Minimum Android version: API 26
- Compile and target SDK: use the newest stable SDK installed in the build
  environment
- Gradle Kotlin DSL
- Version catalog in `gradle/libs.versions.toml`

Do not use Flutter, React Native, a WebView, or XML layouts.

### 6.2 Suggested libraries

Use stable, mutually compatible releases of:

- AndroidX Core KTX
- AndroidX Activity Compose
- Jetpack Compose BOM
- Material 3
- AndroidX Navigation Compose
- AndroidX Lifecycle ViewModel Compose
- Kotlin coroutines
- OkHttp
- Kotlin serialization or Moshi
- Socket.IO Java client compatible with a Socket.IO 4.x server
- Room
- DataStore Preferences
- Coil Compose for optional remote/custom images
- JUnit
- Turbine for Flow tests
- MockWebServer

Use either Kotlin serialization or Moshi throughout. Do not mix multiple JSON
libraries.

### 6.3 Android network configuration

Required manifest permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```

The emulator uses cleartext HTTP on a private LAN. Add a network security
configuration that permits cleartext traffic for this app. Document the
security implication in the connection screen.

Do not request broad local-network or location permissions unless they become
necessary for the later NSD discovery feature.

---

## 7. Proposed project structure

Use one Android application module initially. Keep package boundaries clear so
the project can be split later without a rewrite.

```text
app/
  src/main/java/com/ldtoypad/remote/
    App.kt
    MainActivity.kt

    core/
      result/
        AppError.kt
        AppResult.kt
      network/
        BaseUrlNormalizer.kt
        NetworkModule.kt
      ui/
        AppTheme.kt
        UiText.kt

    data/
      remote/
        EmulatorHttpClient.kt
        EmulatorSocketClient.kt
        dto/
          CharacterDto.kt
          VehicleDto.kt
          RemoteTokenDto.kt
          FlexibleIntSerializer.kt
      local/
        AppDatabase.kt
        dao/
          FavoriteDao.kt
          RecentDao.kt
          PresetDao.kt
        entity/
          FavoriteEntity.kt
          RecentEntity.kt
          PresetEntity.kt
          PresetSlotEntity.kt
        settings/
          ConnectionSettingsStore.kt
      repository/
        ConnectionRepository.kt
        CatalogueRepository.kt
        ToyPadRepository.kt
        PresetRepository.kt

    domain/
      model/
        CatalogueItem.kt
        Character.kt
        Vehicle.kt
        RemoteToken.kt
        PadSlot.kt
        PadZone.kt
        ToyPadState.kt
        ConnectionState.kt
        LightState.kt
        Preset.kt
      usecase/
        ConnectToServer.kt
        CreateToken.kt
        PlaceToken.kt
        RemoveToken.kt
        MoveToken.kt
        DeleteToken.kt
        LoadPreset.kt
        SavePreset.kt

    feature/
      connection/
        ConnectionScreen.kt
        ConnectionViewModel.kt
      pad/
        ToyPadScreen.kt
        ToyPadViewModel.kt
        PadZoneView.kt
        PadSlotView.kt
      library/
        LibraryScreen.kt
        LibraryViewModel.kt
        CatalogueItemRow.kt
        FilterSheet.kt
      presets/
        PresetsScreen.kt
        PresetsViewModel.kt
        PresetEditorScreen.kt
      settings/
        SettingsScreen.kt
        SettingsViewModel.kt

  src/test/
  src/androidTest/
```

Names may differ slightly, but maintain the same separation of remote DTOs,
database entities, domain models, repositories, and UI.

---

## 8. Domain and data models

### 8.1 Catalogue item

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

Split the comma-separated `abilities` string by comma, trim each value, remove
empty values, and preserve display capitalization.

### 8.2 Remote token

```kotlin
enum class TokenType {
    CHARACTER,
    VEHICLE,
    UNKNOWN
}

data class RemoteToken(
    val uid: String,
    val id: Int,
    val name: String,
    val type: TokenType,
    val padIndex: Int?,
    val vehicleUpgradesP23: Long,
    val vehicleUpgradesP25: Long
)
```

Map remote index `-1` to `padIndex = null`.

Do not infer token type solely from ID. Prefer the server's `type` field, with
ID-based inference only as a fallback:

- IDs below 1000 are normally characters.
- IDs 1000 and above are normally vehicles.

### 8.3 Toy Pad state

```kotlin
data class PadSlot(
    val index: Int,
    val zone: PadZone,
    val token: RemoteToken?
)

data class ToyPadState(
    val slots: List<PadSlot>,
    val toyBoxTokens: List<RemoteToken>,
    val allTokens: List<RemoteToken>,
    val lastUpdatedAt: Instant
)
```

Always produce exactly seven slots sorted by index.

If remote data contains two tokens with the same occupied index:

- mark the state as inconsistent;
- show a warning;
- disable mutations;
- allow the user to request `syncToyPad`;
- do not arbitrarily choose one token and continue.

### 8.4 Connection state

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

HTTP availability and Socket.IO availability are separate. The app may remain
usable for direct commands when HTTP works but Socket.IO is temporarily down.

### 8.5 Light state

```kotlin
data class ZoneLight(
    val steadyColor: Color,
    val displayedColor: Color,
    val animationJobId: Long?
)

data class ToyPadLights(
    val center: ZoneLight,
    val left: ZoneLight,
    val right: ZoneLight
)
```

Default all zones to neutral white or a theme-appropriate translucent white.

---

## 9. Server connection workflow

### 9.1 Connection form

Fields:

- Hostname or IP address
- Port, default `80`
- Optional profile name
- Remember connection toggle, default enabled

Accept input in any of these forms:

```text
192.168.1.25
192.168.1.25:8080
http://192.168.1.25
http://192.168.1.25:8080/
debian
```

Normalize to:

```text
http://host[:port]
```

Remove path, query, fragment, and trailing slash. Reject:

- unsupported schemes;
- blank hosts;
- ports outside 1–65535;
- public-looking URLs only with a warning, not a hard-coded Internet block.

Handle IPv6 literals by preserving brackets.

### 9.2 Connection test sequence

Perform these steps in order:

1. Set state to `Connecting`.
2. `GET /json/charactermap.json`.
3. Validate that the body is a non-empty character array.
4. `GET /json/tokenmap.json`.
5. Validate that the body is a non-empty vehicle array.
6. `GET /json/toytags.json`.
7. Validate and map current token state.
8. Open Socket.IO.
9. On socket connection, emit `connectionStatus`.
10. Emit `syncToyPad` once.
11. Wait for `refreshTokens`, but do not fail connection if it is not received.
12. Fetch `toytags.json` once more after a short debounce.
13. Save the connection profile only after HTTP validation succeeds.
14. Navigate to the pad screen.

Timeout recommendations:

- HTTP connect timeout: 5 seconds
- HTTP read timeout: 10 seconds
- Overall connection validation: 20 seconds

### 9.3 Connection errors

Map low-level failures into user-facing errors:

| Condition | Message |
|---|---|
| DNS failure | “Server name could not be found.” |
| Connection refused | “No Toy Pad server is listening at this address.” |
| Timeout | “The Toy Pad server did not respond.” |
| HTTP 404 for catalogue | “This server does not appear to be LD Toy Pad Emulator.” |
| Malformed JSON | “The emulator returned data this app cannot read.” |
| Socket failure only | “Connected, but live updates are unavailable.” |
| Cleartext blocked | “Android blocked the local HTTP connection.” |

Show technical details in an expandable area, not as the primary message.

---

## 10. Synchronization strategy

The server is authoritative for:

- all created tokens;
- current occupied indexes;
- vehicle upgrade values.

The app must never update final pad state optimistically and leave it that way.
It may show a temporary operation indicator, but it must confirm every mutation
by fetching `toytags.json`.

### 10.1 Refresh triggers

Refresh remote state:

- immediately after initial HTTP connection;
- after `refreshTokens`;
- after every successful create/place/remove/move/delete operation;
- when the app enters the foreground;
- after Socket.IO reconnects;
- every 2 seconds while the pad screen is visible;
- every 10 seconds while another app screen is visible;
- after manual refresh.

Pause polling when the app is backgrounded.

Use a single debounced refresh coordinator. If five refresh triggers arrive
together, perform one request rather than five.

### 10.2 Mutation serialization

All pad mutations must run through one `Mutex` or single-consumer command queue.

Operations that must not overlap:

- create;
- place;
- remove;
- move;
- permanent delete;
- preset load.

While one mutation is running:

- show progress on the affected token/slot;
- disable conflicting controls;
- permit navigation only if it does not cancel the operation;
- do not launch a second mutation.

### 10.3 State confirmation

After each mutation:

1. Fetch state.
2. Check the expected condition.
3. If not confirmed, retry state fetch at 150 ms, 300 ms, 600 ms, and 1000 ms.
4. If still not confirmed, emit `syncToyPad`.
5. Fetch one final time.
6. If still incorrect, fail with a state-mismatch error and show both expected
   and actual state in technical details.

Do not automatically repeat the mutation itself unless it is known to be
idempotent. Repeating `/character` or `/vehicle` may create duplicate tokens.

---

## 11. Exact operation workflows

### 11.1 Place an existing token

Inputs:

- `uid`
- target index

Algorithm:

1. Lock the mutation mutex.
2. Refresh state.
3. Find the token by UID.
4. Fail if it no longer exists.
5. Fail if target index is not 1–7.
6. Fail if target index is occupied by another token.
7. If the token is already at the target index, return success without a call.
8. If the token occupies another index, call the move workflow instead.
9. Derive `position` from the hard-coded index-to-zone mapping.
10. Send `POST /place` with UID, ID, position, and index.
11. On 2xx, fetch state until the UID is confirmed at target index.
12. Add the catalogue item to recents.
13. Unlock.

### 11.2 Remove a token from the pad

Inputs:

- occupied index

Algorithm:

1. Lock.
2. Refresh state.
3. Find the token currently occupying the index.
4. If empty, return success.
5. Send `DELETE /remove` with the exact current UID and index.
6. Fetch state until that UID has `padIndex = null`.
7. Unlock.

This operation does not permanently delete the token. It returns it to the toy
box.

### 11.3 Move a token

Inputs:

- UID
- source index
- target index

Algorithm:

1. Lock.
2. Refresh state.
3. Verify source index contains the UID.
4. Verify target index is empty.
5. Send `DELETE /remove` with source index and UID.
6. Poll until the token is off-pad.
7. Wait 150 ms after confirmation. The old browser waits 500 ms; confirmation
   plus a smaller safety delay is preferable.
8. Send `POST /place` using target index and derived position.
9. Poll until target placement is confirmed.
10. If placement fails after removal, leave the token safely in the toy box and
    show “Move was not completed; the token was returned to the toy box.”
11. Never automatically place it back into the source slot without checking
    that the source is still empty.
12. Unlock.

There is no atomic server-side move operation.

### 11.4 Create a token

Inputs:

- catalogue item ID and type

Algorithm:

1. Lock.
2. Fetch `toytags.json` and record the complete set of existing UIDs.
3. For a character, call `POST /character`.
4. For a vehicle, call `POST /vehicle`.
5. Do not repeat the POST if a timeout occurs after request transmission. The
   server might have created the token despite the missing response.
6. Poll `toytags.json` at 100 ms, 200 ms, 400 ms, 800 ms, 1200 ms, and 2000 ms.
7. Identify entries whose UID was not in the pre-request UID set.
8. Prefer a new entry matching the requested ID and type.
9. If exactly one matching new entry exists, creation succeeded.
10. If multiple matching new entries exist, refresh and ask the user to choose;
    do not silently delete duplicates.
11. If no entry appears, report failure with a manual refresh action.
12. Return the complete `RemoteToken`, including the server-generated UID.
13. Unlock.

If the user selected an empty target slot before creation, place the newly
created token only after this workflow has confirmed it exists.

### 11.5 Permanent token deletion

This is destructive and must not be confused with removing from the pad.

Algorithm:

1. Require a confirmation dialog containing the token name.
2. Lock.
3. Refresh state.
4. If the token is on-pad, remove it and confirm it is off-pad first.
5. Emit Socket.IO event `deleteToken` with the UID.
6. Wait for `refreshTokens` or poll state.
7. Confirm that the UID no longer exists.
8. Remove local favorite/recent references that require the UID.
9. Mark presets referencing the deleted UID as degraded rather than deleting
   those presets.
10. Unlock.

If Socket.IO is unavailable, disable permanent deletion. The server has no HTTP
delete-token endpoint.

---

## 12. Preset design

Presets are stored locally in Room. The current emulator has no preset API.

### 12.1 Preset schema

```kotlin
data class Preset(
    val id: Long,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val slots: List<PresetSlot>
)

data class PresetSlot(
    val padIndex: Int,
    val tokenUid: String,
    val catalogueId: Int,
    val tokenType: TokenType,
    val displayNameAtSaveTime: String,
    val vehicleUpgradesP23AtSaveTime: Long?,
    val vehicleUpgradesP25AtSaveTime: Long?
)
```

UID is the primary token identity. ID and name are recovery metadata.

### 12.2 Save preset

1. Require a non-blank name.
2. Refresh current state.
3. Save all occupied slots.
4. Empty presets are allowed only after explicit confirmation.
5. Enforce unique preset names case-insensitively, or ask before replacement.
6. Save vehicle upgrade values for diagnostics and change warnings.

### 12.3 Load preset

Loading is a serialized, multi-step transaction, but the server cannot make it
atomic.

Algorithm:

1. Lock the global mutation queue for the entire operation.
2. Refresh state.
3. Validate that preset indexes are unique and within 1–7.
4. Resolve every preset UID against current server tokens.
5. If a UID is missing:
   - do not automatically create a replacement by default;
   - show the missing token and offer “Create replacement”;
   - explain that a recreated vehicle will not preserve its old upgrades.
6. Build the desired mapping `padIndex -> UID`.
7. Determine tokens already in their correct slots. Leave them untouched.
8. Remove every currently placed token that conflicts with desired state.
9. Confirm all required destination slots are empty.
10. Place desired tokens one at a time in index order.
11. Confirm each placement before continuing.
12. If one placement fails, stop, refresh, and show partial results.
13. Do not claim rollback. There is no atomic rollback capability.
14. Compare loaded vehicle upgrade values with values saved in the preset. If
    changed, warn but do not overwrite them.
15. Unlock.

Present progress such as:

```text
Loading preset: 3 of 5 tokens placed
```

### 12.4 Preset limitation

A preset can faithfully restore placement only while the original token UIDs
still exist on the server. If an upgraded vehicle token is permanently deleted,
the existing server API cannot recreate it with the same UID and upgrade bytes.

This limitation must be visible in the preset UI and README.

---

## 13. UI specification

### 13.1 Navigation

Bottom navigation destinations:

1. Pad
2. Library
3. Presets
4. Settings

Show the connection screen before these destinations if no valid active server
profile exists.

### 13.2 Connection screen

Content:

- App name and short explanation
- Host/IP field
- Port field
- Connect button
- Previously saved profiles
- Progress indicator
- Expandable troubleshooting help

Troubleshooting text should mention:

- phone and emulator must be on the same LAN;
- VM networking should normally use bridged mode;
- container port mappings may require a non-80 port;
- firewall rules can block the server;
- HTTP is unencrypted and intended for trusted home networks.

### 13.3 Pad screen

Top app bar:

- active server name/address;
- HTTP connection indicator;
- Socket.IO connection indicator;
- game-detected indicator;
- refresh action.

Main Toy Pad:

- center zone containing slot 2;
- left zone containing slots 1, 4, and 5;
- right zone containing slots 3, 6, and 7;
- zone backgrounds reflect current lighting state;
- occupied slots show token name, type, and optional image;
- empty slots show “Add” and index number;
- operation-in-progress slots show a spinner;
- avoid drag-and-drop as the only interaction because it is less accessible.

Required interactions:

- Tap empty slot: open library in “choose for slot” mode.
- Tap occupied slot: open token action sheet.
- Long press occupied slot: optional shortcut to move.

Token action sheet:

- Move
- Remove from pad
- View details
- Favorite/unfavorite
- Permanently delete, visually separated and destructive

Include a “Toy box” section below the pad showing all off-pad created tokens.

### 13.4 Library screen

Tabs or segmented control:

- All
- Characters
- Vehicles
- Favorites
- Recent

Controls:

- search field;
- world filter;
- ability filter;
- clear filters;
- sort by name, world, or recently used.

Each catalogue row shows:

- name;
- character/vehicle badge;
- world;
- first few abilities;
- rebuild badge for vehicle rebuild values greater than zero;
- favorite button;
- whether zero, one, or multiple created token instances exist.

Selecting an item:

- If one created off-pad instance exists, offer to place it.
- If multiple instances exist, show a UID/instance chooser.
- If all existing instances are on-pad, offer to create another.
- If no instance exists, offer to create.
- When opened for a target slot, complete creation/selection and placement,
  then return to the pad.

### 13.5 Presets screen

Show:

- preset name;
- number of occupied slots;
- token summary;
- missing-token warning;
- last modified time.

Actions:

- Load
- Rename
- Replace with current pad
- Duplicate
- Delete

Deleting a preset does not delete emulator tokens.

### 13.6 Settings screen

Include:

- active server profile;
- manage connection profiles;
- polling interval with safe defaults;
- light animations toggle;
- manual `syncToyPad`;
- clear local catalogue cache;
- export diagnostics;
- app version;
- protocol limitations.

---

## 14. Local persistence

Use DataStore for:

- active connection profile ID;
- simple connection preferences;
- polling interval;
- light animation preference;
- onboarding completion.

Use Room for:

- server profiles if multiple profile querying is needed;
- favorites by catalogue ID and type;
- recents with timestamp;
- presets;
- preset slots;
- optional catalogue cache.

Scope UID-based data by server profile. Two emulator servers may generate the
same UID or contain different toy boxes.

Suggested server identity:

```text
normalized base URL
```

If a future status endpoint provides a persistent server ID, migrate to it.

Never store passwords because the current server has no authentication.

---

## 15. Error handling rules

Create a typed error model:

```kotlin
sealed interface AppError {
    data class NetworkUnavailable(...)
    data class Timeout(...)
    data class HttpError(val status: Int, ...)
    data class InvalidServerData(...)
    data class SocketUnavailable(...)
    data class TokenNotFound(val uid: String)
    data class SlotOccupied(val index: Int)
    data class StateConflict(...)
    data class CreationUnconfirmed(...)
    data class PartialPresetLoad(...)
}
```

Rules:

- Never show a raw exception as the only user message.
- Never swallow errors silently.
- Log request method and path, but not excessive full catalogue bodies.
- Do not log private network data in release builds unless diagnostics are
  explicitly exported.
- A failed refresh must keep the last known state visible with a stale warning.
- A failed mutation must trigger a state refresh before re-enabling controls.
- Do not assume HTTP 2xx means the requested final state exists.

---

## 16. Optional emulator API improvements

Do not block the Android MVP on these changes. Implement them later in a
separate server commit.

### 16.1 Add status endpoint

```http
GET /api/v1/status
```

Suggested response:

```json
{
  "service": "ld-toypad-emulator",
  "apiVersion": 1,
  "appVersion": "1.4.1",
  "gameConnected": true
}
```

### 16.2 Add state endpoint

```http
GET /api/v1/state
```

Return a validated state snapshot rather than exposing a writable static JSON
file directly.

### 16.3 Fix mutation responses

- Await file writes before responding.
- Return the complete created token from character/vehicle creation.
- Validate IDs, UIDs, positions, and indexes.
- Return JSON errors with appropriate 4xx/5xx codes.
- Reject placing into occupied indexes.
- Add an atomic move endpoint.
- Broadcast a structured `stateChanged` event after every mutation.

### 16.4 Configuration and security

- Read port from an environment variable.
- Optionally advertise through mDNS.
- Optionally require a bearer token or pairing code.
- Keep unauthenticated mode available for backwards compatibility.

---

## 17. Testing plan

### 17.1 Unit tests

Required unit tests:

- Base URL normalization
- IPv4, hostname, custom port, and IPv6 parsing
- Character DTO mapping
- Vehicle DTO mapping
- Ability string splitting
- `index` parsing from string and number
- Upgrade value parsing above `Int.MAX_VALUE`
- Pad index-to-zone mapping for all seven indexes
- Invalid pad index rejection
- Toy Pad state construction
- Duplicate occupied-index detection
- Catalogue filtering
- Preset diff calculation
- Move workflow failure after successful removal
- Creation detection from before/after UID sets

### 17.2 HTTP integration tests

Use MockWebServer to test:

- successful connection;
- missing route;
- malformed JSON;
- slow response;
- empty catalogue;
- create character with delayed appearance;
- create vehicle with delayed appearance;
- placement confirmation;
- removal confirmation;
- state mismatch after 2xx;
- duplicate token creation ambiguity.

### 17.3 Socket tests

Wrap the Socket.IO library behind an interface so ViewModels can use a fake.

Test:

- connect/disconnect/reconnect state;
- `refreshTokens` causing one debounced refresh;
- game connection event;
- each lighting event payload;
- malformed lighting payload ignored safely;
- permanent deletion disabled when socket is unavailable.

### 17.4 Repository/use-case tests

Use fake HTTP and socket clients to test:

- serialized mutations;
- no duplicate POST retry after uncertain create result;
- place into occupied slot rejected locally;
- remove uses refreshed UID;
- move performs remove then place;
- partial preset load is reported accurately;
- missing preset token is not silently recreated.

### 17.5 Compose UI tests

Required:

- connection validation and errors;
- exactly seven slots rendered;
- correct slots grouped into each zone;
- selecting an empty slot opens library selection;
- occupied slot action sheet;
- search and filters;
- preset progress;
- stale-state banner;
- content descriptions for interactive controls.

### 17.6 Manual end-to-end matrix

Test against a real emulator server:

| Scenario | Expected result |
|---|---|
| Phone and server on same LAN | Connects |
| Wrong IP | Clear connection error |
| Wrong port | Clear connection error |
| Socket unavailable, HTTP available | Commands work; live-update warning |
| Create character | One new UID appears |
| Create vehicle | One new UID appears |
| Place in each index 1–7 | Correct zone/position sent |
| Move between all three zones | Token remains valid in game |
| Remove | Token returns to toy box |
| Game changes vehicle upgrades | New values appear after refresh |
| Two clients open | Polling eventually reconciles both |
| Server restarts | App reconnects and refreshes |
| App backgrounds/foregrounds | State refreshes on return |
| Load five-token preset | Final mapping matches preset |
| Preset contains deleted UID | User sees missing-token warning |

---

## 18. Build milestones and acceptance criteria

### Milestone 0 — Project bootstrap

Tasks:

- Create Compose Android project.
- Configure package structure.
- Add dependencies.
- Add cleartext network configuration.
- Add CI command for unit tests and debug build.

Acceptance:

- `assembleDebug` succeeds.
- Unit-test task succeeds.
- App launches to a placeholder connection screen.

### Milestone 1 — Read-only connection

Tasks:

- URL normalization.
- HTTP client.
- DTOs and custom index parser.
- Connection screen.
- Fetch all three JSON resources.
- Display server and catalogue counts.

Acceptance:

- App connects by IP, hostname, and custom port.
- Invalid server gives a useful error.
- 86 characters and 266 vehicles are read for the referenced repository data.
- Mixed string/number indexes parse correctly.

### Milestone 2 — Read-only Pad and Library

Tasks:

- Domain models and repositories.
- Seven-slot pad UI.
- Toy box list.
- Library search/filter UI.
- Polling and foreground refresh.

Acceptance:

- All seven positions map to correct zones.
- Existing placed and off-pad tokens appear correctly.
- Search is case-insensitive.
- World and ability filters combine correctly.

### Milestone 3 — Socket.IO and lights

Tasks:

- Socket wrapper.
- Connection lifecycle.
- Refresh and connection events.
- Light-state rendering.

Acceptance:

- Socket reconnects after temporary loss.
- `refreshTokens` refreshes state.
- Game-detected indicator responds to `Connection True`.
- Zone colors match verified event ordering.

### Milestone 4 — Token mutations

Tasks:

- Create character/vehicle.
- Place/remove/move.
- Mutation mutex.
- Confirmation polling.
- Permanent deletion.

Acceptance:

- Every mutation is confirmed from server state.
- Create handles delayed file writes.
- No two mutations overlap.
- Move failure leaves the token in a known safe state.
- Delete cannot run on a placed token without removing it first.

### Milestone 5 — Local features

Tasks:

- Room/DataStore.
- Favorites.
- Recents.
- Connection profiles.
- Presets.

Acceptance:

- Data survives app restart.
- UID data is scoped by server.
- Preset loading reports progress and partial failure.
- Missing preset tokens are never silently replaced.

### Milestone 6 — Hardening

Tasks:

- Full tests.
- Accessibility pass.
- Release logging rules.
- Diagnostics export.
- README and screenshots.

Acceptance:

- All automated tests pass.
- Manual test matrix passes.
- No raw crash occurs for malformed server responses.
- TalkBack can identify and operate every slot and primary action.

---

## 19. Definition of done

The Android application is complete when:

1. A user can install it on Android API 26 or newer.
2. The user can connect to an unmodified emulator server on the same LAN.
3. All existing characters, vehicles, and token instances are visible.
4. Every pad position is represented accurately.
5. The user can create, place, move, remove, and delete tokens.
6. Every mutation is serialized and verified against server state.
7. Game-driven light changes are displayed.
8. The app recovers from socket loss and server restart.
9. Favorites, recents, profiles, and presets persist locally.
10. Preset limitations are accurately communicated.
11. Unit, integration, and core UI tests pass.
12. The project README contains setup and troubleshooting instructions.

---

## 20. Instructions for the implementation agent

Follow these constraints strictly:

1. Inspect the existing server source before changing any protocol assumption.
2. Do not invent endpoints that are absent from `index.js`.
3. Implement one milestone at a time.
4. Run the debug build and tests after every milestone.
5. Keep emulator server changes separate from Android app changes.
6. Do not retry create POST requests automatically after an uncertain result.
7. Do not mutate local pad state without confirming remote state afterward.
8. Do not perform concurrent pad mutations.
9. Do not parse UID as a number.
10. Do not store upgrade values in Kotlin `Int`.
11. Do not treat remove-from-pad as permanent deletion.
12. Do not promise atomic preset loading or rollback.
13. Do not recreate missing upgraded vehicles silently.
14. Keep networking out of Compose UI functions.
15. Keep DTOs out of ViewModels and UI.
16. Add tests for every bug fixed during implementation.
17. Stop and report the exact failing request, response, and state if the real
    emulator contradicts this specification.

Recommended implementation order:

```text
bootstrap
→ URL normalization
→ HTTP reads
→ DTO/domain mapping
→ pad UI
→ library UI
→ Socket.IO
→ refresh coordinator
→ mutation queue
→ create/place/remove/move
→ local persistence
→ presets
→ hardening
```

Do not begin with visual polish. First prove the complete create → place → move
→ remove round trip against a real emulator.
