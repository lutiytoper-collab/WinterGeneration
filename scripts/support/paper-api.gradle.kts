plugins { `java-library` }
repositories { mavenCentral() }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
sourceSets.main {
    java.srcDirs("upstream/paper-api/src/main/java", "upstream/paper-api/src/generated/java", "upstream/brigadier/src/main/java")
    resources.srcDirs("upstream/paper-api/src/main/resources")
}
dependencies {
    api("com.google.guava:guava:33.3.1-jre")
    api("com.google.code.gson:gson:2.11.0")
    api("org.yaml:snakeyaml:2.2")
    api("org.joml:joml:1.10.8")
    api("it.unimi.dsi:fastutil:8.5.15")
    api("org.apache.logging.log4j:log4j-api:2.24.1")
    api("org.slf4j:slf4j-api:2.0.16")
    api("net.md-5:bungeecord-chat:1.21-R0.2")
    api(platform("net.kyori:adventure-bom:4.26.1"))
    api("net.kyori:adventure-api")
    api("net.kyori:adventure-text-minimessage")
    api("net.kyori:adventure-text-serializer-gson")
    api("net.kyori:adventure-text-serializer-legacy")
    api("net.kyori:adventure-text-serializer-plain")
    api("net.kyori:adventure-text-logger-slf4j")
    api("org.apache.maven:maven-resolver-provider:3.9.6")
    implementation("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.18")
    implementation("org.apache.maven.resolver:maven-resolver-transport-http:1.9.18")
    compileOnly("org.jetbrains:annotations:26.0.2")
    api("org.checkerframework:checker-qual:3.49.2")
    api("org.jspecify:jspecify:1.0.0")
}
tasks.jar { archiveFileName.set("paper-api-1.21.11-source-build.jar") }
tasks.register<Copy>("compileDependencies") { from(configurations.compileClasspath); into(layout.buildDirectory.dir("compile-deps")) }
