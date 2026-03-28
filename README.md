# VEXTIQ PRO v1 — Kotlin Edition

## 🎮 Gaming Optimizer with Compose Desktop

Modern, native desktop application built with Kotlin and Jetpack Compose.

---

## 📦 Requirements

- **JDK 17+** — Download from https://adoptium.net/
- **Gradle** — Included (gradlew)

---

## 🚀 Quick Start

### Run in Development Mode
```bash
./gradlew run
```
Or on Windows:
```bash
gradlew.bat run
```

### Build EXE
```bash
./gradlew packageExe
```
Output: `build/compose/binaries/main/exe/`

---

## 🎨 Features

- ⚡ **Smart Boost** — One-click game optimization
- 📊 **Real-time Stats** — CPU, GPU, RAM monitoring (OSHI library)
- 🎯 **Game Profiles** — 11 games including Battlefield 6
- 💻 **Native UI** — Compose Desktop with cyber theme
- 🖥️ **Cross-platform** — Windows, macOS, Linux

---

## 📁 Project Structure

```
src/main/kotlin/com/vextiq/
├── Main.kt              # Application entry + UI pages
├── core/
│   ├── Games.kt         # Game definitions
│   └── SystemMonitor.kt # OSHI-based monitoring
├── optimizer/
│   └── Optimizer.kt     # Optimization logic
└── ui/
    ├── Theme.kt         # Cyber color scheme
    └── Components.kt    # Reusable UI components
```

---

## 🎮 Supported Games

- 🚀 Star Citizen
- ⚔️ Battlefield 6 (NEW!)
- 🎖️ Battlefield 2042
- 🌃 Cyberpunk 2077
- 🚗 GTA V
- 🎯 Valorant
- 💣 CS2
- 🔧 Rust
- 🎒 Escape from Tarkov
- 🪖 Call of Duty Warzone
- 🏗️ Fortnite

---

## 🔧 Technologies

- **Kotlin 1.9** — Modern JVM language
- **Compose Desktop 1.5** — Declarative UI
- **OSHI 6.4** — Native system monitoring
- **Gradle 8.5** — Build system

---

Made with 💙 by VEXTIQ Team
