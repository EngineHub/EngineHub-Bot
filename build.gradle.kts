plugins {
    id("java")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.cadixdev.licenser") version "0.6.1"
}

group = "org.enginehub"
version = "1.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://m2.dv8tion.net/releases")
    }
    maven {
        name = "sponge"
        url = uri("https://repo.spongepowered.org/maven")
    }
    maven {
        url = uri("https://maven.enginehub.org/repo/")
    }
}

configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:18.0")
    }
}

license {
    newLine(false)
    header(project.file("HEADER.txt"))
    include("**/*.java")
}

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs = listOf("-parameters")
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("dev")
}

tasks.named<Jar>("shadowJar") {
    archiveClassifier.set("")
}

dependencies {
    implementation("net.dv8tion:JDA:4.4.0_352")
    implementation("org.spongepowered:configurate-hocon:3.7.2")
    implementation("com.typesafe:config:1.4.1")
    implementation("com.sk89q:intake:3.1.2")
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.13.0"))
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names")
    testImplementation("junit:junit:4.13.2")
}

publishing {
    publications {
        register<MavenPublication>("java") {
            from(components["java"])
        }
    }
}

tasks.named("assemble") {
    dependsOn("shadowJar")
}
