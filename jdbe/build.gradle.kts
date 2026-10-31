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

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // opening a real experiment (experiments/*.er) needs far more than the default 512 MB
    maxHeapSize = "4g"
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
