import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

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
