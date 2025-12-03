plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("io.ktor.plugin") version "3.0.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server Core
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    
    // Content Negotiation & Serialization
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    
    // CORS
    implementation("io.ktor:ktor-server-cors-jvm")
    
    // Authentication
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    
    // Status Pages (Error Handling)
    implementation("io.ktor:ktor-server-status-pages-jvm")
    
    // Call Logging
    implementation("io.ktor:ktor-server-call-logging-jvm")
    
    // WebSockets (for real-time chat)
    implementation("io.ktor:ktor-server-websockets-jvm")
    
    // MongoDB
    implementation("org.litote.kmongo:kmongo:4.11.0")
    implementation("org.litote.kmongo:kmongo-coroutine:4.11.0")
    
    // BCrypt for password hashing
    implementation("org.mindrot:jbcrypt:0.4")
    
    // Logback for logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // Testing
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.ilkinbairamov.ninjatalk.ApplicationKt")
}