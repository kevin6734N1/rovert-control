# Instrucciones para flashear la ESP32-WROVER-CAM

> Esta guía lleva el firmware del rover a la placa **Freenove
> ESP32-WROVER-CAM** desde cero: instalar el core de Espressif, instalar las
> dos librerías que el proyecto usa, configurar la placa y cargar el programa.
> Al final hay una tabla de problemas con su solución.

---

## 1. Qué necesitas

| Elemento | Detalle |
|---|---|
| Placa **Freenove ESP32-WROVER-CAM** | 1 (ESP32-WROVER, 4 MB PSRAM, OV2640 onboard) |
| **Arduino IDE 2.x** | La versión estable 2.x (no el IDE clásico 1.8) |
| **Cable micro-USB de DATOS** | Un cable solo de carga no sirve: no lleva líneas de datos |
| Core **esp32 by Espressif Systems** 3.x | Se instala en el paso 2 |
| Las 2 librerías del proyecto | `ESP Async WebServer` 3.12.1 y `Async TCP` 3.5.0, de ESP32Async (paso 3) |

> **No uses** un cable USB que solo cargue. Es el error número 1: el IDE no ve
> ningún puerto y no hay forma de flashear. Si tienes dudas, prueba con el cable
> del cargador de un móvil antiguo o con uno que sepas que transfiere datos.

---

## 2. Instalar el core de Espressif (ESP32)

El sketch usa la librería de cámara `esp_camera.h`, que viene con el core
oficial de Espressif. Hay que instalarlo.

1. Abre **Arduino IDE 2.x**.
2. Ve a **Archivo → Preferencias** y baja hasta el apartado
   **Additional boards manager URLs** (*URLs de gestor de tarjetas
   adicionales*).
3. Pega esta URL y pulsa **OK**:

   ```text
   https://espressif.github.io/arduino-esp32/package_esp32_index.json
   ```

4. Abre **Herramientas → Boards Manager** (o el icono *Boards Manager* de la
   barra lateral izquierda).
5. Escribe `esp32` en el buscador. Debe aparecer **esp32 by Espressif
   Systems**.
6. Selecciona la **versión 3.x estable más reciente** (por ejemplo
   `3.0.x` o superior) y pulsa **Install**. La descarga es grande (unos
   200–300 MB); tarda unos minutos.
7. Cuando termine, cierra y vuelve a abrir el IDE: la lista de placas se
   actualiza al reiniciar.

> Si prefieres el gestor por interfaz, la misma instalación está en
> **Herramientas → Boards Manager** → tipo `esp32` → **esp32 by Espressif
> Systems**.

---

## 3. Instalar las dos librerías del proyecto

El sketch incluye dos cabeceras que **no** vienen con el core:

```cpp
#include <ESPAsyncWebServer.h>   // servidor HTTP + WebSocket
#include <AsyncTCP.h>             // dependencia del anterior
```

> **Importante — versiones que funcionan con el core 3.x.** El par correcto es:
>
> | Librería | Versión | Autor | Cabecera que aporta |
> |---|---|---|---|
> | **ESP Async WebServer** | **3.12.1** | ESP32Async (Espressif) | `ESPAsyncWebServer.h` |
> | **Async TCP** | **3.5.0** | ESP32Async (Espressif) | `AsyncTCP.h` |
>
> Las versiones antiguas (`ESPAsyncWebServer` 3.6.0 de mathieucarbou +
> `ESPAsyncTCP` 2.0.0 de esphome) son del **core 2.x** y **no compilan** con
> el core 3.x: fallan con `fatal error: AsyncTCP.h: No such file or directory`
> y con `fatal error: debug.h: No such file or directory`. Si te pasa eso, es
> que tienes las librerías equivocadas.

Por eso el repositorio las trae **vendorizadas** en
`firmware/esp32cam/libraries/`. Tienes dos formas de instalarlas.

### 3.1 Opción A — copiar desde el repositorio (la recomendada)

