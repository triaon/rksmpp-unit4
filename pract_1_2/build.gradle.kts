plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    application
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("Task1Kt")
}

// Задание 1: ./gradlew runTask1 [--args="fail"]
tasks.register<JavaExec>("runTask1") {
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Task1Kt")
    workingDir = projectDir
}

// Задание 2: ./gradlew runTask2 [--args="test_dir 5000"]
tasks.register<JavaExec>("runTask2") {
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Task2Kt")
    workingDir = projectDir
}
