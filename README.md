# 🚀 VEXTIQ PRO v1.1

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Desktop](https://img.shields.io/badge/Compose_Desktop-1.5-blue?logo=jetpack-compose)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Build](https://img.shields.io/badge/Build-Gradle_8.5-green?logo=gradle)](https://gradle.org)

**VEXTIQ PRO** is a premium, high-performance gaming optimizer designed for Windows, built with **Kotlin** and **Jetpack Compose Desktop**. It provides real-time system monitoring and one-click optimizations to ensure your hardware is fully dedicated to your gaming experience.

---

## ✨ What's New in v1.1

- 🧠 **Adaptive Optimization Agent**: Introducing the new `AdaptiveOptimizer` which monitors frame stability and dynamically adjusts system resources to prevent frame drops in real-time.
- 🎮 **New Game Profiles**: Added optimized profiles for **Battlefield 6**, **CS2**, and **Valorant**. 
- 🎯 **Advanced Process Detection**: Enhanced logic to correctly identify active games while ignoring background chatter like Discord.
- 💎 **Cyber-Theme UI Polish**: Refined "Cyber" design with smoother transitions and a more intuitive navigation layout.
- 🛠️ **Stability Suite**: Fixed several Kotlin compilation errors and optimized the MSI installer build process.

---

## 🎨 Key Features

- ⚡ **Smart Boost** — Instant system-wide optimization for low-latency gaming.
- 📊 **Real-time Analytics** — Professional-grade monitoring of CPU, GPU, and RAM usage via the OSHI library.
- 🎯 **11+ Custom Profiles** — Pre-tuned settings for the world's most popular competitive titles.
- 💻 **State-of-the-Art UI** — A modern, native desktop interface that looks and feels like the future of gaming.

---

## 📁 Project Architecture

```text
src/main/kotlin/com/vextiq/
├── Main.kt              # App entry point & Navigation
├── core/
│   ├── Games.kt         # Pre-configured game definitions
│   └── SystemMonitor.kt # Native monitoring logic (OSHI)
├── optimizer/
│   ├── Optimizer.kt     # Core hardware optimization
│   └── AdaptiveOptimizer.kt # [NEW] Real-time AI adjustment
└── ui/
    ├── Theme.kt         # Global Cyber design tokens
    └── Components.kt    # Premium UI elements
```

---

## 🚀 Quick Start

### Development Mode
```powershell
./gradlew.bat run
```

### Build MSI Installer
```powershell
./gradlew.bat packageMsi
```
The output will be available in: `build/compose/binaries/main/msi/`

---

## 📦 Requirements

- **JDK 17+** (Adoptium recommend)
- **Windows 10/11** (Recommended for MSI building)
- **Gradle 8.5** (Bundled via `gradlew`)

---

Made with 💙 by **VEXTIQ Team**
