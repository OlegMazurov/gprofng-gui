plugins {
    id("java")
}

group = "org.gprofng.analyzer"
version = "3.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // The JDBE engine, run on a thread of the Analyzer's JVM for local analysis.
    implementation(project(":jdbe"))
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "org.gprofng.analyzer.AnMain"
        // jdbe.jar is deployed next to this jar
        attributes["Class-Path"] = "jdbe.jar"
    }
}
