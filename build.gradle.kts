plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    application
}

group = "com.ytranklab"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

application {
    mainClass.set("com.ytranklab.MainKt")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("io.ktor:ktor-client-core:3.1.3")
    implementation("io.ktor:ktor-client-cio:3.1.3")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation("org.yaml:snakeyaml:2.4")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("io.mockk:mockk:1.14.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(25)
}
