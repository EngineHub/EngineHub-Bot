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
    options.compilerArgs = listOf("-parameters", "-Werror", "-Aarg.name.key.prefix=")
    options.encoding = "UTF-8"
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("dev")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")

    exclude("GradleStart**")
    exclude(".cache")
    exclude("LICENSE*")
    exclude("META-INF/maven/**")

    manifest.attributes(mapOf("Multi-Release" to "true"))
}

dependencies {
    implementation("net.dv8tion:JDA:5.0.0-alpha.4")

    implementation("org.spongepowered:configurate-hocon:3.7.2")

    implementation("com.typesafe:config:1.4.1")

    val pistonVersion = "0.5.7"
    implementation("org.enginehub.piston:core:${pistonVersion}")
    implementation("org.enginehub.piston:default-impl:${pistonVersion}")
    implementation("org.enginehub.piston.core-ap:annotations:${pistonVersion}")
    annotationProcessor("org.enginehub.piston.core-ap:processor:${pistonVersion}")
    runtimeOnly("org.enginehub.piston.core-ap:runtime:${pistonVersion}")

    implementation(platform("com.fasterxml.jackson:jackson-bom:2.13.0"))
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names")

    val slf4jVersion = "1.7.32"
    val log4jVersion = "2.17.0"
    // Primarily prefer Log4J for logging.
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    // Bind SLF4J over STDOUT [JDA]
    runtimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")
    // Bind Log4J over SLF4J [Piston, etc.]
    runtimeOnly("org.apache.logging.log4j:log4j-to-slf4j:$log4jVersion")

    constraints {
        implementation("org.slf4j:slf4j-api:$slf4jVersion")
    }

    implementation("org.apache.commons:commons-text:1.9")

    implementation("net.sourceforge.tess4j:tess4j:5.0.0") {
        exclude(group = "log4j", module = "log4j")
        exclude(group = "commons-logging", module = "commons-logging")
    }

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
