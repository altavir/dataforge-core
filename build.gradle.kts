import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    val kotlinVersion: String by rootProject.extra("1.3.21")
    val ioVersion: String by rootProject.extra("0.1.5")
    val coroutinesVersion: String by rootProject.extra("1.1.1")
    val atomicfuVersion: String by rootProject.extra("0.12.1")
    val dokkaVersion: String by rootProject.extra("0.9.17")
    val serializationVersion: String by rootProject.extra("0.10.0")

    repositories {
        jcenter()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jfrog.buildinfo:build-info-extractor-gradle:4+")
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
        classpath("org.jetbrains.kotlin:kotlin-frontend-plugin:0.0.45")
        classpath("org.openjfx:javafx-plugin:0.0.7")
    }
}

plugins {
    id("com.jfrog.artifactory") version "4.8.1" apply false
//    id("org.jetbrains.kotlin.multiplatform") apply false
}

allprojects {
    apply(plugin = "maven")
    apply(plugin = "maven-publish")
    apply(plugin = "com.jfrog.artifactory")

    repositories {
        jcenter()
        maven("https://kotlin.bintray.com/kotlinx")
    }

    group = "hep.dataforge"
    version = "0.1.2-dev-1"

    // apply bintray configuration
    apply(from = "${rootProject.rootDir}/gradle/bintray.gradle")

    //apply artifactory configuration
    apply(from = "${rootProject.rootDir}/gradle/artifactory.gradle")

}

subprojects {

    //    dokka {
//        outputFormat = "html"
//        outputDirectory = javadoc.destinationDir
//    }
//
//    task dokkaJar (type: Jar, dependsOn: dokka) {
//           from javadoc . destinationDir
//            classifier = "javadoc"
//    }

    // Create empty jar for sources classifier to satisfy maven requirements
    val stubSources by tasks.registering(Jar::class) {
        archiveClassifier.set("sources")
        //from(sourceSets.main.get().allSource)
    }

    // Create empty jar for javadoc classifier to satisfy maven requirements
    val stubJavadoc by tasks.registering(Jar::class) {
        archiveClassifier.set("javadoc")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions{
            jvmTarget = "1.8"
        }
    }


    afterEvaluate {
        extensions.findByType<KotlinMultiplatformExtension>()?.apply {
            jvm {
                compilations.all {
                    kotlinOptions {
                        jvmTarget = "1.8"
                    }
                }
            }

            js {
                compilations.all {
                    tasks.getByName(compileKotlinTaskName) {
                        kotlinOptions {
                            metaInfo = true
                            sourceMap = true
                            sourceMapEmbedSources = "always"
                            moduleKind = "umd"
                        }
                    }
                }

                configure(listOf(compilations["main"])) {
                    tasks.getByName(compileKotlinTaskName) {
                        kotlinOptions {
                            main = "call"
                        }
                    }
                }
            }

            sourceSets {

                val commonMain by getting {
                    dependencies {
                        api(kotlin("stdlib"))
                    }
                }
                val commonTest by getting {
                    dependencies {
                        implementation(kotlin("test-common"))
                        implementation(kotlin("test-annotations-common"))
                    }
                }
                val jvmMain by getting {
                    dependencies {
                        api(kotlin("stdlib-jdk8"))
                    }
                }
                val jvmTest by getting {
                    dependencies {
                        implementation(kotlin("test"))
                        implementation(kotlin("test-junit"))
                    }
                }
                val jsMain by getting {
                    dependencies {
                        api(kotlin("stdlib-js"))
                    }
                }
                val jsTest by getting {
                    dependencies {
                        implementation(kotlin("test-js"))
                    }
                }
            }

            targets.all {
                sourceSets.all {
                    languageSettings.progressiveMode = true
                }
            }

            configure<PublishingExtension> {

                publications.filterIsInstance<MavenPublication>().forEach { publication ->
                    if (publication.name == "kotlinMultiplatform") {
                        // for our root metadata publication, set artifactId with a package and project name
                        publication.artifactId = project.name
                    } else {
                        // for targets, set artifactId with a package, project name and target name (e.g. iosX64)
                        publication.artifactId = "${project.name}-${publication.name}"
                    }
                }

                targets.all {
                    val publication = publications.findByName(name) as MavenPublication

                    // Patch publications with fake javadoc
                    publication.artifact(stubJavadoc.get())
                }
            }
        }
    }

}