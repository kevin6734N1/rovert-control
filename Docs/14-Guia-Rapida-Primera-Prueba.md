# 14 — Guía rápida: cómo probar el rover (para Fabián y Rodrigo)

> **Esta es la guía corta y en palabras simples.** Todo lo demás del proyecto
> está en documentos más técnicos, pero con esta ya puedes tener el rover
> funcionando. Si algo falla, al final hay los tres problemas más comunes y
> dónde mirar.

---

## La respuesta corta (léelo primero)

**¿Hay que instalar algo en el móvil? NO.**

El rover es un **servidor**: enciende su propia red Wi-Fi y espera a que alguien
se conecte. La ESP32-CAM lleva dentro una página web con los botones de
moverlo. Tu iPhone es solo el "mando a distancia": se conecta a esa red y abre
esa página.

Lo único que hay que "instalar" va en el **rover**, y se hace **una sola vez**,
desde el portátil, con un cable USB. Después, para siempre: **enciendes el
rover, te conectas al Wi-Fi, abres una dirección en el navegador y ya.**

> Si tu móvil fuera Android, también podrías instalar el APK. Como el vuestro es
> iPhone, no hace falta: la web app hace lo mismo.

---

## 1. Las dos "cajas" del rover (para no perderse)

El rover tiene **dos plaquitas**, y cada una tiene su propio programa:

| Placa | Qué hace | Cómo se le pone el programa |
|---|---|---|
| **ESP32-CAM** (la del zócalo de la cámara) | Crea el Wi-Fi, manda el vídeo, traduce tus órdenes | Cable **micro-USB** |
| **Arduino UNO** (la grande) | Mueve los motores | Cable **USB-B** |

**Los dos programas ya están escritos y probados** (compilan sin errores). No
tenéis que programar nada: solo cargarlos en cada placa.

Los archivos están en el repositorio, en `firmware/`.

---

## 2. Lo que necesitáis (cada uno)

- El rover montado y con las ruedas.
- **Un portátil** con **Arduino IDE 2.x** instalado.
- **Dos cables USB de datos**: uno micro-USB (para la ESP32) y uno USB-B (para
  el UNO). ⚠️ Un cable que solo carga **no sirve**: no lleva datos y el
  ordenador no verá la placa.
- El **iPhone o iPad** de cada uno.
- Un **móvil con hotspot** o el Wi-Fi de casa, para descargar el core de
  Espressif la primera vez (son unos 300 MB). Mejor: en casa, antes de la
  sesión.

---

## 3. En el portátil: cargar los dos programas (una sola vez)

Hazlo con el rover **en la mesa y con las ruedas en el aire**.

### 3.1 Preparar el Arduino IDE

1. Abre **Arduino IDE 2.x**.
2. **Archivo → Preferencias** → al final, en *URLs de gestor de tarjetas
   adicionales*, pega:
   `https://espressif.github.io/arduino-esp32/package_esp32_index.json`
   y dale a **OK**.
3. **Herramientas → Administrar bibliotecas** → busca **esp32 by Espressif
   Systems** → **Instalar** la versión **3.x**. (Son ~300 MB: tarda un rato.
   Hazlo en casa.)
4. En **Herramientas → Administrar bibliotecas**, instala estas dos, que son
   obligatorias:
   - **ESP Async WebServer** (autor *ESP32Async*), versión **3.12.1**
   - **Async TCP** (autor *ESP32Async*), versión **3.5.0**

   > Si te da error al compilar con otras versiones, es que no son estas.
   > Las versiones antiguas (3.6.0 y 2.0.0) **no funcionan** con el core 3.x.

### 3.2 El Arduino UNO (el de los motores)

1. Conecta el UNO por USB al portátil.
2. **Herramientas → Placa → Arduino UNO**.
3. **Herramientas → Puerto →** el COM que aparezca.
4. **Archivo → Abrir…** → `firmware/arduino_uno/MotorController/MotorController.ino`
5. **Herramientas → Verificar** (Ctrl+−): debe decir *"Compilación completada"*.
6. **Herramientas → Subir** (Ctrl+U). Tarda unos segundos.
7. Abre el **Monitor Serie** (`Ctrl+Shift+M`), ponlo a **115200 baudios** y
   final de línea **"Both LF y CR"**. Pulsa **RESET** en el UNO.

   Debe salir algo así:

   ```text
   [ROVERT] Controlador de motores listo (motores detenidos)
   ```

   Y el **LED frontal** (el del cable de 220 Ω) se enciende fijo.

### 3.3 La ESP32-CAM (la del Wi-Fi y la cámara)

1. Conecta la ESP32-CAM por su micro-USB al portátil.
2. **Herramientas → Placa → ESP32 Arduino Boards → ESP32 Wrover Module**.
3. **Herramientas → Esquema de partición → Huge APP (3MB No OTA/1MB SPIFFS)**.
4. **Herramientas → Puerto →** el COM que aparezca.
5. **Archivo → Abrir…** → `firmware/esp32cam/RovertCam/RovertCam.ino`
6. **Herramientas → Verificar**: debe compilar sin errores.
7. **Herramientas → Subir**.
   - Si se queda en *"Connecting…"*: mantén **BOOT**, pulsa **RESET**, suelta
     **BOOT** y vuelve a darle a Subir.
8. Monitor Serie a **115200** y final de línea **"Both LF y CR"**. Pulsa
   **RESET**. Debe salir:

   ```text
   [ROVERT] SoftAP 'Rovert-Cam' creado. IP = 192.168.4.1
   [ROVERT] PSRAM detectada
   [ROVERT] Camara OV2640 inicializada OK
   [ROVERT] Servidor HTTP/WebSocket en marcha
   ```

   Si **no** sale *"PSRAM detectada"*, o sale *"ERROR al iniciar la camara"*,
   para y mira la sección 7.

