plugins {
    `kotlin-dsl`
}

repositories {
    // Canonical Central host — the repo.maven.apache.org mirror used by
    // mavenCentral() resolves unreliably on some networks.
    maven { url = uri("https://repo1.maven.org/maven2") }
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-allopen:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-noarg:2.0.21")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:3.3.5")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.6")
}
