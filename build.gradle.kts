subprojects {
    group = "dev.terrorify"
    version = System.getenv("UNIVUE_VERSION") ?: "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}
