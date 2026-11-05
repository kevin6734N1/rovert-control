# Registro de cambios — Rovert

Historial de versiones del proyecto. Cada versión indica qué se añadió, qué se
rompió y cómo se resolvió, para que nadie tenga que repetir un diagnóstico
que ya se hizo.

---

## v1.1.0 — 2026-11-04 — Web app para iPhone / iPad

> **Motivo:** Fabián y Rodrigo tienen iOS, no Android. Se evaluaron cuatro rutas
> (PWA, Kotlin Multiplatform, SwiftUI, Flutter) y se eligió la **PWA servida por
> la propia ESP32**: es la única que no exige Mac, ni Xcode, ni 99 USD/año de
> cuenta de Apple Developer. Argumentación completa con fuentes en
> [`12-Evaluacion-iOS.md`](12-Evaluacion-iOS.md).

### Añadido

**Web app (`firmware/esp32cam/webapp/`)**
- Interfaz de control completa en HTML + CSS + JavaScript, sin dependencias
  externas: cruceta táctil, botón de `STOP`, indicador de conexión, contador de
  FPS, panel de ajustes y soporte de mando por la Gamepad API del navegador.
- **El mismo protocolo** que la app Android (`Docs/04`): `drive`, `stop` y `ping`
  por `ws://192.168.4.1/control`. No hay un "modo iOS" ni ramas especiales.
- El vídeo se obtiene **sondeando `GET /capture`** y pintándolo en un `<canvas>`
  (~10 FPS), en vez de por `multipart/x-mixed-replace`, porque Safari falla con
  `fetch()` + `response.body.getReader()` sobre ese tipo de respuesta
  ("TypeError: Load failed").
- Generador `build-webapp.js` que incrusta los tres archivos en la flash como
  literales `PROGMEM` (31.726 bytes) en `RovertCam/webapp_generated.h`. El `.h`
  va versionado, así que no hace falta Node.js para compilar el firmware.
- Simulador sin dependencias `tools/mock-rover-server.js` para probar la web app
  en un navegador de escritorio sin tener la ESP32 delante.

**Firmware ESP32-CAM**
- Tres rutas nuevas: `GET /` (HTML), `GET /app.css` y `GET /app.js`, servidas con
  `Cache-Control: no-store`.
- `GET /capture` ahora manda `Cache-Control: no-store` y `Pragma: no-cache`,
  para que el navegador no reutilice fotogramas viejos durante el sondeo.

**Documentación**
- `Docs/12-Evaluacion-iOS.md`: comparación de las cuatro rutas para iOS con
  fuentes citadas.
- `Docs/13-Guia-Control-Web-iOS.md`: guía paso a paso para iPhone/iPad, ajustes,
  problemas frecuentes y límites.

**Herramientas**
- `tools/build-firmware.ps1`: compila los dos firmwares con `arduino-cli`
  (ESP32 con PSRAM y 8 MB de flash; UNO con `arduino:avr`).

### Cambiado

- **El diagnóstico JSON se movió de `GET /` a `GET /info`**, porque la raíz `/`
  pasa a servir la web app.
- `RoverClient.fetchDiagnostic()` (app Android) ahora pide `/info`.
- `config.h`: `ROVERT_INFO_PATH` = `"/info"`; se añaden
  `ROVERT_WEBAPP_INDEX_PATH`, `ROVERT_WEBAPP_CSS_PATH` y `ROVERT_WEBAPP_JS_PATH`.

### Sin cambios (compatibilidad garantizada)

- El **protocolo**, los **pines**, el **watchdog** y el firmware del **Arduino UNO**
  son idénticos a la v1.0.0.
- La app Android conserva su comportamiento; los **38 tests unitarios** siguen en
  verde y el lint no reporta errores.
- El APK se vuelve a publicar con el mismo `applicationId`, así que se
  **actualiza encima** de la v1.0.0 sin desinstalar nada.

### Pendiente / no verificado

