# Recordatorios contextuales

Aplicación Android (Kotlin, Jetpack Compose y Material 3) para crear, consultar, modificar y eliminar recordatorios. Cada persona inicia sesión con Firebase Authentication. Los recordatorios se guardan primero en Room y se sincronizan con Cloud Firestore, así que la app funciona sin conexión.

Versión candidata: **1.0.0-rc1** (versionCode 4).

## Requisitos

- Android Studio con el SDK 37 y un emulador o un teléfono (se probó en Medium Phone API 37.0).
- Un proyecto de Firebase con Authentication (email y contraseña) y Cloud Firestore.

## Configurar

Ninguno de estos archivos se sube al repositorio.

1. **Firebase.** Copiá `firebase.properties.example` como `firebase.properties` y completalo con los datos de tu proyecto, que están en Consola de Firebase → Configuración del proyecto → Tus apps.
2. **Reglas de Firestore.** Publicá el contenido de `firestore.rules` en Firestore → Reglas.
3. **Firma del release.** Generá una clave fuera del proyecto y copiá `keystore.properties.example` como `keystore.properties` con su ruta y sus contraseñas:

   ```
   keytool -genkeypair -keystore C:/ruta/segura/recordatorios-release.jks -alias recordatorios -keyalg RSA -keysize 2048 -validity 10000
   ```

   Sin `keystore.properties` el proyecto compila igual, pero el release queda sin firmar.

## Variantes

| Variante | Paquete | Nombre visible | Colección de Firestore |
|---|---|---|---|
| desarrolloDebug | com.laureano.recordatorios.dev | Recordatorios DEV | `dev_users` |
| produccionRelease | com.laureano.recordatorios | Recordatorios | `users` |

La variante de desarrollo agrega una prueba de seguridad en Preferencias, que intenta leer y escribir los datos de otra cuenta. Las dos variantes se pueden instalar a la vez en el mismo teléfono.

## Compilar y ejecutar

```
./gradlew testDesarrolloDebugUnitTest        # pruebas unitarias
./gradlew assembleDesarrolloDebug            # app de desarrollo
./gradlew assembleProduccionRelease          # APK firmado
./gradlew bundleProduccionRelease            # Android App Bundle (AAB)
```

Los artefactos quedan en:

- `app/build/outputs/apk/produccion/release/`
- `app/build/outputs/bundle/produccionRelease/`

Para probar el APK release, desinstalá antes cualquier versión debug con el mismo paquete, porque la firma es distinta.

## Herramientas

`tools/verificar_firestore.py` inicia sesión con una cuenta y lista sus documentos en Firestore, o intenta leer y escribir los de otra cuenta. Sirve para registrar qué pasa en la nube.

```
python tools/verificar_firestore.py listar EMAIL CONTRASENIA --coleccion dev_users
python tools/verificar_firestore.py ajeno EMAIL CONTRASENIA UID_AJENO --coleccion dev_users
```

## Estructura

- `data/auth`: `AuthRepository` y su implementación con Firebase Authentication.
- `data/local`: Room (`RecordatorioEntity`, `RecordatorioDao`, `BaseDatos`).
- `data/remote`: Firestore (`FuenteRemota`, `RecordatorioRemoto`, `ResultadoRemoto`).
- `data/preferencias`: Preferences DataStore.
- `data`: `RecordatorioRepositoryLocalFirst` (coordina Room, Firestore, la sesión y las preferencias), `Mapeos.kt` y `MonitorConectividad`.
- `domain`: modelos y reglas que no dependen de Android.
- `ui`: pantallas, ViewModels y navegación. `ui/acceso` contiene el inicio de sesión y el registro.
