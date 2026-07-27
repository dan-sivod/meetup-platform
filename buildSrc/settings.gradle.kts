pluginManagement {
    repositories {
        // repo1.maven.org is the canonical Central host; the default
        // repo.maven.apache.org mirror resolves unreliably on some networks.
        maven { url = uri("https://repo1.maven.org/maven2") }
        gradlePluginPortal()
    }
}
