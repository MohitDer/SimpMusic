plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("androidx.navigation.safeargs")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.maxrave.simpmusic"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.musicdownloader.musicplayer"
        minSdk = 26
        targetSdk = 34
        versionName = "1.0.0"
        versionCode = 1
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

//        resourceConfigurations += listOf(
//            "en",
//            "vi",
//            "it",
//            "de",
//            "tr",
//            "pt",
//            "fr",
//            "es",
//            "zh",
//            "in"
//        )
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("../release/keystore.jks")
            storePassword = "ghali_hasnaoui_2024"
            keyAlias = "ghali_hasnaoui_2024"
            keyPassword = "ghali_hasnaoui_2024"
        }
        create("release") {
            storeFile = file("../release/keystore.jks")
            storePassword = "ghali_hasnaoui_2024"
            keyAlias = "ghali_hasnaoui_2024"
            keyPassword = "ghali_hasnaoui_2024"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        jniLibs.useLegacyPackaging = true
        jniLibs.excludes += listOf(
            "META-INF/README.md",
            "META-INF/CHANGES",
            "META-INF/COPYRIGHT",
            "META-INF/META-INF/DEPENDENCIES",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt",
            "META-INF/license.txt",
            "META-INF/NOTICE",
            "META-INF/NOTICE.txt",
            "META-INF/notice.txt",
            "META-INF/ASL2.0",
            "META-INF/asm-license.txt",
            "META-INF/notice.txt",
            "META-INF/NOTICE.txt",
            "META-INF/LICENSE.txt",
            "META-INF/license.txt",
            "META-INF/notice.txt",
            "META-INF/NOTICE",
            "META-INF/LICENSE",
            "META-INF/notice",
            "META-INF/notice.txt",
            "META-INF/NOTICE.txt",
            "META-INF/LICENSE.txt",
            "META-INF/license.txt",
            "META-INF/notice.txt",
            "META-INF/NOTICE",
            "META-INF/LICENSE",
            "META-INF/notice",
            "META-INF/notice.txt",
            "META-INF/NOTICE.txt",
            "META-INF/LICENSE.txt",
            "META-INF/license.txt",
            "META-INF/notice.txt",
            "META-INF/NOTICE",
            "META-INF/LICENSE",
            "META-INF/notice",
            "META-INF/notice.txt",
            "META-INF/NOTICE.txt",
            "META-INF/LICENSE.txt",
            "META-INF/license.txt",
            "META-INF/notice.txt",
            "META-INF/NOTICE",
            "META-INF/LICENSE",
            "META-INF/notice",
            "META-INF/notice.txt",
            "META-INF/NOTICE.txt",
            "META-INF/LICENSE.txt",
            "META-INF/license.txt",
            "META-INF/notice.txt",
            "META-INF/NOTICE",
            "META-INF/LICENSE",
            "META-INF/notice",
            "META-INF/notice.txt",
            "META-INF/NOTICE.txt",
            "META-INF/LICENSE.txt",
            "META-INF/license.txt",
            "META-INF/notice.txt",
            "META-INF/NOTICE",
            "META-INF/LICENSE",
            "META-INF/notice",
            "META-INF/notice.txt",
            "META-INF/NOTICE.txt",
            "META-INF/LICENSE.txt",
            "META-INF/license.txt",
            "META-INF/notice.txt",
            "META-INF/NOTICE",
            "META-INF/LICENSE",
            "META-INF/notice",
            "META-INF/notice.txt",
            "META-INF/NOTICE.txt",
            "META-INF/LICENSE.txt",
            "META-INF/license.txt",
            "META-INF/notice.txt",
            "META-INF/NOTICE",
            "META-INF/LICENSE",
            "META-INF/notice",
            "META-INF/notice.txt",
            "META-INF/NOTICE.txt",
            "META-INF/LICENSE.txt",
            "META-INF/license.txt",
            "META-INF/notice",
            "META-INF/ASL2.0",
            "META-INF/*.kotlin_module"
        )
    }

    tasks.withType<JavaCompile>().configureEach {
        options.fork()
        options.forkOptions.jvmArgs?.addAll(
            listOf(
                "--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED"
            )
        )
    }
}

