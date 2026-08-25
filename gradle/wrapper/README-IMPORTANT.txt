gradle-wrapper.jar is intentionally NOT included in this checkout.

This repo was assembled in a sandboxed environment with no network access,
so `gradle wrapper` (the command that downloads/generates this binary jar)
could not be run here.

Before using ./gradlew, generate it yourself, once, with any local Gradle
install (Homebrew `brew install gradle`, SDKMAN `sdk install gradle`, or
your IDE's bundled Gradle all work):

    gradle wrapper --gradle-version 8.10

That regenerates gradle/wrapper/gradle-wrapper.jar (and will overwrite
gradle-wrapper.properties / gradlew / gradlew.bat with the same content
already committed here). Commit the resulting jar - it's meant to be
checked into version control, unlike most binaries.

Alternatively, if you already have Gradle installed, you don't need the
wrapper at all: just run `gradle build` / `gradle test` directly instead of
`./gradlew`.