1. Localiza la carpeta del proyecto en tu ordenador, por ejemplo:

   ```text
   C:\Users\kevin\Proyectos\Robert - ESP32 Cam\firmware\esp32cam\libraries
   ```

2. Abre esa carpeta. Dentro hay **dos** carpetas:

   ```text
   libraries\
   ├── Async_TCP\           (library.properties, src\, ...)
   └── ESP_Async_WebServer\ (library.properties, src\, ...)
   ```

3. Abre la carpeta de librerías del IDE (el *sketchbook*). En Windows es:

   ```text
   C:\Users\<tu_usuario>\Documents\Arduino\libraries
   ```

   - Si la carpeta `Arduino` o `libraries` no existe, créalas a mano (una dentro
     de la otra).
   - Si tienes la carpeta **Documentos** redirigida a OneDrive, la ruta real será
     `...\OneDrive\Documentos\Arduino\libraries`. Compruébalo en el Explorador
     de archivos antes de copiar.
4. Copia **las dos carpetas completas** (`Async_TCP` y `ESP_Async_WebServer`)
   dentro de `...\Arduino\libraries`. El resultado debe quedar así:

   ```text
   C:\Users\<tu_usuario>\Documents\Arduino\libraries\
   ├── Async_TCP\
   └── ESP_Async_WebServer\
   ```

   > Si ya las instalaste desde el **Library Manager** con esos mismos nombres,
   > copiar encima simplemente las sobrescribe: no hay problema. Lo que **no**
   > debe haber es una segunda copia con otro nombre (por ejemplo la antigua
   > `ESPAsyncWebServer`), porque el IDE se quejaría de *"multiple libraries
   > found"*.

5. Reinicia el Arduino IDE.

> Copia la **carpeta entera**, no el contenido suelto: la librería se reconoce
> por su fichero `library.properties`, que está en la raíz de cada carpeta.

### 3.2 Opción B — desde el Library Manager

Si prefieres no copiar a mano:

- Abre **Herramientas → Administrar bibliotecas** (*Manage Libraries*).
- Busca **`ESP Async WebServer`** — autor **ESP32Async** — versión **3.12.1**, e
  instálala.
- Busca **`Async TCP`** — autor **ESP32Async** — versión **3.5.0**, e instálala.

> Estas son los mismas librerías que trae el repositorio, así que las dos
> opciones son equivalentes. Lo importante es la **versión**: no instales las
> antiguas 3.6.0 / 2.0.0, que son del core 2.x y no compilan con el core 3.x.

> Si mezclas las dos opciones (una copiada y otra instalada), puede haber dos
> versiones de la misma librería y el IDE se queja de "multiple libraries found".
> Deja **una sola** versión de cada una.

### 3.3 Comprobar que las librerías están

Con el sketch abierto (paso 4), el menú **Sketch → Verify** debe compilar sin
errores de "library not found". Si aparece:

```text
fatal error: ESPAsyncWebServer.h: No such file or directory
```

es que la librería no está en la carpeta correcta: vuelve al paso 3.1.

---

## 4. Abrir el sketch

1. **Archivo → Abrir…** (o `Ctrl+O`).
2. Navega a:

   ```text
   firmware\esp32cam\RovertCam\RovertCam.ino
   ```

3. El IDE abre el sketch `RovertCam` con sus dos ficheros:
   `RovertCam.ino` y `config.h` (las constantes de configuración: SSID,
   contraseña, pines de la cámara, pines del UART y tiempos).
4. **No muevas `config.h`**: el sketch lo incluye con `#include "config.h"`, y
   debe seguir en la misma carpeta.

---

## 5. Configuración de la placa

Es el paso donde más se falla, así que ve despacio: revisa la tabla y luego
aplica los menús uno a uno.

