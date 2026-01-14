plugins {
    id("java")
    id("application")
    id("checkstyle")
}

group = "ch.geowerkstatt.xtfdifftool"

repositories {
    mavenCentral()
    maven { url = uri("https://jars.interlis.ch") }
}

dependencies {
    implementation("ch.interlis:iox-ili:1.24.4")
    implementation("ch.interlis:ili2c-core:5.6.8")

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