
plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.nexora"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.ccbluex.net/snapshots")
}

dependencies {
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("net.ccbluex:MinecraftAuth:5.0.3-SNAPSHOT")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

javafx {
    version = "21.0.7"
    modules("javafx.controls")
}

application {
    mainClass.set("com.nexora.launcher.LauncherStart")
}


tasks.register<Exec>("buildNexoraSetup") {
    dependsOn("installDist")

    doFirst {
        val javaHome = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        }.get().metadata.installationPath.asFile

        val inputDir = layout.buildDirectory
            .dir("install/${project.name}/lib")
            .get().asFile

        val outputDir = layout.buildDirectory
            .dir("nexora-installer")
            .get().asFile

        outputDir.mkdirs()

        println("Operating system: " + System.getProperty("os.name"))
        println("jpackage path: " + javaHome.resolve("bin/jpackage.exe").absolutePath)

        commandLine(
            javaHome.resolve("bin/jpackage.exe").absolutePath,
            "--type", "exe",
            "--name", "NexoraLauncher",
            "--input", inputDir.absolutePath,
            "--main-jar", "${project.name}-${project.version}.jar",
            "--main-class", "com.nexora.launcher.LauncherStart",
            "--dest", outputDir.absolutePath,
            "--app-version", "1.0.0",
            "--vendor", "Nexora",
            "--win-menu",
            "--win-shortcut",
        )
    }
}
