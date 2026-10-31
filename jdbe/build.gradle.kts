plugins {
    `java-library`
}

group = "org.gprofng.analyzer"
version = "3.0-SNAPSHOT"

java {
    // The engine uses Java 22+ features (unnamed catch variables, java.lang.foreign)
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// Standalone engine for remote use: java -jar jdbe.jar -IPC
tasks.jar {
    archiveBaseName = "jdbe"
    manifest {
        attributes["Main-Class"] = "org.mazurov.jdbe.ERPrint"
    }
}

// Replays a recorded GUI session (SP_ANALYZER_LOG_IPC=5) through a separate
// process and the embedded engine and compares the responses.
// Usage: ./gradlew :jdbe:parityCheck -Plog=<experiment.er | ipc.log>
tasks.register<JavaExec>("parityCheck") {
    group = "verification"
    description = "Process-vs-embedded engine parity check"
    classpath = sourceSets["main"].runtimeClasspath + sourceSets["test"].runtimeClasspath
    mainClass = "org.mazurov.jdbe.ParityCheck"
    dependsOn("testClasses")
    args(providers.gradleProperty("log").map { listOf(it) }.orElse(emptyList()))
}