dependencies {

    //Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)

    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    // Optional - Integration with ViewModels
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    ksp("androidx.lifecycle:lifecycle-compiler:2.7.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    //material design3
    implementation("com.google.android.material:material:1.12.0")
    //runtime
    implementation("androidx.startup:startup-runtime:1.1.1")
    implementation(project(mapOf("path" to ":kotlinYtmusicScraper")))
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

    //ExoPlayer
    val media3_version = "1.2.1"
    implementation("androidx.media:media:1.6.0")
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation("com.google.android.exoplayer:extension-mediasession:2.19.1")
    implementation("androidx.media3:media3-exoplayer:$media3_version")
    implementation("androidx.media3:media3-ui:$media3_version")
    implementation("androidx.media3:media3-session:$media3_version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3_version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3_version")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3_version")
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:$media3_version")
    implementation("androidx.media3:media3-exoplayer-workmanager:$media3_version")
    implementation("androidx.media3:media3-datasource-okhttp:$media3_version")
    // DoubleTapPlayerView
    //implementation("com.github.vkay94:DoubleTapPlayerView:1.0.4")
    implementation(project(mapOf("path" to ":doubletapplayerview")))

    //palette color
    implementation("androidx.palette:palette-ktx:1.0.0")
    //expandable text view
    implementation("com.github.giangpham96:expandable-text:2.0.0")

    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    //Legacy Support
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    //Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3")
    //Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    implementation("com.google.code.gson:gson:2.10.1")

    //Coil
    implementation("io.coil-kt:coil:2.5.0")
    //Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    //Easy Permissions
    implementation("pub.devrel:easypermissions:3.0.0")
    //Palette Color
    implementation("androidx.palette:palette-ktx:1.0.0")

    //Preference
    implementation("androidx.preference:preference-ktx:1.2.1")

    //fragment ktx
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    //Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    ksp("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.8.0")
    //DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    //Swipe To Refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    //Insetter
    implementation("dev.chrisbanes.insetter:insetter:0.6.1")
    implementation("dev.chrisbanes.insetter:insetter-dbx:0.6.1")

    //Shimmer
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    //Lottie
    val lottieVersion = "6.3.0"
    implementation("com.airbnb.android:lottie:$lottieVersion")

    //Paging 3
    val paging_version= "3.2.1"
    implementation("androidx.paging:paging-runtime-ktx:$paging_version")

    implementation("com.daimajia.swipelayout:library:1.2.0@aar")

    //Custom Activity On Crash
    implementation ("cat.ereza:customactivityoncrash:2.4.0")

    implementation("com.intuit.sdp:sdp-android:1.1.0")
    implementation("com.intuit.ssp:ssp-android:1.1.0")

    val latestAboutLibsRelease = "10.10.0"
    implementation ("com.mikepenz:aboutlibraries:${latestAboutLibsRelease}")

    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.github.skydoves:balloon:1.6.4")

    // firebase
    implementation("com.google.firebase:firebase-database:20.3.1")
    // admob
    implementation("com.google.android.gms:play-services-ads:23.0.0")

    // extractor
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.24.2")
    implementation("com.github.TeamNewPipe:nanojson:1d9e1aea9049fc9f85e68b43ba39fe7be1c1f751")
    implementation("org.jsoup:jsoup:1.17.2")

    // rxjava
    implementation("io.reactivex:rxjava:1.3.8")
    implementation("com.artemzin.rxjava:proguard-rules:1.3.3.0")
    implementation("io.reactivex:rxandroid:1.2.1")

    // rxjava2
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    // stream java8
    implementation("com.annimon:stream:1.2.2")
    implementation("org.ocpsoft.prettytime:prettytime:5.0.6.Final")

    // file picker
    implementation("com.nononsenseapps:filepicker:4.2.1")

    implementation("frankiesardo:icepick:3.2.0")
    annotationProcessor("frankiesardo:icepick-processor:3.2.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("com.tbuonomo:dotsindicator:5.0")

}
hilt {
    enableAggregatingTask = true
}
aboutLibraries {
    prettyPrint = true
    registerAndroidTasks = false
    excludeFields = arrayOf("generated")
}
