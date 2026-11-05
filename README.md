# Rovert — Control de rover por Wi-Fi desde Android, iPhone o iPad

**Proyecto de curso de Robótica · "Rovert Post Sismos"**

Rover todoterreno con cámara, controlado desde el móvil por Wi-Fi directo, con
mando DualShock 4 opcional.

| | |
|---|---|
| **App Android** | Android nativa (Kotlin + Jetpack Compose), minSdk 24 (Android 7.0) |
| **App iOS / iPad** | **Web app servida por la propia ESP32**: se abre `http://192.168.4.1` en el navegador, sin instalar nada |
| **Cerebro de red y cámara** | Freenove ESP32-WROVER-CAM (OV2640, PSRAM 4 MB) |
| **Control de motores** | Arduino UNO R3 + L298N + 2 motores TT |
| **Enlace** | Wi-Fi SoftAP propio + WebSocket/HTTP (puerto 80) + UART 38400 8N1 |
| **Repositorio** | <https://github.com/kevin6734N1/rovert-control> |

---

## Qué hace

1. La ESP32-CAM crea su propia red **`Rovert-Cam`** (contraseña `rovert2026`).
2. El teléfono se conecta a esa red y abre la app.
3. La app muestra el **vídeo en directo** de la cámara y recibe las órdenes del
   **mando** o de los **botones táctiles**.
4. La ESP32 traduce esas órdenes y las manda al Arduino por UART.
5. El Arduino mueve los motores con el L298N.
6. Si algo falla (se cae el Wi-Fi, se cierra la app, se corta el cable), el
   rover **se detiene solo** en menos de medio segundo.

---

## Inicio rápido

### Dónde descargar el APK

| Canal | Enlace |
|---|---|
| **GitHub Releases** (recomendado) | <https://github.com/kevin6734N1/rovert-control/releases/tag/v1.0.0> → *Assets* → `Rovert-Control-v1.0.0.apk` |
| **Google Drive** (carpeta *Taller de Robótica*) | <https://drive.google.com/open?id=1bEzPmtJGLL9wSy7pMydhVEz8jJ0VDAkB> |

El APK está firmado con la clave de depuración, así que se instala directamente
en cualquier teléfono (Android 7.0 o superior) sin necesidad de una tienda.

### Para los compañeros (Fabián y Rodrigo)

> **¿Tienes iPhone o iPad en vez de Android?** No hace falta instalar nada: la
> ESP32 sirve su propia web app. Conéctate al Wi-Fi `Rovert-Cam` y abre
> **`http://192.168.4.1`** en el navegador. Guía completa:
> **[`Docs/13-Guia-Control-Web-iOS.md`](Docs/13-Guia-Control-Web-iOS.md)**.

1. Lee **[`Docs/02-Guia-de-Instalacion-APK.md`](Docs/02-Guia-de-Instalacion-APK.md)**
   para instalar la app en el teléfono.
2. Sigue **[`Docs/03-Tabla-de-Conexiones-y-Cableado.md`](Docs/03-Tabla-de-Conexiones-y-Cableado.md)**
   para cablear el rover.
3. Graba los firmwares con
   **[`Docs/06-Instrucciones-Flash-ESP32.md`](Docs/06-Instrucciones-Flash-ESP32.md)** y
   **[`Docs/07-Instrucciones-Flash-Arduino.md`](Docs/07-Instrucciones-Flash-Arduino.md)**.
4. Pasa la lista de **[`Docs/08-Pruebas-de-Aceptacion.md`](Docs/08-Pruebas-de-Aceptacion.md)**.
5. Si algo falla: **[`Docs/09-FAQ-y-Problemas.md`](Docs/09-FAQ-y-Problemas.md)**.

### Control desde iPhone / iPad (web app)

La ESP32-CAM **sirve su propia interfaz web**. Es un cliente más del mismo
protocolo que usa la app Android, así que:

- **No hay que instalar nada**: se abre `http://192.168.4.1` en Safari.
- Funciona en **iPhone, iPad, Android y PC** con cualquier navegador.
- Botones táctiles, mando DualShock 4 (iOS 16.4+), ajustes de polaridad y
  velocidad, y vídeo de la cámara a ~10 FPS.

Detalle en **[`Docs/13-Guia-Control-Web-iOS.md`](Docs/13-Guia-Control-Web-iOS.md)**
y en **[`Docs/12-Evaluacion-iOS.md`](Docs/12-Evaluacion-iOS.md)** (por qué no se
hizo una app nativa para iOS).

### Para desarrollar

