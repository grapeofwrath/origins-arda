plugins {
    java
}

group = "dev.arda"
version = "1.0.0"

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    // Match your server's Minecraft version (Paper project-setup docs use 26.3.build.+)
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")

    // Origins-Reborn has no public Maven coordinates in the docs: drop the jar from your
    // server's plugins/ folder into addon/libs/ and compile against it.
    compileOnly(files("libs/Origins-Reborn.jar"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}
