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
        System.getProperty("os.name").startsWith("Linux")
    }
    workingDir = project.file(".")
    commandLine(
        "bash", "-c",
        """
            mkdir -p build_cpp && \
            cd build_cpp && \
            cmake .. && \
            make && \
            sha256sum libmmkvc.so | cut -d ' ' -f 1 > build-linux.hash
        """.trimIndent()
    )
}

// 配置JVM的processResources任务
tasks.named<ProcessResources>("processResources") {
    dependsOn(processBuild)
    from(project.file("build_cpp/libmmkvc.so"))
    from(project.file("build_cpp/build-linux.hash"))
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "mmkv-kotlin-nativelib-linux",
        version = version.toString(),
    )

    pom {
        name.set("MMKV-Kotlin Linux")
        description.set("MMKV for Kotlin Multiplatform (Linux Native Library)")
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
