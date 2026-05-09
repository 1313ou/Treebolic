/*
 * Copyright (c) Treebolic 2019. Bernard Bou <1313ou@gmail.com>
 */

import java.io.FileInputStream
import java.util.Properties
import java.text.SimpleDateFormat
import java.util.Date

val buildTime: String = SimpleDateFormat("yyyy-MM-dd_HH:mm").format(Date())

fun getGitHash(workingDir: File): String? {
    return try {
        val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
            .directory(workingDir)
            .redirectErrorStream(true)
            .start()
        val result = process.inputStream.bufferedReader().use { it.readText() }.trim()
        val exitCode = process.waitFor()
        if (exitCode == 0) result else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

plugins {
    alias(libs.plugins.androidApplication)
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties: Properties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

android {

    namespace = "org.treebolic"

    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.treebolic"
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get() as String?
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true

        // BuildConfig fields
        buildConfigField("int", "VERSION_CODE", libs.versions.versionCode.get())
        buildConfigField("String", "VERSION_NAME", "\"${libs.versions.versionCode.get()}\"")
        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
        buildConfigField("String", "GIT_HASH", "\"${getGitHash(File("Treebolic"))}\"")
    }

    signingConfigs {
        create("treebolic") {
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
            storeFile = file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.txt")
            signingConfig = signingConfigs.getByName("treebolic")
            versionNameSuffix = "signed"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.treebolic.graph)
    implementation(libs.treebolic.mutable)
    implementation(libs.treebolic.model)
    implementation(libs.treebolic.view)
    implementation(libs.treebolic.provider.xml.dom)
    implementation(libs.treebolic.provider.xml.sax)
    implementation(libs.treebolic.provider.text.indent.tree)
    implementation(libs.treebolic.provider.text.indent)
    implementation(libs.treebolic.provider.text.pair)
    implementation(libs.treebolic.provider.graphviz)

    implementation(project(":treebolicGlue"))
    implementation(project(":treebolicParcel"))
    implementation(project(":treebolicIface"))
    implementation(project(":treebolicClientsIface"))
    implementation(project(":treebolicClientsLib"))
    implementation(project(":treebolicServicesIface"))

    implementation(project(":theming"))
    implementation(project(":commonLib"))
    implementation(project(":storageLib"))
    implementation(project(":searchLib"))
    implementation(project(":fileChooserLib"))
    implementation(project(":downloadLib"))
    implementation(project(":preferenceLib"))
    implementation(project(":guideLib"))
    implementation(project(":rateLib"))
    implementation(project(":othersLib"))
    implementation(project(":donateLib"))

    implementation(libs.appcompat)
    implementation(libs.preference.ktx)
    implementation(libs.material)

    implementation(libs.core.ktx)
    implementation(platform(libs.kotlin.bom))
    implementation(kotlin("stdlib"))
    coreLibraryDesugaring(libs.desugar)

    testImplementation(libs.junit)
}