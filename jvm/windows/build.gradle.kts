import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.vanniktech.maven.publish)
}

version = "1.3.1"
group = "io.github.darriousliu"

kotlin {
    jvmToolchain(22)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_22
    }
}

val processBuild = tasks.register<Exec>("processBuild") {
    onlyIf {
        System.getProperty("os.name").startsWith("Win")
    }
    workingDir = project.file("src")
    commandLine("pwsh","-c","""
        msbuild native-binding-windows.sln /p:Configuration=Release
        (Get-FileHash -Algorithm SHA256 -Path "x64/Release/mmkvc.dll").Hash | Out-File -FilePath "x64/Release/build-windows.hash"
    """.trimIndent())
}

// 配置JVM的processResources任务
tasks.named<ProcessResources>("processResources") {
    dependsOn(processBuild)
    from(project.file("src/x64/Release/mmkvc.dll"))
    from(project.file("src/x64/Release/build-windows.hash"))
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "mmkv-kotlin-nativelib-windows",
        version = version.toString(),
    )

    pom {
        name.set("MMKV-Kotlin Windows")
        description.set("MMKV for Kotlin Multiplatform (Windows Native Library)")
        url.set("https://github.com/ctripcorp/mmkv-kotlin")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("qiaoyuang")
                name.set("Yuang Qiao")
                email.set("qiaoyuang2012@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/ctripcorp/mmkv-kotlin")
            connection.set("scm:git:https://github.com/ctripcorp/mmkv-kotlin.git")
            developerConnection.set("scm:git:https://github.com/ctripcorp/mmkv-kotlin.git")
        }
    }
}