- **No hay hardware en el taller de pruebas**: ni la ESP32-CAM ni el Arduino UNO
  se han grabado ni ejecutado. El firmware está escrito y **compilado**, pero no
  ha corrido en la placa.
- El comportamiento de **Safari en iOS 26** (aviso de "conexión no segura") y el
  rendimiento del sondeo de `/capture` **no se han podido verificar en un iPhone
  real**: es el punto nº 1 de la lista de pruebas de `Docs/10`.
- El **Gamepad API en Safari** requiere **iOS 16.4+**; por debajo, la web app
  funciona solo con los botones táctiles.

---

## v1.0.0 — 2026-11-04 — Versión inicial completa

### Añadido

**App Android (`android/`)**
- Aplicación Kotlin + Jetpack Compose, `applicationId` `com.rovert.rovercontrol`,
  `minSdk 24` (Android 7.0), `targetSdk 34`.
- Pantalla única con: vídeo en directo (MJPEG), barra de estado (conexión, FPS,
  estado del rover), cruceta táctil de botón grande, botón rojo de `STOP`,
  indicador de modo (`TÁCTIL` / `MANDO`) y acceso a los ajustes.
- Cliente de red (`RoverClient`) sobre OkHttp: WebSocket a `/control` con
  reconexión automática y backoff exponencial, heartbeat cada 2 s, y envío
  obligatorio de `stop` al caer la conexión.
- Vista de vídeo (`MjpegStreamView`): parser propio de `multipart/x-mixed-replace`
  con soporte de `Content-Length`, decodificación con `BitmapFactory` fuera del
  hilo principal y cancelación limpia de la petición.
- Entrada de mando (`GamepadController` + `GamepadCaptureView`): lectura de los
  ejes del stick izquierdo por `MotionEvent`, zona muerta configurable, detección
  de desconexión (que fuerza `STOP`).
- Capa de protocolo aislada (`protocol/`): `ProtocolCodec` (JSON) y
  `MotorMapping` (acción → `left`/`right`), sin dependencias de Android, cubierta
  por tests unitarios.
- Ajustes persistentes (`SettingsStore` con SharedPreferences): IP del rover,
  puerto, velocidad máxima, polaridad por motor, zona muerta, conexión automática.
- Manifest con `INTERNET`, `ACCESS_NETWORK_STATE`, `usesCleartextTraffic` y
  `network_security_config` para poder hablar por HTTP claro con la IP local.

**Firmware ESP32-CAM (`firmware/esp32cam/RovertCam/`)**
- SoftAP `Rovert-Cam` (contraseña `rovert2026`) en `192.168.4.1`.
- Endpoint `GET /stream` (MJPEG por `multipart/x-mixed-replace`), `GET /capture`
  (JPEG suelto), `GET /status` y `GET /` (diagnóstico JSON).
  *(En la v1.1.0 el diagnóstico pasa a `GET /info`.)*
- WebSocket `/control` con mensajes `drive`, `stop` y `ping`, y difusión de
  `status`/`pong` hacia la app.
- Puente UART2 (GPIO32 TX / GPIO33 RX, 38400 8N1) hacia el Arduino UNO.
- Watchdog propio: 500 ms sin orden de la app → `STOP` al Arduino.
- Todo lo ajustable (SSID, contraseña, pines, baudios, tiempoouts, calidad y
  resolución de cámara) en `config.h`.

**Firmware Arduino UNO (`firmware/arduino_uno/MotorController/`)**
- Parser de tramas `D` / `S` / `P` con milésimas enteras.
- Control del L298N (ENA/ENB por PWM + IN1..IN4) con corrección de polaridad
  por constante (`LEFT_MOTOR_INVERT`, `RIGHT_MOTOR_INVERT`).
- Watchdog de 400 ms sin trama válida → motores a 0 + trama `W`.
- Telemetría `R,...` cada 100 ms y LED frontal en D2 (faro + parpadeo al mover).

**Librerías vendorizadas (`firmware/esp32cam/libraries/`)**
- `ESP Async WebServer` 3.12.1 y `Async TCP` 3.5.0 (ESP32Async / Espressif),
  incluidas para que los compañeros no tengan que instalarlas a mano.

