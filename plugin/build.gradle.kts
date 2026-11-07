plugins { java }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
repositories { maven("https://repo.papermc.io/repository/maven-public/") }
dependencies {
    implementation(project(":core"))
    val localApi = providers.gradleProperty("paperApiClasspath")
    if (localApi.isPresent) {
        compileOnly(fileTree(localApi.get()) { include("libs/*.jar", "compile-deps/*.jar") })
        testImplementation(fileTree(localApi.get()) { include("libs/*.jar", "compile-deps/*.jar") })
    } else {
        compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
        testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    }
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.ow2.asm:asm-tree:9.7.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.jar {
    archiveFileName.set("MidgardWinterWorld.jar")
    from(project(":core").sourceSets.main.get().output)
}

tasks.test {
    useJUnitPlatform()
    providers.gradleProperty("lunaJar").orNull?.let { systemProperty("luna.jar", it) }
}

// Native worldgen is part of the one-file distribution.
tasks.processResources { from(rootProject.file("storybook")) { into("fantasy-pack") } }
tasks.processResources {
    from(rootProject.file("storybook")) { exclude("data/minecraft/worldgen/placed_feature/*.json"); into("fantasy-terrain-pack") }
    from(rootProject.file("terrain")) { exclude("pack.mcmeta"); into("fantasy-terrain-pack") }
}
