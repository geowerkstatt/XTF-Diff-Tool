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

    implementation("commons-cli:commons-cli:1.11.0")

    implementation(platform("tools.jackson:jackson-bom:3.0.3"))
    implementation("tools.jackson.core:jackson-databind")
    implementation("tools.jackson.core:jackson-core")

    implementation(platform("org.apache.logging.log4j:log4j-bom:2.25.3"))
    implementation("org.apache.logging.log4j:log4j-api")
    implementation("org.apache.logging.log4j:log4j-core")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.6")
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