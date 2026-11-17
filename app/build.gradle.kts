plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.enigma.aminlivetv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.enigma.aminlivetv"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.2.2"
        buildConfigField(
            "String", "CATALOG_URL",
            "\"https://raw.githubusercontent.com/Skyspiriit/Amin-Live-TV/main/channels.json\""
        )
        buildConfigField("boolean", "TEST_CATALOG", "false")
    }

    val keyPath = System.getenv("AMIN_LIVE_TV_KEYSTORE")
    if (keyPath != null) {
        signingConfigs.create("aminLiveTv") {
            storeFile = file(keyPath)
            storePassword = System.getenv("AMIN_LIVE_TV_STORE_PASSWORD")
            keyPassword = System.getenv("AMIN_LIVE_TV_KEY_PASSWORD")
            keyAlias = System.getenv("AMIN_LIVE_TV_KEY_ALIAS")
        }
    }

    buildTypes {
        debug {
            providers.gradleProperty("testCatalogUrl").orNull?.let { url ->
                require(url.matches(Regex("http://127\\.0\\.0\\.1:[0-9]+/[a-zA-Z0-9/._-]+")))
                applicationIdSuffix = ".qa"
                buildConfigField("String", "CATALOG_URL", "\"$url\"")
                buildConfigField("boolean", "TEST_CATALOG", "true")
                manifestPlaceholders["networkPolicy"] = "@xml/network_security_qa"
            }
        }
        release {
            if (keyPath != null) signingConfig = signingConfigs.getByName("aminLiveTv")
        }
    }

    defaultConfig.manifestPlaceholders["networkPolicy"] = "@xml/network_security"

    buildFeatures {
        buildConfig = true
    }

    sourceSets.named("main") {
        assets.srcDir(rootProject.layout.buildDirectory.dir("generated/catalogue"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin.jvmToolchain(17)
}

val bundleCatalogue by tasks.registering(Copy::class) {
    from(rootProject.file("channels.json"))
    into(rootProject.layout.buildDirectory.dir("generated/catalogue"))
}
tasks.named("preBuild").configure { dependsOn(bundleCatalogue) }

dependencies {
    val media3 = "1.8.0"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-exoplayer-dash:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    testImplementation("junit:junit:4.13.2")
}
