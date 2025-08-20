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
        System.getProperty("os.name").startsWith("Win")
    }
    workingDir = project.file(".")
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
