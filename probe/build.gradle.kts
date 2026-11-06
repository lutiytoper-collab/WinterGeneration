plugins { java }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
repositories { maven("https://repo.papermc.io/repository/maven-public/") }
dependencies {
    val localApi = providers.gradleProperty("paperApiClasspath")
    if (localApi.isPresent) compileOnly(fileTree(localApi.get()) { include("libs/*.jar", "compile-deps/*.jar") })
    else compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}
tasks.jar { archiveFileName.set("WinterProbe.jar") }
