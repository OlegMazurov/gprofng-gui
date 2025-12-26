plugins {
    id("java")
}

group = "org.gprofng.analyzer"
version = "2.3-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "org.gprofng.analyzer.AnMain"
    }
}
