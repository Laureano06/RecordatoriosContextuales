# Recordatorios contextuales

Aplicación Android (Kotlin, Jetpack Compose y Material 3) para crear, consultar, modificar y eliminar recordatorios. Funciona sin conexión: guarda todo en Room y lo sincroniza con un servicio REST cuando hay red.

## Requisitos

- Android Studio con el SDK 37 y un emulador (se probó en Medium Phone API 37.0).
- Python 3 para el servicio simulado (solo usa la biblioteca estándar).

## Configurar

La URL del servicio se toma de `api.properties`, en la raíz del proyecto. Ese archivo no se sube al repositorio. Si no existe, se usa `http://localhost:8090/`.

Para cambiar la URL, copiá `api.properties.example` como `api.properties` y editá `api.urlBase`. El servicio no usa credenciales, y el proyecto no incluye secretos.

## Ejecutar

1. Iniciá el servicio simulado desde la raíz del proyecto:

   ```
   python tools/servidor_simulado.py
   ```

   Para probar errores, agregá `--modo error500`, `--modo lento` (provoca un timeout) o `--modo invalido` (responde 400). Los datos del servicio se guardan en `tools/datos_servidor.json`.

2. Con el emulador abierto, redirigí el puerto para que la app llegue a la PC:

   ```
   adb reverse tcp:8090 tcp:8090
   ```

   Hay que repetirlo cada vez que se reinicia el emulador. En un teléfono conectado por USB funciona igual.

3. En Android Studio, sincronizá Gradle, elegí la configuración `app` y ejecutala.

La app también funciona sin el servicio: muestra los datos guardados y marca los cambios como pendientes.

## Pruebas

```
./gradlew testDebugUnitTest
```

Son 32 pruebas unitarias. Incluyen el mapeo entre DTO, Entity y dominio; la lectura de preferencias con valores inválidos; la interpretación de las respuestas HTTP (200, 400, 500, timeout y sin conexión) con MockWebServer; y la sincronización sin duplicados con un DAO y un servicio falsos.

## Estructura

- `data/local`: Room (`RecordatorioEntity`, `RecordatorioDao`, `BaseDatos`).
- `data/preferencias`: Preferences DataStore (`PreferenciasRepository`).
- `data/remote`: Retrofit y OkHttp (`RecordatorioDto`, `RecordatorioApi`, `ClienteRemoto`, `ResultadoRemoto`).
- `data`: `RecordatorioRepositoryLocalFirst` (coordina las tres fuentes), `Mapeos.kt` y `MonitorConectividad`.
- `domain`: modelos y reglas que no dependen de Android.
- `ui`: pantallas, ViewModels y navegación.
