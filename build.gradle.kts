plugins {
    kotlin("jvm") version "1.9.23"
    id("io.papermc.paperweight.userdev") version "1.7.1"
    id("xyz.jpenilla.run-paper") version "2.3.0"
}

group = "de.c4vxl"
version = "1.0-RV-B1"

repositories {
    mavenCentral()
}

dependencies {
    paperweight.paperDevBundle("1.21-R0.1-SNAPSHOT")
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "21"
    }
    jar {
        archiveFileName.set("Vehicles.jar")
    }

    val copyJar by creating(Copy::class) {
        dependsOn(jar)
        from(file("build/libs/Vehicles.jar"))
        into(file("test_server/plugins/"))
    }

    build {
        dependsOn(copyJar)
    }
}