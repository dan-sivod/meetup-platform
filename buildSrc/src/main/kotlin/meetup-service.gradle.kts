plugins {
    id("meetup-kotlin")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    "implementation"("org.springframework.boot:spring-boot-starter-actuator")
    "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
    "implementation"("org.jetbrains.kotlin:kotlin-reflect")
    "testImplementation"("org.springframework.boot:spring-boot-starter-test")
}
