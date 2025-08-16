/*
 * Copyright 2025 Rubens Gomes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the__LICENSE] [1].
 */

/**
 * This is a blueprint Gradle build.gradle.kts file used by Rubens Gomes
 * during the creation of a new Gradle Spring Boot Java development project.
 *
 * @author [Rubens Gomes](https://rubensgomes.com)
 */

plugins {
    id("idea")
    id("jacoco")
    id("java")
    id("version-catalog")
    // net.researchgate.release
    alias(libs.plugins.release)
    // org.sonarqube
    alias(libs.plugins.sonarqube)
    // com.diffplug.spotless
    alias(libs.plugins.spotless)
    // org.springframework.boot
    alias(libs.plugins.spring.boot)
    // io.spring.dependency-management
    alias(libs.plugins.spring.dependency.management)
    // com.dorongold.task-tree
    alias(libs.plugins.task.tree)
}

// --------------- >>> constants <<< ------------------------------------------
// constant being used in this build script.
//  gradle.properties:
val ARTIFACT = providers.gradleProperty("artifact").get() as String
val MAIN_CLASS = providers.gradleProperty("mainClass").get() as String
val SONAR_KEY = project.findProperty("sonar.projectKey") as String
val SONAR_ORG = project.findProperty("sonar.organization") as String
val SONAR_URL = project.findProperty("sonar.host.url") as String

// --------------- >>> repositories <<< ---------------------------------------

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

// --------------- >>> dependencies <<< ---------------------------------------

dependencies {
    // ########## compileOnly ##################################################

    // ########## implementation ###############################################
    // spring boot starter dependencies
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    // TODO Add more Spring Boot starters as needed

    // ########## developmentOnly ##############################################
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // ########## runtimeOnly ##################################################
    // TODO Add runtime dependencies as needed

    // ########## testImplementation ###########################################
    // TODO Add more test compile dependencies as needed
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // ########## testRuntimeOnly #############################################
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // TODO Add more test runtime dependencies as needed
}

// ----------------------------------------------------------------------------
// --------------- >>> Gradle Base Plugin <<< ---------------------------------
// NOTE: This section is dedicated to configuring the Gradle base plugin.
// ----------------------------------------------------------------------------
// https://docs.gradle.org/current/userguide/base_plugin.html

// run sonar independently since it requires a remote connection to sonarcloud.io
// tasks.check { dependsOn("sonar") }

// ----------------------------------------------------------------------------
// --------------- >>> Gradle IDEA Plugin <<< ---------------------------------
// NOTE: This section is dedicated to configuring the Idea plugin.
// ----------------------------------------------------------------------------
// https://docs.gradle.org/current/userguide/idea_plugin.html

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

// ----------------------------------------------------------------------------
// --------------- >>> Gradle jaCoCo Plugin <<< -------------------------------
// NOTE: This section is dedicated to configuring the jacoco plugin.
// ----------------------------------------------------------------------------
// https://docs.gradle.org/current/userguide/jacoco_plugin.html

tasks.jacocoTestReport {
    // tests are required to run before generating the report
    dependsOn(tasks.test)
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        csv.required = false
        html.outputLocation = layout.buildDirectory.dir("jacocoHtml")
    }
}

// ----------------------------------------------------------------------------
// --------------- >>> Gradle Java Plugin <<< ---------------------------------
// NOTE: This section is dedicated to configuring the Java plugin.
// ----------------------------------------------------------------------------
// https://docs.gradle.org/current/userguide/java_plugin.html
java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.AMAZON)
    }
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Specification-Title" to project.properties["title"],
                "Implementation-Title" to project.properties["artifact"],
                "Implementation-Version" to project.properties["version"],
                "Implementation-Vendor" to project.properties["developerName"],
                "Built-By" to project.properties["developerId"],
                "Build-Jdk" to System.getProperty("java.home"),
                "Created-By" to
                        "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})"
            )
        )
    }
}

tasks.compileJava {
    // Ensure we have a clean code prior to compilateion
    dependsOn("spotlessApply")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

// ----------------------------------------------------------------------------
// --------------- >>> Gradle JVM Test Suite Plugin <<< -----------------------
// NOTE: This section is dedicated to configuring the JVM Test Suite plugin.
// ----------------------------------------------------------------------------
// https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html

tasks.test {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
    // WARNING: If a serviceability tool is in use, please run with
    // -XX:+EnableDynamicAgentLoading to hide this warning
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    // report is always generated after tests run
    finalizedBy(tasks.jacocoTestReport)
}

// ----------------------------------------------------------------------------
// --------------- >>> com.diffplug.spotless Plugin <<< -----------------------
// NOTE: This section is dedicated to configuring the spotless plugin.
// ----------------------------------------------------------------------------
// https://github.com/diffplug/spotless

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.17.0")
    }

    kotlinGradle {
        target("*.gradle.kts")
        ktfmt()
    }
}

// ----------------------------------------------------------------------------
// --------------- >>> net.researchgate.release Plugin <<< --------------------
// NOTE: This section is dedicated to configuring the release plugin.
// ----------------------------------------------------------------------------
// https://github.com/researchgate/gradle-release

release {
    with(git) {
        pushReleaseVersionBranch.set("release")
        requireBranch.set("main")
    }
}

// ----------------------------------------------------------------------------
// --------------- >>> org.sonarqube Plugin <<< -------------------------------
// NOTE: This section is dedicated to configuring the sonarqube plugin.
// ----------------------------------------------------------------------------
// https://docs.sonarsource.com/sonarqube-server/latest/analyzing-source-code/scanners/sonarscanner-for-gradle/

sonar {
    properties {
        // SONAR_TOKEN must be defined as an environment variable
        property("sonar.projectKey", SONAR_KEY)
        property("sonar.organization", SONAR_ORG)
        property("sonar.host.url", SONAR_URL)
    }
}

// task.check includes jacocoTestReport
// tasks.sonar { dependsOn("jacocoTestReport") }
// tasks.sonar { dependsOn("check") }

// ----------------------------------------------------------------------------
// --------------- >>> org.springframework.boot Plugin <<< --------------------
// NOTE: This section is dedicated to configuring the Spring Boot plugin.
// ----------------------------------------------------------------------------
// https://docs.spring.io/spring-boot/gradle-plugin/index.html

springBoot {
    mainClass.set("$MAIN_CLASS")
}

tasks.bootJar {
    // layered.enabled.set(false)
    layered.enabled.set(true)
    dependsOn("check")
    manifest { attributes("Start-Class" to "$MAIN_CLASS") }
}

// ----------------------------------------------------------------------------
// --------------- >>> Add Copyright Task <<< ---------------------------------
// ----------------------------------------------------------------------------

tasks.register("addCopyright") {
    val copyright =
        """
/*
 * Copyright 2025 Rubens Gomes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the__LICENSE] [1].
 */        
    """
            .trimIndent()

    val srcDir = file("src")

    doLast {
        srcDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .forEach { file ->
                val lines = file.readLines()
                if (lines.isNotEmpty() && lines[0].contains("Copyright")) {
                    return@forEach
                }
                file.writeText(copyright + "\n" + file.readText())
                println("Added copyright to: ${file.path}")
            }
    }
}
