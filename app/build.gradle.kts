import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("org.jetbrains.kotlin.plugin.serialization")
    jacoco
}

jacoco {
    toolVersion = "0.8.11"
}

// Read API key from local.properties
val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) load(localFile.inputStream())
}

android {
    namespace = "com.app.wordlearn"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.app.wordlearn"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Expose Gemini API key via BuildConfig (stays out of git via local.properties)
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\""
        )

        // Web Client ID for Google Sign-In (kept out of git via local.properties)
        buildConfigField(
            "String",
            "WEB_CLIENT_ID",
            "\"${localProperties.getProperty("WEB_CLIENT_ID", "")}\""
        )
    }

    buildTypes {
        debug {
            // Debug build'inde minify kapalı — hızlı iterasyon için.
            isMinifyEnabled = false
            // JaCoCo bytecode instrumentation için coverage data toplamayı aç.
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Material Components
    implementation("com.google.android.material:material:1.12.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.04.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    ksp("com.google.dagger:hilt-android-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Kotlinx Serialization (JSON seed parsing)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Gemini AI (for Word Chain LLM stories)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // WorkManager for Notifications
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// =================================================================
// JaCoCo unit-test coverage raporu — SonarQube'a `sonar.coverage.jacoco.xmlReportPaths`
// üzerinden gönderilir. Komut:
//   ./gradlew jacocoTestReport
// XML çıktısı: app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
// HTML çıktısı: app/build/reports/jacoco/jacocoTestReport/html/index.html
// =================================================================
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    description = "Unit testleri için coverage raporu üretir (XML + HTML)."

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    // Generated / framework kodlarını ölç dışı tut — coverage gerçek iş kodumuzu yansıtsın.
    val coverageExcludes = listOf(
        // Android generated
        "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test*.*", "android/**/*.*",
        // Hilt generated
        "**/*_HiltModules*.*", "**/*_Factory*.*", "**/*_MembersInjector*.*",
        "**/Hilt_*.*", "**/*_GeneratedInjector*.*", "**/DaggerHilt_*.*",
        "**/hilt_aggregated_deps/**",
        // Compose generated
        "**/*ComposableSingletons*.*", "**/*\$\$inlined*.*",
        // Serialization & KSP
        "**/*\$serializer*.*", "**/*\$Companion*.*"
    )

    val kotlinTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(coverageExcludes)
    }
    val javaTree = fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug/classes") {
        exclude(coverageExcludes)
    }

    classDirectories.setFrom(files(kotlinTree, javaTree))
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(fileTree(layout.buildDirectory.get()).include(
        "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
        "jacoco/testDebugUnitTest.exec"
    ))
}