**Documentación (`Docs/`)**
- Índice, arquitectura y decisiones de diseño, contrato de comandos, protocolo
  UART, guía de instalación del APK, tabla de conexiones y cableado, flashed de
  ambos firmwares, pruebas de aceptación, FAQ y plan de pruebas por sesiones.

### Decisiones de diseño relevantes
- La ESP32-CAM crea su **propia red Wi-Fi** para no depender del router del aula.
- Los pines de cámara son los de **Freenove ESP32-WROVER-CAM**, no los de
  AI-Thinker (son incompatibles y fallan sin error de compilación).
- El UART usa **GPIO32/GPIO33**: en el módulo WROVER, GPIO16/17 los ocupa la
  PSRAM interna y GPIO6–11 la flash.
- Polaridad y velocidad máxima se corrigen **en la app**, no en el firmware.
- Tres barreras de parada independientes: app, ESP32 y Arduino.

### Pendiente / no implementado
- Telemetría de nivel de batería (requiere divisor de tensión y un ADC extra).
- Grabación de vídeo o fotos desde la app.
- Control de la luz LED desde el software (la maneja el Arduino como faro).
- Posicionamiento (odometría o GPS).

### Nota de seguridad: token de Google Drive en este computador

Para subir el APK a Google Drive se usó **rclone**, cuya página web de subida
no es automatizable. El token de OAuth quedó guardado en:

```
C:\Users\kevin\Proyectos\Robert - ESP32 Cam\tools\rclone-config\rclone.conf
```

- Esa carpeta está en `.gitignore`: **el token nunca se sube a GitHub**.
- El token concede acceso completo al Drive de la cuenta (ver, editar y borrar).
- Para revocarlo: <https://myaccount.google.com/permissions> → busca "rclone" →
  *Quitar acceso*. También puedes borrar el archivo `rclone.conf`.
- El APK ya está publicado en GitHub Releases, así que el token **no hace falta**
  para nada del proyecto.

---

## Problemas encontrados y cómo se resolvieron

| Síntoma | Causa | Solución adoptada |
|---|---|---|
| La app se conecta al Wi-Fi pero Android avisa "sin Internet" | Un SoftAP no da salida a Internet | Es el comportamiento esperado; se documenta "Mantener conectado" en la guía de instalación |
| La cámara no da imagen aunque el código compila | Tabla de pines de la placa equivocada (AI-Thinker vs Freenove) | Pines de la WROVER centralizados en `config.h`, con la tabla de ambas placas en el doc de arquitectura |
| El rover no se detiene al cerrar la app | Dependencia exclusiva del cliente | Se añadieron tres barreras: `stop` en `onPause`, watchdog de 500 ms en la ESP32 y watchdog de 400 ms en el Arduino |
| Riesgo de perder la conexión con el Wi-Fi durante una demo | La ESP32 no puede forzar la desconexión del cliente | La app reconecta sola con backoff y el Arduino para solo si el UART se corta |
| Un motor gira al revés | Polaridad del L298N depende del cableado | Constante `RIGHT_MOTOR_INVERT` en el sketch + corrección en los ajustes de la app |
| Descargas del SDK de Android a 0,1 MB/s con `Invoke-WebRequest` | Limitación de ese cmdlet en la red del laboratorio | Se pasó a `curl.exe` (≈9 MB/s) |

---

## Cómo publicar una versión nueva

1. Actualizar `Docs/11-Registro-de-Cambios.md` con la entrada nueva.
2. Compilar y pasar los tests:
   ```bash
   cd android
   ../tools/gradle-8.9/bin/gradle.bat clean testDebugUnitTest assembleDebug
   ```
3. Copiar el APK a `dist/` con el nombre `Rovert-Control-v<versión>.apk`.
4. Crear el release en GitHub:
   ```bash
   gh release create v<versión> dist/Rovert-Control-v<versión>.apk --title "Rovert Control v<versión>" --notes "..."
   ```
5. `git push` de los cambios.
