plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

sourceSets {
    test {
        kotlin.srcDir("$rootDir/tests/game2048-core")
    }
}

dependencies {
    testImplementation(libs.junit)
}