| Parámetro | Valor que debes elegir | Menú en Arduino IDE 2.x |
|---|---|---|
| **Placa** | **ESP32 Wrover Module** (o *Freenove ESP32 WROVER CAM* si tu IDE lo muestra) | Herramientas → Placa |
| **Esquema de partición** | **Huge APP (3MB No OTA/1MB SPIFFS)** | Herramientas → Esquema de partición |
| **Velocidad de carga** | **115200** si el menú aparece (ver nota) | Herramientas → Velocidad de carga |
| **Puerto** | El COM/TTY de la placa | Herramientas → Puerto |
| **Frecuencia de CPU** | 240 MHz (valor por defecto) | Herramientas → Frecuencia de CPU |
| **USB CDC On Boot** | Disabled (por defecto) | Herramientas → USB CDC On Boot |

> Con la placa **ESP32 Wrover Module** el core ya deja fijados el **tamaño de
> flash (4 MB)** y la **PSRAM**, así que **no aparecen** los menús *Tamaño de
> flash* ni *PSRAM*: no hace falta tocarlos. Si tu IDE sí los muestra (IDE
> antiguo o placa mal detectada), pon **4 MB** y **Enabled**.

### 5.1 Notas de la tabla

- **PSRAM.** Con la placa *ESP32 Wrover Module* viene activada por el propio
  core. Es **imprescindible**: sin PSRAM detectada la cámara no puede reservar
  los fotogramas JPEG y el firmware avisa con
  `[ROVERT] AVISO: no se detecta PSRAM, el stream MJPEG no funcionara bien`.
- **Partición "Huge APP (3MB No OTA/1MB SPIFFS)".** El esquema por defecto del
  core para 4 MB es *Default 4MB with spiffs*, que deja solo **1,2 MB** para la
  aplicación: el sketch ocupa ~1,1 MB, así que entraría **justo y sin margen**.
  Con *Huge APP* hay 3 MB de aplicación y **no** usas OTA (que en un taller no
  hace falta).
- **Velocidad de carga.** En el ESP32 clásico (con puente USB-serie) el menú
  *Velocidad de carga* puede no aparecer: el core lo ofrece solo en las placas
  con USB nativo. **Si aparece, pon 115200**: es más lento pero mucho más
  fiable con el puente USB-serie de la placa. Si no aparece, deja el valor por
  defecto y no te preocupes.
- **Puerto.** Conecta el cable micro-USB *antes* de abrir el menú de puertos.
  En Windows aparecerá como `COM3`, `COM4`… (el número cambia). Si no aparece
  nada, mira la tabla de problemas del §8 (drivers).

### 5.2 Orden de los menús, paso a paso

```text
1.  Herramientas → Placa → ESP32 Arduino Boards → ESP32 Wrover Module
2.  Herramientas → Esquema de partición → Huge APP (3MB No OTA/1MB SPIFFS)
3.  Herramientas → Puerto → COMx  (el de tu placa)
4.  Herramientas → Verificar   (Ctrl+-)  → debe compilar sin errores
```

### 5.3 Alternativa: compilar desde la línea de comandos

Si quieres saltarte el IDE, el repositorio trae un script que compila **los dos
firmwares** con `arduino-cli` (instala él mismo el core esp32 3.x y el core
`arduino:avr`):

```powershell
powershell -ExecutionPolicy Bypass -File tools\build-firmware.ps1
```

