# CodeX Client

CodeX Client is a Fabric utility client for Minecraft `1.21.4`.

This repository is published as an archival release. Production on CodeX Client has been officially discontinued. No further official releases, support, or roadmap work are planned. The codebase is being left in a cleaned single-version state so it can be studied, forked, and extended independently.

## Current State

- Target version: Minecraft `1.21.4`
- Loader: Fabric
- Java: `21`
- Build layout: single Gradle project at repository root
- Status: archived, public, no active production

## Project Layout

The active project lives entirely at the repository root.

- [build.gradle.kts](/C:/Users/ADITYA/OneDrive/Desktop/CodeX%20Client%20(src)/build.gradle.kts)
- [settings.gradle.kts](/C:/Users/ADITYA/OneDrive/Desktop/CodeX%20Client%20(src)/settings.gradle.kts)
- [src/main/java](/C:/Users/ADITYA/OneDrive/Desktop/CodeX%20Client%20(src)/src/main/java)
- [src/main/resources](/C:/Users/ADITYA/OneDrive/Desktop/CodeX%20Client%20(src)/src/main/resources)
- [src/test/java](/C:/Users/ADITYA/OneDrive/Desktop/CodeX%20Client%20(src)/src/test/java)

## Features Included

The current client build registers these modules:

- `Toggle Sprint`
- `ClickGUI`
- `Keystrokes`
- `Armor Status`
- `FPS & Ping`
- `Zoom`
- `Potion Effects`
- `Time Changer`
- `Fullbright`
- `Custom Crosshair`
- `Block Overlay`
- `Aim Assist`

Feature behavior and polish are exactly what this codebase currently implements. This repository is not presented as a commercial product, a maintained client platform, or a bypass-focused client.

## Build

Run all commands from the repository root.

```powershell
.\gradlew.bat build
.\gradlew.bat test
```

Useful additional commands:

```powershell
.\gradlew.bat check
.\gradlew.bat runClient
```

## Development Notes

- Main Fabric entrypoints:
  - [CodeX.java](/C:/Users/ADITYA/OneDrive/Desktop/CodeX%20Client%20(src)/src/main/java/com/codex/CodeX.java)
  - [CodeXClient.java](/C:/Users/ADITYA/OneDrive/Desktop/CodeX%20Client%20(src)/src/main/java/com/codex/client/CodeXClient.java)
- Fabric metadata:
  - [fabric.mod.json](/C:/Users/ADITYA/OneDrive/Desktop/CodeX%20Client%20(src)/src/main/resources/fabric.mod.json)
- Mixins:
  - [codex.mixins.json](/C:/Users/ADITYA/OneDrive/Desktop/CodeX%20Client%20(src)/src/main/resources/codex.mixins.json)

Configuration is stored in the Minecraft config directory:

- `codex-client.properties`
- `codex-gui.properties`

## Archive Notice

CodeX Client is no longer in production.

This repository is being published so the code remains accessible in a cleaner and more maintainable state than the original private working tree. Any fork that continues development should do so under its own maintenance, branding, and release responsibility.
