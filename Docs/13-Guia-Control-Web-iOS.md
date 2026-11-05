# 13 — Control desde iPhone, iPad o cualquier navegador

> **¿Es tu primera vez con el rover?** Empieza por
> **[`14-Guia-Rapida-Primera-Prueba.md`](14-Guia-Rapida-Primera-Prueba.md)**,
> que lo explica todo en una sentada y en palabras simples. Este documento es
> el detalle de la web app.

> **Para quién es esta guía:** Fabián y Rodrigo (iPhone) y Kevin (iPad).
> Si tienes Android, usa la app nativa: es más fluida. Esta guía es para quien
> **no puede instalar el APK**.

---

## 1. Qué es esto y por qué existe

La ESP32-CAM del rover ya es un **servidor**: sirve el vídeo por HTTP y recibe
las órdenes por WebSocket. La app Android es simplemente *un cliente* de ese
servidor.

Pues bien: **cualquier navegador también puede ser ese cliente**. La ESP32
sirve su propia interfaz web en `http://192.168.4.1/`, así que desde un iPhone
o un iPad se controla el rover **sin instalar absolutamente nada**.

| | App Android (APK) | Web app (navegador) |
|---|---|---|
| Instalación | Descargar e instalar el APK | **Ninguna**: se abre una URL |
| Funciona en iOS | ❌ (Android only) | ✅ (Safari, Chrome, iPadOS) |
| Vídeo | Stream MJPEG continuo (~12 FPS) | Sondeo de fotogramas (~10 FPS) |
| Mando DualShock 4 | Sí (API nativa de Android) | Sí (Gamepad API del navegador, **iOS 16.4+**) |
| Botones táctiles | Sí | Sí |
| Polaridad y velocidad | Sí | Sí (panel de ajustes) |

La decisión de por qué no se hizo una app nativa para iOS está argumentada en
**[`12-Evaluacion-iOS.md`](12-Evaluacion-iOS.md)** (resumen: publicar una app
nativa iOS exige un Mac, Xcode y 99 USD/año de cuenta de desarrollador de Apple;
la web app no exige nada de eso).

---

## 2. Puesta en marcha (5 pasos)

### Paso 1 — Enciende el rover y conéctate a su Wi-Fi

En el iPhone (o iPad):

1. Abre **Ajustes → Wi‑Fi**.
2. Entra en la red **`Rovert-Cam`** — contraseña **`rovert2026`**.
3. iOS te avisará: *"Esta red no tiene conexión a Internet"*. **Ignóralo** y
   pulsa **"Mantener conectado"** (o "Continuar").
   - Si te aparece **"Usar cellular" / "Mantener en cellular"**, **elige
     "Mantener conectado"**, nunca la opción de usar datos móviles.

### Paso 2 — Apaga los datos móviles

En **Ajustes → Datos móviles → Datos móviles → Desactivado**.

> **¿Por qué?** Si los datos móviles están encendidos, iOS puede "cambiar" a la
> red de la operadora en cuanto detecta que `Rovert-Cam` no tiene Internet, y
> entonces el navegador deja de ver al rover. Es el problema número uno en el
> taller. Con los datos apagados, iOS se queda en la red del rover.

*(En el iPad, si tiene eSIM activa, revisa que el Wi‑Fi esté en "Sin conexión"
o simplemente apaga los datos en Configuración → Datos móviles.)*

### Paso 3 — Abre la web app

Abre **Safari** y escribe en la barra de direcciones:

```
http://192.168.4.1
```

Pulsa "Ir". La página tarda 2–3 segundos la primera vez: la ESP32 está
sirviendo el vídeo de la cámara.

Deberías ver:

- arriba a la izquierda, un **punto verde** y la palabra **"Conectado"**;
- en el centro, la **imagen de la cámara**;
- arriba a la derecha, los **FPS** y el botón ⚙ de ajustes;
- abajo, la **cruceta** de movimiento (▲ ◀ ■ ▶ ▼).

### Paso 4 — Añádela a la pantalla de inicio (recomendado)

En Safari, pulsa **Compartir → "Añadir a pantalla de inicio"**.

Se crea un icono como una app más. A partir de ahí se abre a pantalla
completa, sin la barra de direcciones, y **el vídeo empieza mucho más rápido**.