Los binarios quedan en `tools\build\esp32\` (`RovertCam.ino.bin`) y
`tools\build\uno\` (`MotorController.ino.hex`).

> Con esta configuración (`esp32:esp32:esp32wrover:PartitionScheme=huge_app`)
> el firmware compila **sin un solo aviso**: 1.095.104 bytes de programa (34 %
> de 3 MB) y 58.956 bytes de RAM.

El último paso (Verificar) es importante: si el sketch compila y dice
*"Compilación completada"* o *"Compilation complete"*, tienes las librerías y la
placa bien configuradas. Solo entonces pasa a flashear.

---

## 6. Flashear la placa

La placa Freenove lleva un **USB-serie onboard** y los dos botones que hacen
falta: **BOOT** (GPIO0) y **RESET** (EN).

1. Con la placa conectada por USB y el IDE con el puerto correcto.
2. Pulsa el botón **▶ Subir** (*Upload*).
3. El IDE compila y empieza a subir. En muchos proyectos basta con esperar.
4. Si el IDE se queda en *"Connecting…"* o falla, haz la **secuencia manual**:

   ```text
   1. Mantén pulsado el botón BOOT
   2. Pulsa y suelta el botón RESET
   3. Suelta el botón BOOT
   4. Vuelve a pulsar ▶ Subir
   ```

   Este "triángulo" de botones es el método universal para forzar el modo
   bootloader (GPIO0 a nivel bajo al reiniciar).
5. Cuando termina, el IDE muestra *"Se ha subido el programa"* / *"Upload
   complete"* y la placa se reinicia sola.

### 6.1 Antes de flashear: motor parado

Si el rover ya está cableado, **desconecta la alimentación de los motores**
(puedes dejar el USB del Arduino desconectado). El firmware arranca siempre con
los motores detenidos, pero flashear el Arduino puede reiniciarlo y un reinicio
con una orden activa sería peligroso. Lo correcto es trabajar con el rover
**sin ruedas en el suelo**.

---

## 7. Verificación: qué debe verse en el Monitor Serie

1. Abre **Herramientas → Monitor Serie** (o `Ctrl+Shift+M`).
2. Pon los **baudios a 115200** y la **finalización de línea en "Both LF y CR"**
   (o *Newline*). El firmware usa `Serial.begin(115200)`.
3. Pulsa **RESET** en la placa.

Salida esperada, línea a línea:

```text
=========================================
  ROVERT — ESP32-CAM  v1.0.0
=========================================
[ROVERT] SoftAP 'Rovert-Cam' creado. IP = 192.168.4.1
[ROVERT] UART2 listo: TX=32 RX=33 a 38400 baudios
[ROVERT] PSRAM detectada
[ROVERT] Camara OV2640 inicializada OK
[ROVERT] Servidor HTTP/WebSocket en marcha
```

Qué significa cada línea:

| Línea | Significado |
|---|---|
| `SoftAP 'Rovert-Cam' creado. IP = 192.168.4.1` | La red Wi-Fi del rover ya existe: búscala en el móvil |
| `UART2 listo: TX=32 RX=33 a 38400 baudios` | El enlace con el Arduino está configurado (aún no dice que el Arduino conteste) |
| `PSRAM detectada` | **Correcto**: la memoria PSRAM del WROVER está activa. Si sale el aviso de "no se detecta PSRAM", revisa Herramientas → PSRAM |
| `Camara OV2640 inicializada OK` | La cámara arranca. Si sale `ERROR al iniciar la camara: 0x...`, los pines de la cámara no son los del WROVER |
| `Servidor HTTP/WebSocket en marcha` | El rover ya responde en `http://192.168.4.1/` |
Líneas adicionales cuando la app se conecta:

```text
[ROVERT] Cliente WebSocket conectado (id 0)
[ROVERT] Cliente de video conectado
```

y cuando la app se desconecta:

```text
[ROVERT] STOP: cliente WebSocket desconectado
```

### 7.1 Comprobación final con el navegador

Con el móvil (o el portátil) conectado a la red `Rovert-Cam`, abre:

```text
http://192.168.4.1/
```

Debe abrirse la **web app de control** (la cruceta y el vídeo). Ese mismo
enlace es el que usan Fabián y Rodrigo desde sus iPhones, sin instalar nada
(guía completa en `Docs/13-Guia-Control-Web-iOS.md`).

Y para el **diagnóstico en JSON**:

```text
http://192.168.4.1/info
```

Debe devolver algo así:

```json
{"device":"Rovert ESP32-CAM","fw":"1.0.0","ssid":"Rovert-Cam",
 "ip":"192.168.4.1","camera":true,"clients":1,"uptimeMs":12345,
 "state":"stopped","left":0.000,"right":0.000}
```

> Ojo: el diagnóstico **ya no está en `/`**. La raíz `/` la ocupa ahora la web
> app, así que `/` devuelve HTML y el JSON está en **`/info`**.

