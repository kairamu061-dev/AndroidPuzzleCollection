pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AndroidPuzzleCollection"

include(":app")
include(":game-common")
include(":sudoku-core")
include(":slide-core")
include(":game2048-core")
include(":nonogram-core")
