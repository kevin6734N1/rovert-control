# Evaluación: llevar el control del rover a iPhone e iPad

> Fecha: 2026-11-04 · Motivo: Fabián y Rodrigoavliron a decir que usan **iOS**, no Android.
> Conclusión corta: **el rover no cambia nada**. La ESP32-CAM ya habla HTTP y
> WebSocket; el problema es únicamente cómo entregar la *app cliente* a unos
> iPhones sin tener un Mac ni una cuenta de Apple de pago.

---

## 1. Lo que NO hay que tocar

| Pieza | ¿Cambia? | Por qué |
|---|---|---|
| Firmware ESP32-CAM | **No** | El protocolo (HTTP + WebSocket + JSON) es independiente del sistema operativo del cliente. |
| Firmware Arduino UNO | **No** | Solo recibe tramas UART. |
| App Android (APK) | **No** | Sigue funcionando igual para quienes tengan Android. |
| App classmates (Fabián, Rodrigo) | **Sí** | Necesitan un cliente iOS. |

El rover es un **servidor** (SoftAP + HTTP + WebSocket). Cualquier cliente que
hable ese protocolo sirve: una app nativa, una web app, un script de Python.

---

## 2. El obstáculo real: publicar en iOS

### 2.1 Compilar

Apple no permite compilar apps de iOS fuera de macOS. Confirmado en la
documentación oficial de Kotlin/Native:

> *"Building final binaries for Apple targets on Linux and Windows is also not
> possible."* — [Kotlin/Native supported targets and hosts](https://kotlinlang.org/docs/native-target-support.html)

Esto aplica a **todas** las rutas (Kotlin Multiplatform, Flutter, React Native,
Swift): para producir un binario de iOS hace falta Xcode, que solo corre en macOS.

**Salida: un runner macOS de GitHub Actions.** Los repositorios públicos tienen
2.000 minutos/mes de runners macOS gratis, así que el build del `.ipa` se puede
automatizar en CI sin tener un Mac en casa.

### 2.2 Firmar y distribuir

Aquí está el verdadero cuello de botella, y no es técnico:

| Vía | Requiere | Caducidad | Dificultad para el compañero |
|---|---|---|---|
| **TestFlight** | Apple Developer Program (**99 USD/año**) | 90 días por build | Baja: instala la app TestFlight y acepta la invitación |
| **Sideload con Apple ID gratis** | Un Mac + Xcode una vez | **7 días** | Media: hay que re-firmar cada semana |
| **Sideload con certificado de pago** | 99 USD/año + Mac | 1 año | Media: instalar perfil y confiar en el certificado |
| **AltStore / SideStore** | Un dispositivo "de apoyo" siempre encendido | 7 días (se renueva solo) | Media |
| **PWA servida por la propia ESP32** | **Nada** | Inmediato | **Nula: abrir una URL** |

Es decir: **cualquier app iOS nativa exige, en algún momento, 99 USD al año**
(o un dance de re-firmado cada 7 días). Ese es el motivo por el que la opción
recomendada abajo no es "la app nativa es mala", sino "la app nativa es
carísima de distribuir para dos compañeros de clase".

---

## 3. Las cuatro opciones, evaluadas

### Opción A — PWA servida por la propia ESP32 ⭐ **recomendada**

La ESP32 sirve además una web app (HTML + JS + CSS) en `http://192.168.4.1/`.
El iPhone se conecta al Wi-Fi del rover, abre Safari y tiene el panel de control
completo: vídeo, cruceta táctil, mando y STOP.

- **Instalación:** ninguna. Se abre una URL.
- **Coste:** 0 USD, 0 cuentas, 0 Mac.
- **Riesgo para la app Android:** ninguno. La PWA es *aditiva*.
- **Trabajo:** ~1 jornada (web app de ~600 líneas + un handler en el firmware).

Requisitos de iOS que **sí** cumple (verificados):