En `http://192.168.4.1/stream` se ve el vídeo de la cámara en cualquier
navegador (es el stream MJPEG que usa la app Android).

---

## 8. Tabla de problemas

| Síntoma | Causa probable | Solución |
|---|---|---|
| **No aparece ningún puerto** en Herramientas → Puerto | Cable solo de carga, o falta el driver USB-serie | Cambia a un cable **de datos**. Si el puerto sigue sin aparecer, instala el driver del conversor de tu placa: la Freenove usa **CP210x/CP2102** en algunas revisiones y **CH340** en otras; mira la serigrafía del chip junto al USB. En Windows, *Administrador de dispositivos* → *Puertos (COM y LPT)* debe mostrar el dispositivo |
| **`Failed to connect to Espressif device: No serial data received`** | El IDE no consigue hablar con el chip | Cierra el Monitor Serie (solo uno puede usar el puerto), pulsa RESET, y reintenta con la secuencia BOOT → RESET → BOOT del §6 |
| **`A fatal error occurred: Failed to connect to ESP32: Timed out waiting for packet header`** | La placa no entró en modo bootloader | Repite la secuencia manual de botones. Mantén BOOT **hasta** que el IDE muestre *"Connecting…"* |
| **`Invalid head of packet`** | Velocidad de carga demasiado alta | Pon Herramientas → Velocidad de carga = **115200** y vuelve a subir |
| **`fatal error: ESPAsyncWebServer.h: No such file or directory`** | Las librerías no están instaladas | Copia las dos carpetas a `...\Documents\Arduino\libraries` (§3.1) y reinicia el IDE |
| **`fatal error: AsyncTCP.h: No such file or directory`** o **`fatal error: debug.h: No such file or directory`** | Librerías de la versión antigua (core 2.x) mezcladas con las nuevas | Borra las librerías antiguas (`ESPAsyncTCP`, `ESPAsyncWebServer`) de `...\Arduino\libraries` y copia las del repositorio: `Async_TCP` 3.5.0 y `ESP_Async_WebServer` 3.12.1 (§3) |
| **La placa se reinicia en bucle** (`Brownout detector was triggered`) | Alimentación insuficiente: el pack no da los 1 A de pico | Usa un convertidor buck/boost de 5 V **≥ 1 A**, revisa que los conectores no estén flojos y que la batería tenga carga. Ver la entrada 12 de `Docs/09-FAQ-y-Problemas.md` |
| **`[ROVERT] ERROR al iniciar la camara: 0x...`** | Pines de cámara incorrectos (casi siempre: valores de AI-Thinker) | No toques `config.h`: la tabla de la WROVER ya es correcta. Comprueba que `CAM_PIN_*` sigue como en el repositorio y que la placa es una WROVER, no una AI-Thinker |
| **`[ROVERT] AVISO: no se detecta PSRAM`** | PSRAM desactivado en el IDE o sketch mal compilado | Herramientas → PSRAM → **Enabled**, y comprueba que has seleccionado **ESP32 Wrover Module** (no *ESP32 Dev Module*) |
| **La red `Rovert-Cam` no aparece en el móvil** | La placa no arrancó bien o la IP del móvil no cambia | Mira el Monitor Serie: si no ves `SoftAP 'Rovert-Cam' creado`, el firmware no arrancó. Comprueba que el móvil está en la misma banda (2,4 GHz) y desactiva cualquier ahorro de energía de la Wi-Fi del móvil |
| **El Monitor Serie muestra caracteres raros** | Baudios equivocados | Pon **115200** baudios y final de línea *Both LF y CR* |

---

## Si algo falla

Este documento cubre el camino previsto, pero el inventario de problemas reales
del proyecto (brownouts, cámaras que no arrancan, redes que no aparecen,
divisores mal puestos) está en **`Docs/09-FAQ-y-Problemas.md`**. Si un punto de
esta guía no coincide con lo que ves, busca el síntoma ahí antes de tocar el
cableado.
