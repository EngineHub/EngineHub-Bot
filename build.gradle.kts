import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("org.enginehub.crankcase.java") version "0.1.2"
    id("org.enginehub.crankcase.licensing") version "0.1.2"
    id("com.gradleup.shadow") version "9.6.1"
}

group = "org.enginehub"
version = "1.0-SNAPSHOT"

crankcaseJava {
    javaRelease = 21
    disabledLints = listOf("processing")
    disabledErrorprone = listOf(
        "CatchAndPrintStackTrace",
        "FutureReturnValueIgnored",
    )
}

tasks.compileJava {
    options.compilerArgs.add("-Aarg.name.key.prefix=")
    options.errorprone.excludedPaths = ".*/build/generated/.*"
}

tasks.jar {
    archiveClassifier = "dev"
}

tasks.shadowJar {
    archiveClassifier = ""

    exclude("GradleStart**")
    exclude(".cache")
    exclude("LICENSE*")
    exclude("META-INF/maven/**")

    manifest.attributes(mapOf("Multi-Release" to "true"))
}

dependencies {
    implementation("net.dv8tion:JDA:6.1.2") {
        exclude(module="opus-java")
    }

    implementation("org.spongepowered:configurate-hocon:3.7.2")

    implementation("com.typesafe:config:1.4.2")

    val pistonVersion = "0.5.7"
    implementation("org.enginehub.piston:core:${pistonVersion}")
    implementation("org.enginehub.piston:default-impl:${pistonVersion}")
    implementation("org.enginehub.piston.core-ap:annotations:${pistonVersion}")
    annotationProcessor("org.enginehub.piston.core-ap:processor:${pistonVersion}")
    runtimeOnly("org.enginehub.piston.core-ap:runtime:${pistonVersion}")

    implementation(platform("com.fasterxml.jackson:jackson-bom:2.13.2.20220328"))
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names")

    val slf4jVersion = "1.7.36"
    val log4jVersion = "2.17.2"
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

    implementation("net.sourceforge.tess4j:tess4j:5.9.0") {
        exclude(group = "log4j", module = "log4j")
        exclude(group = "commons-logging", module = "commons-logging")
    }
}
