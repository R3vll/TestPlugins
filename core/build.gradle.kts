plugins {
    kotlin("jvm")
}

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
    ivy {
        url = uri("https://github.com/recloudstream/cloudstream/releases/download")
        patternLayout {
            artifact("[revision]/classes.jar")
        }
        metadataSources { artifact() }
    }
}

dependencies {
    compileOnly("com.lagradot:cloudstream3:pre-release")
    testImplementation("com.lagradot:cloudstream3:pre-release")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}
