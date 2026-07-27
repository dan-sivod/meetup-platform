plugins {
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    // Canonical Central host — the repo.maven.apache.org mirror used by
    // mavenCentral() resolves unreliably on some networks.
    maven { url = uri("https://repo1.maven.org/maven2") }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    "testImplementation"("org.jetbrains.kotlin:kotlin-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
