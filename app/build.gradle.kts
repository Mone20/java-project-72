plugins {
    id("java")
}

group = "hexlet.code"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("gg.jte:jte:3.1.9") // Exception in thread "main" java.lang.NoClassDefFoundError: gg/jte/resolve/DirectoryCodeResolver
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("io.javalin:javalin:6.1.3")
    implementation("io.javalin:javalin-bundle:6.1.3")
    implementation("io.javalin:javalin-rendering:6.1.3")

}

tasks.test {
    useJUnitPlatform()
}