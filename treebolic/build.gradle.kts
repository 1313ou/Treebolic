/*
 * Copyright (c) Treebolic 2019. Bernard Bou <1313ou@gmail.com>
 */

import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

private val vCode by lazy { rootProject.extra["versionCode"] as Int }
private val vName by lazy { rootProject.extra["versionName"] as String }
private val vCompileSdk by lazy { rootProject.extra["compileSdk"] as Int }
private val vMinSdk by lazy { rootProject.extra["minSdk"] as Int }
private val vTargetSdk by lazy { rootProject.extra["targetSdk"] as Int }

android {

    namespace = "org.treebolic"

    compileSdk = vCompileSdk

    defaultConfig {
        applicationId = "org.treebolic"
        versionCode = vCode
        versionName = vName
        minSdk = vMinSdk
        targetSdk = vTargetSdk
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
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
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.txt")
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

    kotlinOptions {
        jvmTarget = "17"
    }
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

    implementation(libs.multidex)
    implementation(libs.core.ktx)

    testImplementation(libs.junit)

    coreLibraryDesugaring(libs.desugar)
}