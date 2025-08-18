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
 * This is a blueprint Gradle build.gradle.kts file used by Rubens Gomes during the creation of a
 * new Gradle Spring Boot Java development project.
 *
 * @author [Rubens Gomes](https://rubensgomes.com)
 */
plugins {
  id("idea")
  id("jacoco")
  id("java")
  id("java-library")
  id("maven-publish")
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

// --------------- >>> gradle properties <<< ----------------------------------
// required in configuration of: publishing publications
val artifact: String by project
val title: String by project
// required in configuration of: publishing publications license
val license: String by project
val licenseUrl: String by project
// required in configuration of: publishing publications developer
val developerEmail: String by project
val developerId: String by project
val developerName: String by project
// required in configuration of: publishing publications scm
val scmConnection: String by project
val scmUrl: String by project
// required in configuration of: publishing repositories maven
val repsyUrl: String by project
// required in configuration of: publishing repositories credentials
// REPSY_USERNAME must be defined as an environment variable
// REPSY_PASSWORD must be defined as an environment variable
val repsyUsername: String? = System.getenv("REPSY_USERNAME")
val repsyPassword: String? = System.getenv("REPSY_PASSWORD")
// required in configuration of: sonar properties
// SONAR_TOKEN must be defined as an environment variable
val sonarKey = project.findProperty("sonar.projectKey") as String
val sonarOrg = project.findProperty("sonar.organization") as String
val sonarUrl = project.findProperty("sonar.host.url") as String

project.group = project.findProperty("group") as String

project.description = project.findProperty("description") as String

// --------------- >>> repositories <<< ---------------------------------------

repositories {
  // Use Maven Central for resolving dependencies.
  mavenCentral()
}

// --------------- >>> dependencies <<< ---------------------------------------
dependencies {
  // ########## compileOnly ##################################################
  compileOnly("org.projectlombok:lombok")

  // ########## developmentOnly ##############################################
  developmentOnly("org.springframework.boot:spring-boot-devtools")

  // ########## implementation ###############################################
  // spring boot starter dependencies
  api("org.springframework.boot:spring-boot-starter-validation")
  api("org.springframework.boot:spring-boot-starter-web")
  // TODO Add more Spring Boot starters as needed

  // other third-party libs
  api("org.apache.commons:commons-lang3")

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
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
  withSourcesJar()
  withJavadocJar()
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
    vendor.set(JvmVendorSpec.AMAZON)
  }
}

tasks.jar {
  dependsOn("sonar")
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
                "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})"))
  }
}

tasks.compileJava {
  // Ensure we have a clean code prior to compilateion
  dependsOn("spotlessApply")
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
// --------------- >>> Gradle Maven Publish Plugin <<< ------------------------
// NOTE: This section is dedicated to configuring the maven-publich plugin.
// ----------------------------------------------------------------------------
// https://docs.gradle.org/current/userguide/publishing_maven.html

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      versionMapping {
        usage("java-api") { fromResolutionOf("runtimeClasspath") }
        usage("java-runtime") { fromResolutionResult() }
      }

      groupId = project.group.toString()
      artifactId = artifact
      version = project.version.toString()

      from(components["java"])

      pom {
        name = title
        inceptionYear = "2025"
        packaging = "jar"

        licenses {
          license {
            name = license
            url = licenseUrl
          }
        }

        developers {
          developer {
            id = developerId
            name = developerName
            email = developerEmail
          }
        }

        scm {
          connection = scmConnection
          developerConnection = scmConnection
          url = scmUrl
        }
      }
    }
  }

  repositories {
    maven {
      url = uri(repsyUrl)
      credentials {
        username = repsyUsername
        password = repsyPassword
      }
    }
  }
}

// ----------------------------------------------------------------------------
// --------------- >>> com.diffplug.spotless Plugin <<< -----------------------
// NOTE: This section is dedicated to configuring the spotless plugin.
// ----------------------------------------------------------------------------
// https://github.com/diffplug/spotless

spotless {
  java {
    target("src/**/*.java")

    // Use Google Java Format
    googleJavaFormat()

    // Remove unused imports
    removeUnusedImports()

    licenseHeader(
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
             * limitations under the License.
             */
        """
            .trimIndent())

    // Custom import order
    importOrder("java", "javax", "org", "com", "")

    // Trim trailing whitespace
    trimTrailingWhitespace()

    // End with newline
    endWithNewline()
  }

  // Format Kotlin files (if you add any)
  kotlin {
    ktfmt()
    trimTrailingWhitespace()
    endWithNewline()
  }

  // Format Gradle Kotlin DSL build file
  kotlinGradle {
    target("*.gradle.kts")
    trimTrailingWhitespace()
    endWithNewline()
    ktfmt()
  }
}

// ----------------------------------------------------------------------------
// --------------- >>> net.researchgate.release Plugin <<< --------------------
// NOTE: This section is dedicated to configuring the release plugin.
// ----------------------------------------------------------------------------
// https://github.com/researchgate/gradle-release

release {
  // Git configuration
  git {
    requireBranch.set("main")
    pushToRemote.set("origin")
    pushToBranchPrefix.set("")
  }

  // Version management
  versionPropertyFile.set("gradle.properties")
  tagTemplate.set("v${version}")

  // Pre-release tasks
  preTagCommitMessage.set("Pre-tag commit: ")
  tagCommitMessage.set("Creating tag: ")
  newVersionCommitMessage.set("New version: ")

  // Fail on updateNeeded
  failOnUpdateNeeded.set(true)
}

// ----------------------------------------------------------------------------
// --------------- >>> org.sonarqube Plugin <<< -------------------------------
// NOTE: This section is dedicated to configuring the sonarqube plugin.
// ----------------------------------------------------------------------------
// https://docs.sonarsource.com/sonarqube-server/latest/analyzing-source-code/scanners/sonarscanner-for-gradle/

sonar {
  properties {
    // SONAR_TOKEN must be defined as an environment variable
    property("sonar.projectKey", sonarKey)
    property("sonar.organization", sonarOrg)
    property("sonar.host.url", sonarUrl)
    property("sonar.sources", "src/main/java")
    property("sonar.tests", "src/test/java")
    property("sonar.exclusions", "**/generated/**")
    property("sonar.java.source", "21")
    property(
        "sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
  }
}

// task.check includes jacocoTestReport
tasks.sonar { dependsOn("jacocoTestReport") }

// tasks.sonar { dependsOn("check") }

// ----------------------------------------------------------------------------
// --------------- >>> org.springframework.boot Plugin <<< --------------------
// NOTE: This section is dedicated to configuring the Spring Boot plugin.
// ----------------------------------------------------------------------------
// https://docs.spring.io/spring-boot/gradle-plugin/index.html

// Disable the bootJar task since this is a library
tasks.bootJar { enabled = false }

// ----------------------------------------------------------------------------
// --------------- >>> Print Config Vars Task <<< -----------------------------
// ----------------------------------------------------------------------------
tasks.register("printConfigVars") {
  group = "build"
  description = "Prints variables configured in the project."

  doLast {
    println("artifactId: $artifact")
    println("title: $title")
    println("version: $version")
    println("group: $project.group")
    // description is derived from project.description
    println("description: $description")
    println("license: $license")
    println("licenseUrl: $licenseUrl")
    println("developerEmail: $developerEmail")
    println("developerId: $developerId")
    println("developerName: $developerName")
    println("scmConnection: $scmConnection")
    println("scmUrl: $scmUrl")
    println("repsyUrl: $repsyUrl")
    println("repsyUsername: $repsyUsername")
    println("repsyPassword: $repsyPassword")
  }
}
