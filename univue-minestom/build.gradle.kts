plugins {
    `java-library`
    `maven-publish`
}

val javaVersion = System.getenv("JAVA_VERSION") ?: "26"

dependencies {
    api(project(":univue-core"))
    // Same rationale as acmd-minestom: depend on Minestom directly for the
    // inventory/event/item APIs; never pull it transitively.
    compileOnly("net.minestom:minestom:2026.05.17c-26.1.1")
    // MinecraftServer.LOGGER is slf4j; Minestom is compileOnly so its slf4j
    // does not ride along transitively.
    compileOnly("org.slf4j:slf4j-api:2.0.16")
    compileOnly("org.jetbrains:annotations:26.0.2")

    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.jar {
    manifest {
        attributes["Automatic-Module-Name"] = "dev.terrorify.univue.minestom"
    }
}

tasks.withType<Test> { useJUnitPlatform() }

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String
        }
    }
    repositories {
        val mavenUrl = System.getenv("MAVEN_URL")
        if (mavenUrl != null) {
            maven {
                name = "envmaven"
                url = uri(mavenUrl)
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
        }
        mavenLocal()
    }
}
