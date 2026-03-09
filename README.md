# CodeX Client

CodeX Client is a Fabric client for Minecraft `1.21.4`.

I am publishing this repository as an archival release.

CodeX Client is no longer in active production, and I am not planning any more official releases. Even so, I wanted the source to be left behind in a clean state instead of disappearing as a messy private worktree. If someone wants to study it, fork it, or keep building on it, they can.

## Current State

- Target version: Minecraft `1.21.4`
- Loader: Fabric
- Java: `21`
- Build layout: single Gradle project at repository root
- Status: archived public source release

## Original Idea

The original plan for CodeX was simple:

- build a ghost / legit semi-hacked Minecraft client
- keep it free for everyone
- grow it into something much bigger, with a lot more features, modules, polish, and quality over time
- push it closer to the scale of other well-known clients, but with its own identity

That was the real direction behind this project.

This repo is not the full vision. It is the cleaned version of what was actually built before production was dropped.

## What Is In This Repo

The project now lives as one root-level Gradle setup:

- [`build.gradle.kts`](build.gradle.kts)
- [`settings.gradle.kts`](settings.gradle.kts)
- [`src/main/java`](src/main/java)
- [`src/main/resources`](src/main/resources)
- [`src/test/java`](src/test/java)

The current archived build registers these modules:

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

What is here is exactly what this codebase currently implements. I am not presenting it as a finished commercial client or an actively maintained platform. It is the real state of the project at the point I decided to stop.

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

## Project Notes

- Main Fabric entrypoints:
  - [`CodeX.java`](src/main/java/com/codex/CodeX.java)
  - [`CodeXClient.java`](src/main/java/com/codex/client/CodeXClient.java)
- Fabric metadata:
  - [`fabric.mod.json`](src/main/resources/fabric.mod.json)
- Mixins:
  - [`codex.mixins.json`](src/main/resources/codex.mixins.json)

Configuration is stored in the Minecraft config directory:

- `codex-client.properties`
- `codex-gui.properties`

## Why It Is Public

I decided to stop production on CodeX Client.

Instead of leaving behind broken branches, old multi-version scaffolding, private leftovers, and abandoned integrations, I cleaned the repo up and released it properly.

If somebody wants to continue this idea, they can. Just do it under their own maintenance, branding, and release responsibility.
