plugins {
    kotlin("jvm") version "2.3.20"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

javafx {
    version = "25"
    modules = listOf("javafx.controls", "javafx.graphics")
}

application {
    // A plain launcher (not an Application subclass) avoids the
    // "JavaFX runtime components are missing" error on non-modular runs.
    mainClass.set("MainKt")
}

tasks.test {
    useJUnitPlatform()
}
