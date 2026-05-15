import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.21"
    id("org.jetbrains.compose") version "1.5.11"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

group = "com.vextiq"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

// Force JDK 21 compatibility
kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("com.github.oshi:oshi-core:6.4.8")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

compose.desktop {
    application {
        mainClass = "com.vextiq.MainKt"
        
        nativeDistributions {
            // MSI + EXE installers - both Windows Installer formats with upgrade support
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "VEXTIQ PRO"
            packageVersion = "1.8.1"
            description = "VEXTIQ PRO Gaming Optimizer"
            vendor = "VEXTIQ"
            
            windows {
                console = false
                dirChooser = true
                perUserInstall = false
                menu = true
                menuGroup = "VEXTIQ"
                shortcut = true
                // CRITICAL: Same UUID = Windows auto-upgrades, prevents duplicate installs
                upgradeUuid = "4a5b6c7d-8e9f-0a1b-2c3d-4e5f6a7b8c9d"
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
        }
        
        buildTypes.release {
            proguard {
                isEnabled = false
            }
        }
    }
}

detekt {
    toolVersion = "1.23.4"
    config = files("detekt-config.yml")
}
