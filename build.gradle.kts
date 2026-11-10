plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// Kotlin code style for every module, configured in .editorconfig. ktlint runs as a command-line
// tool because its Gradle plugin does not see the Kotlin sources of AGP 9 (built-in Kotlin).
val ktlint: Configuration = configurations.create("ktlint")

dependencies {
    ktlint(libs.ktlint.cli) {
        attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL)) }
    }
}

val kotlinSources = listOf("*/src/**/*.kt", "**/*.kts", "!**/build/**")

val ktlintCheck = tasks.register<JavaExec>("ktlintCheck") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Checks the Kotlin code style with ktlint."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    args(kotlinSources)
    inputs.files(
        fileTree(rootDir) {
            include("*/src/**/*.kt", "**/*.kts", ".editorconfig")
            exclude("**/build/**")
        },
    )
    outputs.upToDateWhen { true }
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Fixes the Kotlin code style with ktlint where it can."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    args(listOf("-F") + kotlinSources)
}

tasks.check { dependsOn(ktlintCheck) }