```bash
# App Android
cd android
../tools/gradle-8.9/bin/gradle.bat assembleDebug     # APK en app/build/outputs/apk/debug/
../tools/gradle-8.9/bin/gradle.bat testDebugUnitTest  # tests del protocolo y del mapeo de motores

# Web app (HTML + CSS + JS de la interfaz que sirve la ESP32)
node firmware/esp32cam/webapp/build-webapp.js   # regenera webapp_generated.h

# Simulador del rover en el PC (para probar la web app sin hardware)
node tools/mock-rover-server.js 8080             # luego abre http://localhost:8080/

# Firmwares: también se pueden compilar por línea de comandos
powershell -ExecutionPolicy Bypass -File tools/build-firmware.ps1
#   - firmware/esp32cam/RovertCam/RovertCam.ino   (placa: ESP32 Dev Module, PSRAM on, 8 MB)
#   - firmware/arduino_uno/MotorController/MotorController.ino
```

---

## Documentación

| Documento | Para qué sirve |
|---|---|
| [`Docs/00-INDICE.md`](Docs/00-INDICE.md) | Índice de toda la documentación |
| [`Docs/01-Arquitectura-y-Decisiones.md`](Docs/01-Arquitectura-y-Decisiones.md) | Decisiones de diseño y por qué se tomaron |
| [`Docs/02-Guia-de-Instalacion-APK.md`](Docs/02-Guia-de-Instalacion-APK.md) | Instalación de la app y primer uso |
| [`Docs/03-Tabla-de-Conexiones-y-Cableado.md`](Docs/03-Tabla-de-Conexiones-y-Cableado.md) | Cableado completo, tablas de pines, diagramas |
| [`Docs/04-Protocolo-Comandos.md`](Docs/04-Protocolo-Comandos.md) | **Contrato** app ↔ ESP32 ↔ Arduino |
| [`Docs/05-Protocolo-UART-ESP32-Arduino.md`](Docs/05-Protocolo-UART-ESP32-Arduino.md) | Detalle del enlace UART y del watchdog |
| [`Docs/06-Instrucciones-Flash-ESP32.md`](Docs/06-Instrucciones-Flash-ESP32.md) | Cómo grabar la ESP32-CAM |
| [`Docs/07-Instrucciones-Flash-Arduino.md`](Docs/07-Instrucciones-Flash-Arduino.md) | Cómo grabar el Arduino UNO |
| [`Docs/08-Pruebas-de-Aceptacion.md`](Docs/08-Pruebas-de-Aceptacion.md) | Checklist de verificación |
| [`Docs/09-FAQ-y-Problemas.md`](Docs/09-FAQ-y-Problemas.md) | Problemas frecuentes y soluciones |
| [`Docs/10-Plan-de-Pruebas-Fabian-Rodrigo.md`](Docs/10-Plan-de-Pruebas-Fabian-Rodrigo.md) | Plan de trabajo en dos sesiones |
| [`Docs/11-Registro-de-Cambios.md`](Docs/11-Registro-de-Cambios.md) | Historial de versiones |
| [`Docs/12-Evaluacion-iOS.md`](Docs/12-Evaluacion-iOS.md) | Por qué se eligió la web app en vez de una app nativa para iOS |
| [`Docs/13-Guia-Control-Web-iOS.md`](Docs/13-Guia-Control-Web-iOS.md) | **Control desde iPhone / iPad / navegador** |

---

## Datos técnicos de un vistazo

| Parámetro | Valor |
|---|---|
| SSID / contraseña | `Rovert-Cam` / `rovert2026` |
| IP del rover | `192.168.4.1` |
| Vídeo | MJPEG `multipart/x-mixed-replace`, 640×480, ~12 FPS |
| Control | WebSocket `ws://192.168.4.1/control`, JSON |
| Diagnóstico | `http://192.168.4.1/info` y `http://192.168.4.1/status` |
| Web app (iOS / navegador) | `http://192.168.4.1/` |
| Fotograma suelto | `http://192.168.4.1/capture` |
| UART ESP32→UNO | GPIO32 (TX) / GPIO33 (RX), 38400 8N1 |
| Watchdog del UNO | 400 ms sin trama válida → motores a 0 |
| Watchdog de la ESP32 | 500 ms sin orden de la app → envía `STOP` |

---

## Seguridad (lo que el rover hace siempre)

- Los motores **nunca** arrancan solos: hay que mantener pulsado el control.
- Al soltar, al perder el foco, al ir a segundo plano o al perder la conexión,
  la app manda `STOP` en el acto.
- Hay **tres** barreras de parada independientes (app, ESP32, Arduino).
- El Arduino no acepta tramas mal formadas y nunca mueve los motores por error.

---

## Licencia

Uso académico del curso de Robótica. El código se comparte tal cual para que
los compañeros puedan estudiarlo y modificarlo.
