plugins {
    kotlin("jvm") version "1.9.22"
    id("com.gradleup.shadow") version "8.3.0"
}

group = "com.unchoose.cobblemon"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotmc-repo"
    }
    maven("https://repo.alessiodp.com/releases/")
}

dependencies {
    // Spigot API
    compileOnly("org.spigotmc:spigot-api:1.21.1-R0.1-SNAPSHOT")
    
    // Kotlin - 编译时需要，但不打包进插件
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // Database - 编译时需要，运行时由libby加载
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.45.1.0")
    compileOnly("com.mysql:mysql-connector-j:8.3.0")
    
    // BukkitLibraryManager - 运行时加载依赖，需要打包进插件
    implementation("net.byteflux:libby-bukkit:1.3.1")
}

kotlin {
    jvmToolchain(21)
}

tasks.shadowJar {
    // 重定位 libby 包，避免冲突
    relocate("net.byteflux.libby", "com.unchoose.cobblemon.libs.libby")
    
    // 设置输出文件名
    archiveBaseName.set("UnChooseCobblemon")
    archiveClassifier.set("")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(
            "version" to project.version,
            "name" to project.name
        )
    }
}