> ⚠️ **No la abras desde el icono sin haber conectado antes al Wi‑Fi del rover.**
> Si el Wi‑Fi no está conectado, la web app no puede hablar con el rover.
> Es el mismo orden siempre: **Wi‑Fi primero, luego icono**.

### Paso 5 — Comprueba que responde

Toca ▲ (Adelante) y mira:

- los **motores giran**,
- el panel de estado pasa de `TÁCTIL` a la dirección,
- la consigna muestra `1.00 / 1.00`.

Al soltar, el rover se para solo (por diseño: hay que **mantener pulsado**).

---

## 3. El panel de ajustes (⚙)

| Ajuste | Para qué sirve | Valor por defecto |
|---|---|---|
| **Servidor del rover** | IP del rover, si no es `192.168.4.1` | el de la página |
| **Puerto** | Puerto HTTP. **Deja 0** para el mismo que usa la página (80) | `0` |
| **Velocidad máxima** | Tope de velocidad: 10 %–100 % | `100 %` |
| **Polaridad motor izquierdo** | **Normal** o **Invertida** | Normal |
| **Polaridad motor derecho** | **Normal** o **Invertida** | Normal |
| **Zona muerta del stick** | Ignora movimientos pequeños del stick (0–0,40) | `0,18` |
| **FPS del vídeo** | Fotogramas por segundo del sondeo (2–20) | `10` |

Los ajustes se guardan en el propio navegador, así que se recuerdan.

### Polaridad de los motores

Es el ajuste que más se va a tocar en el taller. Si al pulsar ▲ **el rover
avanza pero un motor gira al revés**, no pasa nada: abre ⚙ y pon en **Invertida**
el motor que va al revés. También se puede cambiar al revés (pulsar ▼ para que
avance hacia atrás). Los valores se pueden corregir **desde el móvil, sin
volver a grabar el firmware**.

### Usar el mando (DualShock 4)

1. Conecta el mando por **Bluetooth** en los ajustes de iOS (Ajustes →
   Bluetooth). Debe aparecer como "Controlador de juegos sin cables".
2. En Safari, toca el chip **"Mando: no conectado"** de abajo para que la web
   app vuelva a buscar el mando.
3. Cuando lo detecte, el chip pasa a **"Mando: DualShock 4"** (o el nombre que
   tenga) y a partir de ahí manda el stick izquierdo y la cruceta.

> **Requisito:** la Gamepad API del navegador solo existe en **iOS 16.4 o
> superior**. En iOS 16.3 o anterior la web app funciona igual, pero **solo con
> los botones táctiles**. (La app nativa de Android no tiene esta limitación.)

Los botones táctiles siguen funcionando siempre, incluso con el mando
conectado: el mando tiene prioridad cuando se mueve el stick.

---

## 4. Problemas frecuentes (iPhone / iPad)

| Síntoma | Causa probable | Solución |
|---|---|---|
| Safari no abre la página | No estás conectado al Wi‑Fi `Rovert-Cam` | Ajustes → Wi‑Fi → conéctate; recarga la página |
| "Esta red no tiene conexión a Internet" | Aviso normal: el rover no tiene Internet | Pulsa "Mantener conectado" |
| Se queda en "Conectando…" | Los datos móviles están encendidos y iOS salió de la red del rover | **Apaga los datos móviles** y recarga |
| La página carga pero no hay vídeo | La cámara no arrancó o el stream falló | Comprueba en `/info` (ver abajo) que `"camera":true` |
| Va a trompicones | Sondeo a 10 FPS sobre una red Wi‑Fi muy cargada | Baja "FPS del vídeo" a 5–6 en ⚙ |
| Los motores van al revés | Polaridad | ⚙ → Polaridad → Invertida |
| El mando no aparece | iOS < 16.4, o el mando no está conectado por Bluetooth | Actualiza iOS; conecta el mando antes de abrir la web app |
| Aviso "conexión no segura" | iOS 26 avisa de que la página usa HTTP sin cifrar | Es **esperado y no pasa nada**: el tráfico va directo de tu móvil al rover, sin salir a Internet. Pulsa "Continuar" si lo permite. Ver §5 |

### Comprobación rápida del rover

Si algo va mal, abre en el navegador:

