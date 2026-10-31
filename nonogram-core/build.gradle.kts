plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

sourceSets {
    test {
        kotlin.srcDir("$rootDir/tests/nonogram-core")
    }
}

dependencies {
    testImplementation(libs.junit)
}