| Requisito iOS | Estado en nuestra PWA |
|---|---|
| Permiso de "red local" (iOS 14+) | **No aplica**: según la nota TN3179 de Apple, *"Traffic originating from WKWebView, SFSafari, and Safari doesn't require local network access"*. Ese permiso solo se pide a las apps nativas. |
| ATS (App Transport Security) | **No aplica**: ATS solo restringe a apps nativas. Safari puede usar `http://` sin problema. |
| Mixed content (página https → ws://) | **No aplica**: la página se sirve desde la propia ESP32 en `http://`, así que el WebSocket `ws://192.168.4.1/control` es *same-origin*. |
| Gamepad API en Safari | **Sí**: disponible en Safari de iOS desde **iOS 16.4**. El DualShock 4 se empareja por Bluetooth con el iPhone y se lee igual que en Android. |
| Streaming MJPEG (`multipart/x-mixed-replace`) | **Problema conocido**: `fetch()` + `response.body.getReader()` sobre un `multipart/x-mixed-replace` falla en Safari con *"TypeError: Load failed"* ([caso documentado](https://stackoverflow.com/questions/79577116/fetching-a-mjpeg-video-stream-on-ios-throws-typeerror-load-failed-error)). **Solución: usar el endpoint `/capture`** (un JPEG por petición) a ~10 FPS y pintarlo en un `<canvas>`. Así no hay multipart y funciona en todos los navegadores. |

Limitaciones honestas de la PWA:
- No hay vibración del mando (Safari no expone la Gamepad Haptics API) — irrelevante para este proyecto.
- Sin modo "app instalada" real: en iOS, *Add to Home Screen* sobre una página `http://` abre una ventana propia, pero sin service worker (eso exige HTTPS). Se pierde el icono y el modo a pantalla completa.
- Si el navegador pasa a segundo plano, el WebSocket se corta → **la ESP32 para el rover** por su watchdog. Comportamiento seguro y deseable.
- Para iPhone hace falta **iOS 16.4 o superior** si se quiere usar el mando.

### Opción B — App iOS nativa con Kotlin Multiplatform

Refactorizar el núcleo a `commonMain` y escribir la capa iOS con Compose Multiplatform.

- Reutilización real: `model/`, `protocol/` y `ControlEngine` son Kotlin puro y ya están separados; `net/` habría que pasarlo de OkHttp a **Ktor** (OkHttp no existe en iOS); `video/` habría que rehacerlo con Skia (`decodeToImageBitmap`) en vez de `BitmapFactory`; `input/` habría que reescribirlo contra el framework **GameController** de iOS.
- **Build:** solo en macOS → workflow de GitHub Actions con runner `macos-15`.
- **Distribución:** TestFlight → **99 USD/año obligatorios**.
- **Riesgo:** el refactor toca la app Android que ya funciona y está verificada. Mitigable, pero es trabajo real (días, no horas).
- Nota: el target `iosArm64` de Kotlin/Native **no se puede ni compilar** en Windows, así que la mitad iOS solo se validaría en CI.

### Opción C — App iOS nativa en SwiftUI

- Código completamente nuevo: ~1.500–2.000 líneas de Swift. **Cero reutilización** del trabajo en Kotlin.
- Mismo coste de distribución (99 USD/año) y mismo requisito de macOS.
- Única ventaja: acceso a APIs de iOS más "nativas" y mejor rendimiento del vídeo (puede usar `AVSampleBufferDisplayLayer`).
- En términos de esfuerzo/resultado, **la peor opción** aquí.

### Opción D — Flutter

- Reescribir todo en Dart. Ni una línea del trabajo actual se reutiliza.
- El build de iOS también exige macOS. La distribución es la misma.
- Descartada.

### Resumen comparativo

| | A: PWA | B: KMP nativa | C: SwiftUI | D: Flutter |
|---|---|---|---|---|
| Instalación para el compañero | Abrir URL | TestFlight | TestFlight | TestFlight |
| Coste anual | **0 USD** | 99 USD | 99 USD | 99 USD |
| ¿Necesita Mac? | No | No (CI) | No (CI) | No (CI) |
| Reutiliza el código actual | El firmware, 100 % | ~60 % | 0 % | 0 % |
| Trabajo estimado | ~1 jornada | 3–5 jornadas | 5–7 jornadas | 5–7 jornadas |
| Riesgo para la app Android | Ninguno | Medio | Bajo | Bajo |
| Mando DualShock 4 | Sí (iOS 16.4+) | Sí | Sí | Sí |
| Vídeo | `/capture` a 10 FPS | MJPEG nativo | MJPEG nativo | MJPEG nativo |

---

## 4. Recomendación

**Opción A (PWA) como entrega inmediata**, y **dejar el núcleo preparado**
para la B si más adelante hay Mac y presupuesto.

Motivos:

1. **Elimina el bloqueo de distribución**, que es el único problema serio. No
   hay cuenta de Apple, ni 99 USD, ni caducidad de 7 días, ni "confía en el
   certificado" para Fabián y Rodrigo. Ellos conectan el Wi-Fi y abren Safari.
2. **No toca nada de lo que ya funciona.** La app Android sigue igual, los
   firmwares siguen igual, y los tests siguen verdes.
3. **Es la misma experiencia para todos.** Fabián, Rodrigo y Kevin pueden usar
   exactamente el mismo panel en el iPhone y en el iPad. Bonus: también sirve
   como plan B en Android si a alguien le falla la instalación del APK.
4. **El rover ya es un servidor.** Añadir un `index.html` al servidor es
   conceptually la solución natural, no un apaño.

Y si en el futuro se decide ir a la app nativa: la PWA no se tira, sirve como
interfaz de diagnóstico, y la arquitectura del proyecto (protocolo documentado
en `Docs/04`, núcleo ya separado en `model/` + `protocol/` + `ControlEngine`)
ya está en la forma correcta para un `commonMain`.

---

## 5. Qué habría que hacer en la opción A

1. **Firmware ESP32**
   - Servir `/` (o `/app/`) con el `index.html`, y `/app/app.js`, `/app/app.css`.
   - Servir los estáticos desde PROGMEM (`PROGMEM const char INDEX[]`).
   - El WebSocket `/control` **no cambia**: es el mismo que ya usa la app Android.
   - Reutilizar el endpoint `/capture` que ya existe para el vídeo de la PWA.

2. **Web app** (`firmware/esp32cam/webapp/`)
   - Panel con `<canvas>` para el vídeo (polling de `/capture`).
   - Cruceta táctil con Pointer Events (`pointerdown` / `pointerup` / `pointercancel`).
   - Lectura del mando con la **Gamepad API** (`navigator.getGamepads()`), con zona
     muerta configurable.
   - WebSocket a `ws://<host>/control` con el mismo JSON que la app Android.
   - Reglas de seguridad idénticas: `stop` al soltar, al perder el foco, al
     desconectar y al ocultar la pestaña (`visibilitychange`).

3. **Documentación**
   - `Docs/12-Guia-Control-Web-iOS.md`: cómo abrir el panel en iPhone e iPad,
     cómo emparejar el DualShock 4 con el iPhone y qué hacer si iOS pregunta por
     la red local.
   - Actualizar `README.md` y `Docs/02` con la segunda vía de acceso.

4. **Pruebas**
   - Un mock en Node.js que imite los endpoints de la ESP32 para poder probar la
     web app en un navegador de escritorio **sin hardware**.

---

## 6. Fuentes consultadas

- [Kotlin/Native supported targets and hosts](https://kotlinlang.org/docs/native-target-support.html) — los binarios de Apple no se compilan en Windows ni Linux.
- [Kotlin Multiplatform mobile FAQ](https://kotlinlang.org/docs/multiplatform-mobile-faq.html) — el plugin KMP Mobile solo funciona en macOS.
- [Kotlin Multiplatform quickstart](https://kotlinlang.org/docs/multiplatform/quickstart.html) — hace falta macOS + Xcode para crear apps iOS.
- [Apple TN3179 — Understanding local network privacy](https://developer.apple.com/documentation/technotes/tn3179-understanding-local-network-privacy) — el tráfico de Safari no requiere permiso de red local.
- [Apple — If an app would like to connect to devices on your local network](https://support.apple.com/en-us/102229) — el permiso solo afecta a apps nativas.
- [Stack Overflow — fetching a MJPEG stream on iOS throws "TypeError: Load failed"](https://stackoverflow.com/questions/79577116/fetching-a-mjpeg-video-stream-on-ios-throws-typeerror-load-failed-error) — el problema de `multipart/x-mixed-replace` con `fetch` en Safari.
- [caniuse — Gamepad API](https://caniuse.com/gamepad) — soporte en Safari de iOS.
- [Gamepad API en iOS/Safari 16.4+ y pares Bluetooth](https://gpadlab.com/learn/guides/testing-controllers-on-mobile) — el DualShock 4 funciona en iPhone.
- [Apple Developer Forums — Local network connection incluso denegada](https://developer.apple.com/forums/thread/789461) — matices del permiso de red local en iOS 18/26.
