plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.example"
version = "4.7.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.1.1")
    implementation("org.yaml:snakeyaml:1.33")
    implementation("net.dv8tion:JDA:5.6.1")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks {
    jar {
        archiveBaseName.set("velocord")
    }
    
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("velocord")
        archiveVersion.set("4.7.1")
        
        configurations = listOf(project.configurations.runtimeClasspath.get())
        
        mergeServiceFiles()
        
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
    
    build {
        dependsOn(shadowJar)
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
} 