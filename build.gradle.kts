import org.cadixdev.gradle.licenser.LicenseExtension

plugins {
    id("java")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.cadixdev.licenser") version "0.6.1"
}

group = "org.enginehub"
version = "1.0-SNAPSHOT"

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven {
        name = "sponge"
        url = uri("https://repo.spongepowered.org/maven")
    }
    maven {
        url = uri("https://maven.enginehub.org/repo/")
    }
}

configure<LicenseExtension> {
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

val pistonVersion = "0.5.7";

dependencies {
    implementation("net.dv8tion:JDA:5.0.0-alpha.3")
    implementation("org.spongepowered:configurate-hocon:3.7.2")
    implementation("com.typesafe:config:1.4.1")
    implementation("org.enginehub.piston:core:${pistonVersion}")
    runtimeOnly("org.enginehub.piston.core-ap:runtime:${pistonVersion}")
    implementation("org.enginehub.piston:default-impl:${pistonVersion}")
    implementation("org.enginehub.piston.core-ap:annotations:${pistonVersion}")
    annotationProcessor("org.enginehub.piston.core-ap:processor:${pistonVersion}")
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.13.0"))
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    testImplementation("junit:junit:4.13.2")
}

configure<PublishingExtension> {
    publications {
        register<MavenPublication>("java") {
            from(components["java"])
        }
    }
}

tasks.named("assemble") {
    dependsOn("shadowJar")
}