---

## 4. En el móvil: 4 pasos y listo

1. **Apaga los datos móviles.**
   Ajustes → Datos móviles → **Datos móviles → Desactivado**.

   > **Esto es lo más importante.** El rover no tiene internet, y si el móvil
   > tiene los datos activos, iOS se pasa a la red de la operadora y deja de
   > ver al rover. Es el problema número uno.

2. **Conéctate al Wi-Fi del rover.**
   Ajustes → Wi-Fi → **`Rovert-Cam`** → contraseña **`rovert2026`**.
   iOS dirá *"Esta red no tiene conexión a Internet"*: **ignóralo** y pulsa
   **"Mantener conectado"**.

3. **Abre Safari** y escribe **`http://192.168.4.1`** → "Ir".

4. **Añádelo a la pantalla de inicio** (para usarlo como una app):
   Safari → **Compartir** → **"Añadir a la pantalla de inicio"**.

   Ya tienes el mando en el móvil. No hay nada más que instalar.

> ⚠️ **Orden siempre el mismo:** primero te conectas al Wi-Fi del rover, y
> luego abres la página (o el icono). Si no, no funciona.

---

## 5. La primera prueba (con las ruedas en el aire)

**Regla de oro: las pruebas de motor se hacen con el rover calzado, nunca en el
suelo.** Un motor TT puede llegar a 300 rpm en menos de un segundo: en el suelo,
el rover se va de cabeza contra la mesa o la pared.

1. Apoya el rover en un libro o una caja, con las **ruedas en el aire** y
   libres de girar.
2. Enciende la ESP32 y el Arduino.
3. En el móvil: comprueba arriba a la izquierda que pone **"Conectado"** (punto
   verde) y que se ve la imagen de la cámara.
4. Mantén pulsado **▲**. Los dos motores giran hacia delante.
5. Suelta. Se paran solos.
6. Repite con **▼**, **◀** y **▶**.

### Si un motor gira al revés

No pasa nada y **no hay que tocar el cableado ni recompilar nada**:

1. Toca **⚙** (arriba a la derecha).
2. En **"Polaridad motor izquierdo"** o **"Polaridad motor derecho"**, pon el
   que va al revés en **"Invertida"**.
3. Guarda. Listo.

Esto se ajusta **desde el móvil**, en cualquier momento.

---

## 6. ¿Y el mando de PS4?

1. Conéctalo por **Bluetooth** en los ajustes del iPhone/iPad (debe aparecer
   como *"Controlador de juegos"* o *"Wireless Controller"*).
2. En la web app, toca el botón de abajo **"Mando: no conectado"** para que la
   página vuelva a buscarlo.
3. Cuando lo detecte, pondrá **"Mando: DualShock 4"** y ya manda.

> El mando por Bluetooth en iOS necesita **iOS 16.4 o superior**. Si tenéis
> una versión más antigua, **no pasa nada**: los botones táctiles de la
> pantalla siguen funcionando perfectamente.

---

## 7. Los 3 problemas más comunes (y solo 3)

| Qué pasa | Por qué | Qué hacer |
|---|---|---|
| **La página no carga** o se queda en "Conectando…" | Los datos móviles están encendidos, o no estás en el Wi-Fi `Rovert-Cam` | Apaga los datos móviles, comprueba el Wi-Fi y recarga la página |
| **No sale el vídeo (pantalla negra)** | La cámara no arrancó | Mira el Monitor Serie de la ESP32: tiene que decir *"Camara OV2640 inicializada OK"*. Si dice *"ERROR al iniciar la camara"*, el sketch se compiló con los pines de otra placa: usa el del repositorio sin tocarlo |
| **El IDE no ve la placa (no sale ningún COM)** | Cable solo de carga, o falta el driver | Usa un cable **de datos**. Si es una placa con chip CP210x, instala el driver de Silicon Labs; si es CH340, el de WCH |

Para todo lo demás, `Docs/09-FAQ-y-Problemas.md` tiene el detalle (síntoma →
causa → solución) y `Docs/08-Pruebas-de-Aceptacion.md` tiene la lista de
comprobaciones para delante del profesor.

---

## 8. Resumen en una línea

> Instala **los dos programas en el rover** (una vez, por USB, desde el
> portátil) → enciéndelo → **apaga los datos móviles** → conéctate a
> `Rovert-Cam` → abre **`http://192.168.4.1`** → a conducir.

---

## Documentos relacionados

| Documento | Para qué |
|---|---|
| [`03 — Tabla de conexiones y cableado`](03-Tabla-de-Conexiones-y-Cableado.md) | Qué cable va de dónde (con dibujos) |
| [`06 — Grabar la ESP32](06-Instrucciones-Flash-ESP32.md)` | Detalle de flashear la ESP32 |
| [`07 — Grabar el Arduino](07-Instrucciones-Flash-Arduino.md)` | Detalle de flashear el UNO y probar motores |
| [`08 — Pruebas de aceptación](08-Pruebas-de-Aceptacion.md) | Las 32 comprobaciones |
| [`09 — FAQ y problemas](09-FAQ-y-Problemas.md)` | Todas las averías, una a una |
| [`13 — Control web en iPhone/iPad](13-Guia-Control-Web-iOS.md)` | Detalle de la web app y sus ajustes |
| [`10 — Plan de pruebas](10-Plan-de-Pruebas-Fabian-Rodrigo.md)` | Cómo repartir las dos sesiones |
