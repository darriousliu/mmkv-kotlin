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
        System.getProperty("os.name").startsWith("Mac")
    }
    workingDir = project.file(".")
    commandLine(
        "zsh", "-c",
        """
            mkdir -p build_cpp && \
            cd build_cpp && \
            cmake .. && \
            make && \
            echo $(shasum -a 256 libmmkvc.dylib | cut -d ' ' -f 1) > build-macos.hash
        """.trimIndent()
    )
}

// 配置JVM的processResources任务
tasks.named<ProcessResources>("processResources") {
    dependsOn(processBuild)
    from(project.file("build_cpp/libmmkvc.dylib"))
    from(project.file("build_cpp/build-macos.hash"))
}