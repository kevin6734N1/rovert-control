# Índice de la documentación — Rovert

> Toda la documentación del proyecto está en esta carpeta. Estos tres archivos
> son el punto de partida para cualquiera que reciba el rover.

---

## Ruta rápida

### Soy Fabián o Rodrigo y me pasaron el APK

1. **[02 — Guía de instalación del APK](02-Guia-de-Instalacion-APK.md)**
   Instalar la app, conectar el Wi-Fi del rover, emparejar el mando.
2. **[03 — Tabla de conexiones y cableado](03-Tabla-de-Conexiones-y-Cableado.md)**
   Conectar la ESP32, el Arduino, el L298N, la cámara, los motores y las baterías.
3. **[06 — Instrucciones para grabar la ESP32](06-Instrucciones-Flash-ESP32.md)**
   y **[07 — Instrucciones para grabar el Arduino](07-Instrucciones-Flash-Arduino.md)**.
4. **[08 — Pruebas de aceptación](08-Pruebas-de-Aceptacion.md)**
   La lista que hay que ir marcando delante del profesor.
5. **[09 — FAQ y problemas](09-FAQ-y-Problemas.md)** cuando algo no vaya bien.

### Tengo iPhone o iPad (no puedo instalar el APK)

1. **[13 — Control desde iPhone, iPad o cualquier navegador](13-Guia-Control-Web-iOS.md)**
   Conéctate al Wi-Fi `Rovert-Cam` y abre **`http://192.168.4.1`**. No hay que
   instalar nada: la propia ESP32 sirve la interfaz.
2. **[09 — FAQ y problemas](09-FAQ-y-Problemas.md)** si algo no va.
3. **[12 — Evaluación de iOS](12-Evaluacion-iOS.md)** para saber por qué se
   eligió esta opción y no una app nativa.

### Quiero entender o modificar el proyecto

| Documento | Contenido |
|---|---|
| **[01 — Arquitectura y decisiones de diseño](01-Arquitectura-y-Decisiones.md)** | Por qué se eligió cada cosa y qué alternativas se descartaron |
| **[04 — Protocolo de comandos](04-Protocolo-Comandos.md)** | **Contrato** entre app, ESP32 y Arduino. Si lo cambias, cámbialo en los tres lados |
| **[05 — Protocolo UART ESP32–Arduino](05-Protocolo-UART-ESP32-Arduino.md)** | Tramas, watchdog y diagramas de tiempos |
| **[13 — Control desde iPhone / iPad / navegador](13-Guia-Control-Web-iOS.md)** | Cómo se usa la web app y dónde está su código fuente |
| **[11 — Registro de cambios](11-Registro-de-Cambios.md)** | Historial de versiones y qué se rompió/arregló |

---

## Índice completo

| # | Documento | Descripción |
|---|---|---|
| 00 | [Índice](00-INDICE.md) | Este archivo |
| 01 | [Arquitectura y decisiones de diseño](01-Arquitectura-y-Decisiones.md) | Vista general, decisiones con su porqué, estructura del repo, capas de la app |
| 02 | [Guía de instalación del APK](02-Guia-de-Instalacion-APK.md) | Instalación en Android, conexión al Wi-Fi del rover, uso del mando |
| 03 | [Tabla de conexiones y cableado](03-Tabla-de-Conexiones-y-Cableado.md) | Pinout completo, cableado, alimentación, listas de material, verificación |
| 04 | [Protocolo de comandos](04-Protocolo-Comandos.md) | Red, endpoints HTTP, WebSocket, tabla de comandos, reglas de seguridad |
| 05 | [Protocolo UART ESP32–Arduino](05-Protocolo-UART-ESP32-Arduino.md) | Enlace físico, tramas, watchdog, tiempos |
| 06 | [Instrucciones para grabar la ESP32](06-Instrucciones-Flash-ESP32.md) | Arduino IDE, placa, librerías, proceso de grabación, diagnóstico |
| 07 | [Instrucciones para grabar el Arduino](07-Instrucciones-Flash-Arduino.md) | Preparación, grabación, prueba de motores, polaridad |
| 08 | [Pruebas de aceptación](08-Pruebas-de-Aceptacion.md) | Checklist verificable de todos los requisitos |
| 09 | [FAQ y problemas](09-FAQ-y-Problemas.md) | Symptoma → causa → solución |
| 10 | [Plan de pruebas para Fabián y Rodrigo](10-Plan-de-Pruebas-Fabian-Rodrigo.md) | Plan de dos sesiones, responsabilidades, riesgos |
| 11 | [Registro de cambios](11-Registro-de-Cambios.md) | Versiones, cambios y problemas resueltos |
| 12 | [Evaluación de iOS](12-Evaluacion-iOS.md) | Comparación de rutas (PWA, KMP, SwiftUI, Flutter) y por qué se eligió la PWA |
| 13 | [Control desde iPhone / iPad / navegador](13-Guia-Control-Web-iOS.md) | Guía paso a paso de la web app, ajustes, problemas y límites |

---

## Convenciones

- **SSID del rover:** `Rovert-Cam` — contraseña: `rovert2026`
- **IP del rover:** `192.168.4.1`
- **Puerto HTTP/WebSocket:** `80`
- **Web app (iPhone/iPad/navegador):** `http://192.168.4.1/` — la sirve la propia
  ESP32. Código en `firmware/esp32cam/webapp/`.
- **Enlace con el Arduino:** UART 38400 8N1
- Todos los textos de la app están en **español**, igual que esta documentación.
- Cuando un documento menciona un pin, **siempre** se refiere a la placa
  **Freenove ESP32-WROVER-CAM** (no a la AI-Thinker ESP32-CAM, que tiene
  otra tabla de pines incompatible).

---

## Glosario rápido

| Término | Significado |
|---|---|
| **SoftAP** | La ESP32-CAM crea su propia red Wi-Fi; no hace falta router |
| **MJPEG** | Vídeo formado por JPEG sueltos que se van enviando por HTTP |
| **WebSocket** | Conexión persistente para mandar órdenes y recibir estado |
| **Watchdog** | Temporizador de seguridad que detiene el rover si algo falla |
| **PSRAM** | Memoria externa rápida de la WROVER, necesaria para el vídeo |
| **Strapping pin** | GPIO que se lee al arrancar para configurar el chip (0, 2, 5, 12, 15) |
| **L298N** | Módulo puente H que controla los motores con dos señales PWM |
| **SoftwareSerial** | UART por software del Arduino, para no usar D0/D1 (USB) |
