import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Lee un archivo .properties de la raíz si existe. Esos archivos no se suben al repositorio
fun leerPropiedades(nombre: String): Properties = Properties().apply {
    val archivo = rootProject.file(nombre)
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

// Configuración del proyecto de Firebase (ver firebase.properties.example)
val firebase = leerPropiedades("firebase.properties")

// Datos de la clave de firma (ver keystore.properties.example)
val firma = leerPropiedades("keystore.properties")

android {
    namespace = "com.laureano.recordatorios"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.laureano.recordatorios"
        minSdk = 26
        targetSdk = 37
        // versionCode sube en cada entrega; versionName es lo que ven los testers
        versionCode = 4
        versionName = "1.0.0-rc1"

        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${firebase.getProperty("firebase.projectId", "")}\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"${firebase.getProperty("firebase.appId", "")}\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"${firebase.getProperty("firebase.apiKey", "")}\"")
        buildConfigField("String", "FIREBASE_SENDER_ID", "\"${firebase.getProperty("firebase.senderId", "")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Si no hay keystore.properties, el release queda sin firmar en lugar de usar datos falsos
            val ruta = firma.getProperty("storeFile")
            if (ruta != null) {
                storeFile = file(ruta)
                storePassword = firma.getProperty("storePassword")
                keyAlias = firma.getProperty("keyAlias")
                keyPassword = firma.getProperty("keyPassword")
            }
        }
    }

    // Dos ambientes estables: no hay que tocar nada a mano antes de compilar
    flavorDimensions += "entorno"
    productFlavors {
        create("desarrollo") {
            dimension = "entorno"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Recordatorios DEV")
            // Los datos de desarrollo van a otra colección de Firestore
            buildConfigField("String", "COLECCION_USUARIOS", "\"dev_users\"")
            buildConfigField("boolean", "HERRAMIENTAS_PRUEBA", "true")
        }
        create("produccion") {
            dimension = "entorno"
            resValue("string", "app_name", "Recordatorios")
            buildConfigField("String", "COLECCION_USUARIOS", "\"users\"")
            buildConfigField("boolean", "HERRAMIENTAS_PRUEBA", "false")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            if (firma.getProperty("storeFile") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
