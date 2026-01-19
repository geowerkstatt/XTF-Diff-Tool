plugins {
    id("java")
    id("application")
    id("checkstyle")
}

group = "ch.geowerkstatt.xtfdifftool"

repositories {
    mavenCentral()
}

dependencies {
    implementation("commons-cli:commons-cli:1.11.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "ch.geowerkstatt.xtfdifftool.Main"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(" ") { file -> file.name }
        attributes["Implementation-Version"] = version
    }
}

tasks.test {
    useJUnitPlatform()
}