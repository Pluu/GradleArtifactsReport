plugins {
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("artifactMonitoring") {
            id = "app.pluu.artifactMonitoring"
            implementationClass = "com.pluu.convention.PluuArtifactMonitoringConventionPlugin"
        }
    }
}
