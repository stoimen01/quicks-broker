import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.21"
    id("application")
}

application {
    mainClassName = "broker.BrokerKt"
}

group = "com.quicks"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.typesafe.akka:akka-actor_2.12:2.5.21")
    implementation("com.typesafe.akka:akka-stream_2.12:2.5.21")
    implementation("com.typesafe.akka:akka-http_2.12:10.1.8")
    testImplementation("com.typesafe.akka:akka-testkit_2.12:2.5.21")
    testImplementation("junit:junit:4.12:")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}