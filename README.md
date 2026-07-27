# SPEDA GO

**The Mark VI mobile UI.** A native Android client for SPEDA — Kotlin, Jetpack
Compose, no WebView shell, no Flutter, no React Native. It talks to your own
[Igor](#the-backend-it-talks-to) backend over HTTPS and streams turns over SSE,
and it wears the same Stark fluid-glass hologram interface as the desktop client
— same tokens, same material, same motion, same copy.

```
Kotlin 2.1  ·  Compose (BOM 2025.01)  ·  minSdk 31  ·  targetSdk 35  ·  AGP 8.9  ·  Gradle 8.11.1
```

Single-user by design. There is no sign-up, no account server, no telemetry:
first launch asks for your backend URL and your API key, and that key is wrapped
by the Android Keystore and never leaves the device.

---

## What it is

SPEDA (Specialized Personal Executive Digital Assistant) is a proactive ambient
assistant with a roster of specialist agents — SPEDA, Sentinel (finance),
NightCrawler (OSINT), Ultron (academic), Centurion (security), Atomix (health),
Optimus, Orion, and the War Room. The backend and the desktop client live in the
`speda-mark6` monorepo; **this repo is the phone.**

It was split out of `packages/heartbreaker-android` with its full commit history
intact, so `git log` here is the real record of the port, not a squashed dump.

The port's governing rule: **the sub-768px web layout is the mobile spec.** The
desktop client already ships a complete mobile adaptation — off-canvas frosted
drawer, HUD strip with DIAG dropdown, composer overflow menu, sticky composer,
44dp touch targets. Android replicates *that*, value for value, rather than
reinterpreting the desktop layout. Pure logic (the chat reducer, the segment
builder, the theme math, the markdown pre-processors) was transliterated from the
TypeScript and locked with fixtures generated from the originals — see
[Parity is tested, not claimed](#parity-is-tested-not-claimed).

---

## Features

### Chat
- **Live SSE streaming** with per-frame chunk coalescing and a typewriter reveal
  that catches up exponentially (`speed = max(45, remaining × 7)` chars/s).
- **Detached runs.** A turn keeps going server-side when you switch sessions,
  background the app, or lose signal; the app re-attaches on return via
  `/chat/active` → `/chat/attach/{id}`.
- **A watchdog with an opinion.** 15s stall → a status line naming the model,
  300s dead → abort with a phase-specific diagnostic (no-start / tool-stuck /
  no-tokens), never a bare spinner.
- **Tool feed** — verb + target rows that expand into red/green edit diffs,
  `$ command` + output, or key:value + result, interleaved at the exact character
  offset in the reply where each tool fired.
- **Offline transcripts.** Every settled turn is cached per `(agent, session)`
  and rehydrated instantly on open; the server wins on refresh unless it comes
  back empty.
- Rename / delete sessions, regenerate, edit-and-resend, copy, and read-aloud via
  on-device TTS.

### Rich content
| Fence / syntax | Rendered as |
|---|---|
| Markdown | Native Compose prose — accent header plates, `MAIN_SUB` underscore split, `▸` list markers, chip-styled inline code, data-grid tables, boxed source chips |
| ` ```chart ` | Stark line / area / bar / pie charts |
| ` ```calendar ` | Holographic week view with HUD ring and event chips |
| ` ```map ` | Interactive MapLibre GL vector map on a dark basemap we own — no Play Services |
| ` ```svg ` | AndroidSVG → Compose Canvas, crisp at any zoom |
| Code fences | Glass code block, language header, copy |
| `$…$` / `$$…$$` | KaTeX (bundled offline) with currency-`$` protection |
| Files & images | Download cards filed under `Documents/Speda Mark VI`, image thumbnails, uploads as chips |

Partial fences stream safely: an incomplete chart or calendar shows a quiet
`MATERIALIZING` placeholder instead of a parse error flickering mid-frame.

### The roster
- **Agent switcher** (the armoury overlay) — counter-rotating HUD rings, staggered
  pod boot, lock-in flare, then a 500ms palette morph into the new agent's colour.
- **Comms** — the inter-agent message tray: who dispatched what to whom, live
  `WORKING… Ns` elapsed timers, threaded replies, broadcast/HP tags.
- **Systems board** — uplink telemetry, the `ROUTING_MATRIX` model tiles (tap to
  route the active model), per-agent core pins, MCP context shards, token-budget
  gauge, RTT trace, and the knowledge bank with revision history and restore.
- **House Party Protocol** — the all-hands mode, passphrase-gated, with the full
  ignite / stand-down cinematic.

### Settings
Eight tabs: General, Configuration (the backend's own typed config schema,
masked secrets, applied-live vs restart-required), Connections (Google / Notion
OAuth, MCP toggles), Automations (n8n + Telegram), Health, Interface, Data
(import a Claude export, index history), Account.

### Android exclusives
- **Health sync (Atomix).** Health Connect → Igor on a 4-hour WorkManager cadence,
  read-only: steps, distance, sleep, heart rate, resting HR, exercise, weight,
  body fat, SpO₂. Every type is approved by you in Health Connect's own system
  sheet — declaring the permission grants nothing.
- **Location awareness**, opt-in behind a Settings toggle, so the assistant knows
  where "near me" is.
- Edge-to-edge AMOLED black, real backdrop refraction (Haze), and glass that
  degrades by design to the occluding-fill fallback where nested blur is
  cancelled.

---

## Architecture

```
speda-go/
├── app/                                   # the application module
│   └── src/main/kotlin/com/speda/heartbreaker/
│       ├── data/        # IgorApi (SSE + REST), Keystore-wrapped uplink store,
│       │                # settings/agents DTOs, message cache, downloader, health poller
│       ├── domain/      # pure Kotlin, no Android: the 19-action chat reducer,
│       │                # buildSegments, markdown prep, math extract, partial-JSON,
│       │                # chart/calendar/map specs, polyline, watchdog, tool status
│       ├── health/      # Health Connect source + sync manager + WorkManager worker
│       └── ui/          # chat, prose, settings, shell, comms, systems, switcher, gallery
└── designsystem/                          # the Stark language, as a library module
    └── src/main/kotlin/com/speda/heartbreaker/designsystem/
        ├── theme/       # colour math, base token tables, the theme engine, palette morph
        ├── glass/       # THE ONE glass material + etched seams + haze
        ├── brand/       # the roster: names, accents, marks, taglines
        ├── background/  # the ambient blob field that re-hues per agent
        ├── type/        # Rajdhani / Inter / JetBrains Mono ramp (bundled, OFL)
        ├── motion/      # motion tokens
        └── icons/       # the glyph set
```

Two rules hold the shape:

1. **One glass material.** Exactly one `Modifier.hbGlass()` implementation. Every
   surface uses it with thin state modifiers (tint / active / amber / ghost /
   round) — never a per-component recipe.
2. **Zero identity strings outside `brand/`.** Agent names, accents and taglines
   live in `Brands.kt` and nowhere else, mirroring the backend's own Rule 10.

The theme engine takes one accent hex and derives the entire palette: bright =
mix 28% white, dim = mix 62% void, `rehue()` keeps S/L and swaps hue across the
base token tables. Switching agents morphs that palette over 500ms, easing
`easeInOutQuad`, rebuilt every frame.

---

## Getting started

**You need:** Android Studio (Ladybug or newer) or a JDK 17+ toolchain, the
Android SDK (compileSdk 35), and a reachable Igor backend.

```bash
git clone https://github.com/spedatox/speda-go
cd speda-go
./gradlew :designsystem:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. `local.properties` is
gitignored — Android Studio writes your `sdk.dir` on first sync; from the CLI,
export `ANDROID_HOME` instead.

Building from a plain shell (no IDE) with Android Studio's bundled JBR:

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ANDROID_HOME="$LOCALAPPDATA/Android/Sdk" ./gradlew :app:assembleDebug
```

### First run

The app opens on **ESTABLISH UPLINK** and asks for two things:

| Field | Value |
|---|---|
| `API BASE` | `https://your-host:port` — where Igor is listening |
| `API KEY` | your `SPEDA_API_KEY`; sent as `X-API-Key` on every request |

The key is stored via `UplinkStore`, wrapped by an Android Keystore-held AES key.
It is never logged, never synced, and `allowBackup` is off.

If your backend is plain `http://`, add that host to
`app/src/main/res/xml/network_security_config.xml` — cleartext is denied by
default and there is no global opt-out in this build.

### Release builds

R8 + resource shrinking are on; the signing config is commented out in
`app/build.gradle.kts` because this is a personal, single-user app with no Play
Store presence. Wire your own keystore there — `*.jks`, `*.keystore` and
`keystore.properties` are all gitignored, deliberately.

---

## Parity is tested, not claimed

Everything transliterated from the TypeScript is asserted against fixtures
generated from the shipping web client, so drift shows up as a red test rather
than as a slightly-wrong pixel:

| Module | Test | Covers |
|---|---|---|
| designsystem | `ThemeEngineTest` | `buildThemeVars` / `deriveAccents` reproduced for all 9 agents (369 assertions in the original cross-check) |
| designsystem | `AgentMarksTest` | the roster's marks |
| app | `SegmenterTest` | `buildSegments` tool-interleaving against generated fixtures |
| app | `ReducerTest` | the 19-action chat store, including the SELECT_SESSION-during-stream race |
| app | `MarkdownPrepTest` | the markdown pre-processors |
| app | `MathExtractTest` | math extraction and the currency-`$` guard |
| app | `MapSpecTest` | ` ```map ` fence parsing |

One hard-won detail worth keeping: rounding uses `floor(x + 0.5)` to match
JavaScript's `Math.round`, **not** Kotlin's banker's `round`. Swap it and the
palette drifts by a bit per channel.

---

## The backend it talks to

Every call carries `X-API-Key`. The surface this client consumes:

- **Chat** — `POST /chat/{agent_id}` (SSE: `start|chunk|tool|tool_result|file|done|error`),
  `GET /chat/attach/{request_id}`, `GET /chat/active`, `POST /chat/cancel/{request_id}`,
  `GET /welcome/{agent_id}`
- **Sessions** — `GET /sessions`, `GET /sessions/{id}/messages`, `PATCH`, `DELETE`
- **Models** — `GET /models`, `GET|POST /agents/models`, `/agents/legion-models`
- **Agents** — `GET /agents`, `GET /agents/comms`, `GET|POST /agents/house-party`
- **Memory** — `GET|PUT /memory/files` (409 optimistic concurrency),
  `/memory/files/revisions`, `/memory/files/restore`, `/memory/sources`
- **Health** — `POST /health/data`, `GET /health/status`, `GET /health`
- **Ops** — `GET|PUT /config`, `GET|POST /budget-mode`, `GET|POST /connections`,
  `/automations*`, `/admin/index-history`

No backend change is required to run this client against a stock Mark VI.

---

## Status

Shipped: the design system, the chat core, the full prose/rich-content renderer,
files and images, settings, the systems board, comms, the agent switcher, maps,
and Health Connect sync. Build is green — both modules compile, the unit suites
pass, and `assembleDebug` produces an installable APK.

Open: voice input in the composer (read-aloud already works), push notifications,
and the screenshot-diff visual-parity ritual against the web build — today
correctness is asserted by the unit tests above, not by pixel diffing.

---

## Relationship to the monorepo

This repo is a `git subtree split` of `packages/heartbreaker-android` in
`speda-mark6`. It carries that path's history and stands alone: its own Gradle
build, its own version catalog, no monorepo tooling required. Fixes can travel
back the same way they came.

Personal project. No warranty, no support, and nothing here is intended for
distribution.