```
http://192.168.4.1/info
```

Deberías ver un JSON con `"camera":true` y `"clients":0` o `1`. Si `"camera":false`,
el problema es la cámara, no la web app (mira `Docs/09-FAQ-y-Problemas.md`).

---

## 5. Aviso de "conexión no segura" en iOS 26

A partir de **iOS 26**, Apple muestra un aviso cuando una página se abre por
`http://` en una red local. El aviso es **informativo**: no bloquea el control
del rover, porque todo el tráfico va **de tu móvil al rover directamente** (no
pasa por ningún servidor de Apple ni por Internet, así que no hay nada que
cifrar).

Si aun así el aviso te impide continuar, se puede evitar así:

- **Opción A (recomendada):** usa la web app tal cual y acepta el aviso. El
  control funciona igual.
- **Opción B:** instala la web app en la pantalla de inicio y ábrela desde el
  icono; en muchos casos el aviso solo aparece en la navegación normal.
- **Opción C:** si en tu iOS el aviso sí bloquea, **déjalo anotado y dilo en el
  taller**: habría que mirar la opción de *Ajustes → Apps → Safari → &
  "Aviso de conexión no segura"* (depende de la versión exacta de iOS 26).

> Esto **no se ha podido verificar en un iPhone real** (no hay hardware en el
> taller de pruebas). Es el punto pendiente número uno de la lista de pruebas
> de `Docs/10-Plan-de-Pruebas-Fabian-Rodrigo.md`.

---

## 6. Limitaciones (honestamente)

- El vídeo va a **~10 FPS por sondeo**, no a 12 FPS de stream continuo. Es
  suficiente para ver por dónde va el rover, pero se nota un pelín de retraso.
  La app Android va más fluida.
- **No hay instalación**: cada vez que abres la web app tardas un par de
  segundos en cargar.
- En iOS **anterior a 16.4** no hay soporte de mando.
- La web app **no guarda vídeo ni fotos**; la app Android tampoco.
- Si el navegador iOS mata la pestaña por falta de memoria, hay que volver a
  abrirla (no pasa nunca en uso normal).

---

## 7. Detalles técnicos (por si hay que tocar el código)

- **Código fuente:** `firmware/esp32cam/webapp/` → `index.html`, `app.css`, `app.js`.
- **Cómo llega al firmware:** `node firmware/esp32cam/webapp/build-webapp.js`
  genera `firmware/esp32cam/RovertCam/webapp_generated.h`, que incrusta los tres
  archivos en la flash como literales `PROGMEM` (31.726 bytes). El `.h`
  **está versionado en Git**, así que no hace falta Node.js para compilar.
- **Rutas que sirve la ESP32:** `/` (HTML), `/app.css`, `/app.js`.
- **Protocolo:** exactamente el mismo que la app Android (`Docs/04`). La web app
  envía `{"type":"drive",...}` y `{"type":"stop",...}` por `ws://192.168.4.1/control`.
- **Vídeo:** sondea `GET /capture` y lo pinta en un `<canvas>`. Se hizo así
  porque Safari falla con `fetch()` + `response.body.getReader()` sobre
  `multipart/x-mixed-replace` (error "TypeError: Load failed").
- **Simulador para desarrollo:** `node tools/mock-rover-server.js 8080` levanta un
  rover falso en el PC (con WebSocket propio, sin dependencias) para probar la
  web app sin hardware.

### Cambios en el firmware respecto a la v1.0.0

- `RovertCam.ino`: tres rutas nuevas (`/`, `/app.css`, `/app.js`) + `#include
  "webapp_generated.h"`.
- `config.h`: `ROVERT_INFO_PATH` pasa de `"/"` a `"/info"`; se añaden
  `ROVERT_WEBAPP_INDEX_PATH`, `ROVERT_WEBAPP_CSS_PATH` y `ROVERT_WEBAPP_JS_PATH`.
- `/capture` ahora manda cabeceras `Cache-Control: no-store` para que el
  navegador no sirva fotogramas viejos.
- `RoverClient.fetchDiagnostic()` (app Android) ahora pide `/info`.
- **No se ha tocado nada más**: el protocolo, los pines, el watchdog, los
  firmwares del Arduino y los 38 tests de la app siguen igual.
