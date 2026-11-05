# FAQ y resolución de problemas

> Catálogo de las incidencias reales del proyecto: qué pasa, por qué pasa y qué
> hacer. Están ordenadas de la más frecuente a la menos. Si tu síntoma no está
> aquí, mira el **árbol de diagnóstico** del principio y luego el apartado
> **"Si nada de esto funciona"** del final.

---

## Árbol de diagnóstico rápido

Antes de tocar nada, responde estas tres preguntas:

```text
1. ¿El móvil está conectado a la red "Rovert-Cam"?
      NO → problema de red o de alimentación del rover (entradas 2, 3 y 12)
      SÍ ↓

2. ¿El navegador abre http://192.168.4.1/ ?
      NO → el rover no está sirviendo: firmware, alimentación o cableado (3, 4, 12)
      SÍ ↓

3. ¿La app pone "Conectado"?
      NO → problema de la app o del WebSocket (2)
      SÍ ↓

   El rover no responde a los comandos → problema de la cadena
   ESP32 → UART → Arduino → L298N → motores (6, 7, 8, 11)
```

| Nº | Síntoma | Ir a |
|---|---|---|
| 1 | El móvil dice «Sin Internet» | [1](#1-el-móvil-dice-sin-internet) |
| 2 | La app dice «Sin conexión» | [2](#2-la-app-dice-sin-conexión) |
| 3 | El vídeo sale negro | [3](#3-el-vídeo-sale-negro-pero-la-app-conecta) |
| 4 | No hay vídeo en absoluto | [4](#4-no-hay-vídeo-en-absoluto) |
| 5 | El rover va al revés | [5](#5-el-rover-acuation-al-pulsar-adelante) |
| 6 | Una rueda gira invertida | [6](#6-una-rueda-gira-al-revés) |
| 7 | El rover no se mueve | [7](#7-el-rover-no-se-mueve-en-absoluto) |
| 8 | El rover se mueve de forma errática | [8](#8-el-rover-se-mueve-pero-de-forma-errática) |
| 9 | La app se cierra al girar el móvil | [9](#9-la-app-se-cierra-al-girar-el-teléfono) |
| 10 | El mando no conecta | [10](#10-el-mando-no-conecta) |
| 11 | El mando conecta pero el rover no se mueve | [11](#11-el-mando-conecta-pero-el-rover-no-se-mueve) |
| 12 | La ESP32 se reinicia al arrancar los motores | [12](#12-la-esp32-se-reinicia-cuando-arrancan-los-motores) |
| 13 | El Arduino se reinicia al flashearlo | [13](#13-el-arduino-se-reinicia-al-flashearlo) |
| 14 | El rover se mueve solo tras una desconexión | [14](#14-el-rover-se-mueve-solo-tras-una-desconexión) |
| 15 | Cambiar el nombre y la contraseña de la Wi-Fi | [15](#15-cambiar-el-nombre-o-la-contraseña-de-la-wi-fi) |
| 16 | Cambiar la IP del rover | [16](#16-cambiar-la-ip-del-rover) |
| 17 | Cambiar la resolución de la cámara | [17](#17-cambiar-la-resolución-de-la-cámara) |
| 18 | Consultar el nivel de batería | [18](#18-consultar-el-nivel-de-batería) |
| 19 | Actualizar el firmware | [19](#19-actualizar-el-firmware) |
| 20 | Desinstalar la app | [20](#20-desinstalar-la-app) |
| 21 | El vídeo va a trompicones | [21](#21-el-vídeo-va-a-trompicones-o-se-congela) |
| 22 | El rover tarda mucho en arrancar | [22](#22-el-rover-tarda-mucho-en-arrancar) |

---

## 1. El móvil dice «Sin Internet»

**Síntoma.** Al conectar el móvil a `Rovert-Cam`, Android avisa: *"Esta red no
tiene acceso a Internet"* o *"Sin Internet, se te desconectará más adelante"*.

**Causa.** El rover crea su propia red Wi-Fi (SoftAP) y **no hay ningún router
ni conexión a Internet detrás**. Es el diseño del proyecto, no un fallo.

**Solución.** Pulsa **«Mantener conectado» / «Mantener la conexión» / «Sí,
quiero conectarme»**. Si eliges la opción de desconectarse, el móvil volverá a
los datos móviles y la app no verá al rover.

**Consecuencia a tener en cuenta.** Mientras estés en esa red, **el móvil deja
de usar datos móviles**: WhatsApp, el correo y el navegador se quedan sin
Internet. Para volver a la normalidad, desconéctate de `Rovert-Cam` en
Ajustes → Wi-Fi.

**Comprobación.** Abre `http://192.168.4.1/` en el navegador del móvil: si ves
el JSON de diagnóstico, la red funciona perfectamente.

---

## 2. La app dice «Sin conexión»

**Síntoma.** La app no pasa de *"Conectando…"* o muestra *"Sin conexión"* /
*"Error de conexión"* aunque el navegador sí abra `http://192.168.4.1/`.

**Causas y soluciones, en orden de probabilidad:**

1. **La IP de los ajustes no es la correcta.** Abre el icono de **Ajustes**
   (engranaje) y comprueba que **IP del rover = `192.168.4.1`** y
   **Puerto = 80**.
2. **El rover se reinició y tardó un poco.** La app reintenta sola con un
   backoff de 1 s a 10 s: espera 10 segundos antes de declarar el fallo.
3. **El WebSocket está ocupado.** Solo puede haber **una** app conectada al
   WebSocket `/control` a la vez. Si otro móvil (o el navegador con una
   pestaña abierta) está conectado, el nuevo no entra. Cierra las otras apps y
   la pestaña del navegador.
4. **Batería del móvil agotándose o Wi-FiSaver.** Desactiva el ahorro de datos
   y el ahorro de batería de la app (Ajustes → Apps → Rovert Control → Batería
   → Sin restricciones).
5. **El móvil se quedó con la red del router.** Comprueba el nombre de la red
   en la barra de notificaciones.

**La app no necesita Internet**: si el móvil está en `Rovert-Cam`, ya puede
hablar con el rover.

---

## 3. El vídeo sale negro pero la app conecta

**Síntoma.** La app está *Conectado*, el panel de vídeo sale completamente
negro, pero el resto (estado, orden) funciona.

**Causas y soluciones:**

1. **La cámara no arrancó.** Mira el **Monitor Serie de la ESP32 a 115200**:
   - Si aparece `[ROVERT] ERROR al iniciar la camara: 0x...`, el problema es de
     inicialización de la cámara (ver entrada 4).
   - Si aparece `[ROVERT] AVISO: no se detecta PSRAM, el stream MJPEG no
     funcionara bien`, la cámara no puede reservar los fotogramas JPEG: revisa
     **Herramientas → PSRAM → Enabled** y que la placa seleccionada sea
     **ESP32 Wrover Module**.
2. **La cámara está bien pero la app no pide el stream.** Pulsa **Reintentar**
   en el panel de vídeo, o cierra y abre la app.
3. **En el navegador sí hay imagen pero en la app no.** Comprueba que la app
   apunta al rover correcto (Ajustes → IP). Y comprueba que no hay otra app
   abierta consumiendo el stream.

**Comprobación directa.** Abre `http://192.168.4.1/stream` en el navegador: si
ves la imagen ahí, la cámara funciona y el problema es de la app.

---

## 4. No hay vídeo en absoluto

**Síntoma.** Ni la app ni el navegador muestran imagen; a veces el navegador
devuelve un error 503.

**Causas y soluciones:**

1. **La cámara no se inicializó.** En el Monitor Serie (115200) debe aparecer
   `[ROVERT] Camara OV2640 inicializada OK`. Si sale un error, revisa:
   - **Los pines de la cámara.** Los de la **Freenove ESP32-WROVER-CAM** son
     los de `config.h` (`CAM_PIN_*`). Son **distintos** de los de la AI-Thinker
     ESP32-CAM. Si alguien cambió los pines, vuelve a copiar el fichero
     `config.h` del repositorio.
   - **La placa es la correcta.** El sketch está escrito para la WROVER.
2. **Falta PSRAM.** Sin ella, el stream MJPEG no funciona (mensaje de aviso en
   el Monitor Serie).
3. **La resolución es demasiado alta.** `FRAMESIZE_UXGA` (1280×1024) o
   `FRAMESIZE_QXGA` sin PSRAM provocan reinicios. Baja a `FRAMESIZE_VGA`.
4. **El stream se corta.** En el Monitor Serie verás
   `[ROVERT] camera_fb_get() devolvió NULL: fin del stream` si la cámara se
   queda sin fotogramas: normalmente es falta de corriente (entrada 12) o
   resolución demasiado alta.
5. **Abre `http://192.168.4.1/capture`** en el navegador: descarga **un solo
   JPEG**. Si descarga una imagen, la cámara funciona y el problema es del
   stream o de la app.

---

## 5. El rover acciona al pulsar «Adelante»

*(Síntoma: al pulsar Adelante, el rover va hacia atrás.)*

**Causas y soluciones:**

1. **Polaridad invertida en el firmware.** Es el caso más común: el motor
   derecho está montado "del revés". Solución: en
   `firmware/arduino_uno/MotorController/MotorController.ino`, cambia **una sola**
   constante:
   ```cpp
   const bool LEFT_MOTOR_INVERT  = false;   // ← ponlo true si el izquierdo va al revés
   const bool RIGHT_MOTOR_INVERT = true;    // ← ponlo false si el derecho va al revés
   ```
   y vuelve a flashear el Arduino (`Docs/07-Instrucciones-Flash-Arduino.md`).
2. **Polaridad invertida en la app (sin tocar el firmware).** Ajustes →
   **Invertir motor izquierdo** / **Invertir motor derecho**. Es la vía
   rápida: la app corrige la consigna antes de enviarla.
3. **Los dos motores van al revés a la vez.** Eso no es polaridad: es que el
   signo del contrato está invertido de lado a lado. Comprueba que en el
   Monitor Serie del Arduino la telemetría `R,…` muestra valores **positivos**
   cuando pides avanzar.

> Recuerda: el signo `+` significa avance. En la tabla del contrato, adelante es
> `left=+1.0, right=+1.0`.

---

## 6. Una rueda gira al revés

**Síntoma.** El rover va recto pero con las ruedas girando en sentidos
opuestos; o gira sobre el sitio al pulsar Adelante.

**Solución 1 — en la app (30 segundos, sin desmontar nada).**
Ajustes → activa **Invertir motor izquierdo** o **Invertir motor derecho**
(el que corresponda) y prueba otra vez.

**Solución 2 — en el firmware (definitiva).**
Cambia **una sola** constante `LEFT_MOTOR_INVERT` / `RIGHT_MOTOR_INVERT` y
reflashea. **No inviertas los dos motores a la vez** ni cambies el cableado de
`OUT1..OUT4`: con una sola constante se arregla.

**Diagnóstico rápido.** Con las ruedas en el aire y pulsando Adelante:
- los dos giran hacia delante → correcto;
- uno hacia delante y otro hacia atrás → ese es el que hay que invertir;
- los dos hacia atrás → ver entrada 5.

---

## 7. El rover no se mueve en absoluto

**Síntoma.** La app está *Conectado*, muestra la orden *Adelante*, pero las
ruedas no giran.

**El mejor diagnóstico está en el Monitor Serie del Arduino (115200).** Mira las
tramas de telemetría `R,<izq>,<der>,<seq>` que se emiten cada 100 ms:

| Lo que ves | Dónde está el problema | Qué hacer |
|---|---|---|
| `R,500,500,42` (valores no nulos) | El enlace y la app **funcionan** | El problema está **después** del Arduino: L298N o motores (sigue abajo) |
| `R,0,0,7` siempre en 0 | El Arduino **no recibe** órdenes | Enlace UART: divisor mal puesto, GND no común, o la ESP32 no envía (comprueba que el Monitor Serie de la ESP32 muestra `[ROVERT] UART2 listo: TX=32 RX=33 a 38400 baudios`) |
| Tramas `W,<seq>` repetidas | El **watchdog** está venciendo | El Arduino no recibe tramas válidas: revisa D9/D10 y el divisor |

Si el comando sí llega (`R,500,500,…`) pero los motores no giran:

1. **Jumpers ENA/ENB del L298N puestos.** Quítalos: si están, el L298N ignora el
   PWM de D3/D6.
2. **El L298N no tiene alimentación de motores.** Mide en el terminal
   `+12V (Vs)`: debe haber tensión de la pila 18650 (unos 7,4 V).
3. **El L298N no tiene masa común** con el Arduino.
4. **Los motores están mal conectados** a `OUT1..OUT4`, o sus cables están
   sueltos.
5. **Los pines del Arduino están mal cableados** (D3→ENA, D6→ENB, D8→IN1,
   D7→IN2, D12→IN3, D11→IN4). Revisa la tabla del documento de cableado.
6. **El rover está con las ruedas bloqueadas** o chocando con el chasis.

---

## 8. El rover se mueve pero de forma errática

**Síntoma.** Se mueve a tirones, se frena solo, o se para cada dos segundos.

**Causas y soluciones:**

1. **Alimentación débil.** Es la causa más común. Si el rover se mueve solo al
   30 % de velocidad, o se para "a mitad", la pila o el convertidor no dan
   corriente. Pásalo al 100 %: si el problema desaparece, es falta de
   alimentación (ver entrada 12).
2. **Pérdida de paquetes en el Wi-Fi.** La app manda órdenes a 10 Hz; si la
   señal es débil, se pierde alguna y el **watchdog de 500 ms de la ESP32**
   frena el rover. Soluciones: acercar el móvil al rover, alejarlo del Wi-Fi
   del router, o bajar la velocidad máxima al 50 % para hacer las pruebas.
3. **Conectores de motor sueltos.** Un motor que da tirones suele tener un
   contactor oxidado o flojo. Aprieta y limpia los conectores.
4. **Interferencia con el USB.** Si el USB del Arduino está conectado con el
   rover en marcha, el **ruido de los motores** puede garabatear el UART por
   software. Solución: desconecta el USB del Arduino durante las pruebas de
   movimiento, o baja `maxSpeed`.
5. **Bajo voltaje de la pila 18650.** Carga las pilas antes de la demostración.

---

## 9. La app se cierra al girar el teléfono

**Síntoma.** Al rotar el móvil, la app parece cerrarse o volver a la pantalla
de inicio.

**Realidad del proyecto.** La app tiene la **orientación bloqueada en
vertical** (`android:screenOrientation="portrait"` en el manifiesto) y además
declara `configChanges` para `orientation|screenSize|screenLayout|keyboardHidden|uiMode|density|fontScale`. Eso significa que **Android no destruye y recrea la actividad al girar**: la app **no se cierra**, simplemente se queda en vertical y el contenido se recoloca.

**Si de verdad se cierra, el motivo es otro:**

1. **Android mata el proceso por falta de memoria.** Es normal en móviles
   antiguos con muchas apps abiertas. Cierra otras apps y prueba otra vez.
2. **La pantalla se apagó o el sistema pidió la contraseña de bloqueo.** La app
   manda el `stop` en cuanto pierde el foco, por seguridad (esto es
   intencionado y es lo correcto).
3. **Un fallo real de la app.** Anota qué estabas haciendo y repórtalo; no es
   un comportamiento esperado.

**Recomendación de uso.** Trabaja siempre en vertical: es el diseño de la
interfaz (D-pad + STOP + vídeo).

---

## 10. El mando no conecta

**Síntoma.** La app muestra *"Mando: ninguno"* y el DualShock 4 no aparece en
los ajustes de Bluetooth.

**Solución paso a paso:**

1. **El mando se empareja con el TELÉFONO**, no con el rover (el rover no tiene
   Bluetooth). Emparéjalo en **Ajustes → Conexiones → Bluetooth**.
2. **Abre el Bluetooth** del móvil si estaba apagado.
3. **Pon el mando en modo emparejamiento**: mantén pulsado el botón **PS**
   hasta que la barra de luz **parpadee en blanco rápidamente** (dos destellos
   rápidos). Si solo parpadea despacio, ya estaba emparejado con otro
   dispositivo: para emparejarlo con otro, hay que.resetearlo manteniendo
   pulsado **SHARE + PS** unos segundos.
4. Toca **Emparejar dispositivo nuevo** en el móvil. Si te pide un PIN, el del
   DualShock 4 es **0000** o **1234**.
5. **Permiso de dispositivos cercanos**: en Android 10 o superior, el sistema
   pide permiso de *ubicación* para descubrir dispositivos Bluetooth. Si lo denegaste,
   ve a **Ajustes → Aplicaciones → Rovert Control → Permisos** y actívalo.
6. Comprueba que el móvil **ve** el mando en la lista de Bluetooth antes de
   abrir la app.

**Prueba rápida.** Abre la app y mira la línea del mando: si aparece
*"Mando: Wireless Controller"* (o el nombre que tenga), el mando **sí** está
conectado.

---

## 11. El mando conecta pero el rover no se mueve

**Síntoma.** La app muestra el mando conectado, pero al mover el stick el
rover no responde.

**Causas y soluciones:**

1. **Modo equivocado.** En cuanto mueves el stick la app cambia sola de
   **TÁCTIL** a **MANDO** y libera la cruceta táctil, así que normalmente no
   hay que hacer nada. Pero si has pulsado **STOP** antes, el rover **no**
   vuelve a moverse hasta que sueltes todos los controles (el STOP tiene
   prioridad).
2. **Zona muerta demasiado alta.** Por defecto es del **18 %**: si el stick
   está en el centro, la app lo considera neutro. Sube el valor en Ajustes →
   **Zona muerta del stick** (rango 0–40 %) o empuja el stick más a fondo.
   Si el rover se mueve solo con el stick en el centro, **baja** la zona
   muerta.
3. **El rover no está conectado.** El mando solo genera órdenes: si la app
   dice *Sin conexión*, no hay a quién mandárselas. Resuelve antes la entrada 2.
4. **No estás usando el stick izquierdo.** El proyecto usa el **stick
   izquierdo** (gas + dirección). El derecho no está mapeado a movimiento. Si
   el stick está en el centro, la **cruceta** del mando actúa como respaldo.
5. **Los ejes están al revés.** En el stick de PS4, el eje vertical es
   invertido en el sistema: el firmware de la app ya lo corrige (empujar
   arriba = avanzar). Si notas que hacia arriba va hacia atrás, es un problema
   de asignación de ejes del mando, no de la app.

---

## 12. La ESP32 se reinicia cuando arrancan los motores

**Síntoma.** Al pulsar Adelante, la ESP32 se reinicia (se oye el *bip* de
arranque), se cae la red `Rovert-Cam` y el rover se para. En el Monitor Serie
sale:

```text
Brownout detector was triggered
```

**Causa.** La ESP32 se queda sin corriente. Al arrancar los motores, la
corriente del L298N crea una caída de tensión en la línea de alimentación de 5 V
de la ESP32. La placa necesita **al menos 1 A de pico** y una alimentación
estable.

**Solución, en este orden:**

1. **Alimenta los motores desde otro origen.** El pack de 18650 va al L298N;
   la ESP32 se alimenta del pack de 4× AA con su convertidor buck/boost. Nunca
   desde el mismo regulador que los motores.
2. **Usa un convertidor buck/boost de 5 V capable de ≥ 1 A** (2 A mejor).
3. **Acorta y engrosa los cables** de alimentación de la ESP32. Los cables finos
   y largos son la mitad del problema.
4. **Añade un condensador de 470–1000 µF** (electrolítico, con la polaridad
   correcta) **en paralelo** entre el pin 5 V y el GND de la ESP32, lo más
   cerca de la placa posible. Amortigua los picos de corriente.
5. **Comprueba el empalme del portapilas**: las pilas AA en serie pierden
   tensión y las contacto sucio.
6. **Baja el límite de velocidad** al 50 % mientras pruebas: reduce el pico de
   corriente de los motores.

**Aviso:** si el problema es la **placa** y no la alimentación, revisar que el
cable USB no esté alimentando la ESP32 a la vez que el convertidor (dos
fuentes de 5 V enfrentadas). Usa **una** u otra.

---

## 13. El Arduino se reinicia al flashearlo

**Síntoma.** Al subir el programa al UNO, la placa se reinicia, el IDE da
errores de comunicación, o el rover reacciona mientras se programa.

**Por qué no debería pasar.** El sketch usa **SoftwareSerial en D10 (RX) y D9
(TX)**, **no** el UART de hardware (D0/D1, que están conectados al USB). Por
eso el USB puede quedarse conectado y se puede reprogramar el rover montado.

**Qué hacer si te pasa:**

1. **Cierra el Monitor Serie** antes de subir: solo un programa puede usar el
   puerto serie a la vez.
2. **Desconecta temporalmente el cable de la ESP32 hacia D10** mientras
   flasheas. Si la salida de la ESP32 (GPIO32) sigue conectada a D10, hay dos
   salidas peleando por el mismo pin.
3. **Desconecta el USB de la ESP32** mientras flasheas el Arduino: al reiniciar
   la ESP32 durante su propio flasheo, sus pines quedan en alto impedance y
   pueden generar tramas basura. El parser del Arduino las descarta (una trama
   inválida **nunca** mueve los motores), pero es más limpio trabajar por
   partes.
4. **Mantén las ruedas en el aire** mientras flasheas: un reinicio a mitad de
   una orden activa movería el rover.
5. Si el UNO se reinicia **al mover los motores** (no al flashear), mira la
   entrada 8: suele ser ruido eléctrico sobre el UART por software.

---

## 14. El rover se mueve solo tras una desconexión

**Síntoma.** Tras perder la conexión, el rover **sigue moviéndose** un rato (o
no se para).

**Esto es un fallo crítico de seguridad.** El proyecto tiene **tres barreras**
independientes, y todas deben parar el rover:

| Barrera | Tiempo | Qué hace |
|---|---|---|
| App | Inmediato | Manda `stop` al soltar el control, al perder el foco, al cerrar el WebSocket o al desconectar el mando |
| ESP32 | **500 ms** | Si no llega ninguna orden, envía `S` (STOP) al Arduino |
| Arduino | **400 ms** | Si no llega ninguna trama válida, pone los motores a 0 y avisa con `W` |

**Diagnóstico:**

1. Mira el **Monitor Serie del Arduino**: si aparecen tramas `W,<seq>` o el
   mensaje `[ROVERT] WATCHDOG: motores detenidos por falta de tramas`, el
   watchdog **está funcionando**.
2. Mira el **Monitor Serie de la ESP32**: si aparece
   `[ROVERT] STOP: cliente WebSocket desconectado` o
   `[ROVERT] STOP: timeout de ordenes (watchdog de la ESP32)`, la segunda
   barrera está funcionando.
3. Comprueba que el Arduino realmente usa el watchdog: el sketch debe tener
   `const uint16_t WATCHDOG_MS = 400;`.

**Si el rover sigue moviéndose**, revisa por este orden:

- ¿La conexión entre ESP32 y Arduino está **cortada** de verdad? Si solo se
  cayó el Wi-Fi, la ESP32 sigue viva y es ella la que debe parar el rover.
- ¿El **cable de datos está bien** en D9/D10? Un cable mal puesto deja a la
  ESP32 creyendo que habla con el Arduino.
- ¿El motor está **alimentado desde otro sitio** (por ejemplo, la lógica del
  L298N desde el 5 V del Arduino pero los motores por otra vía)? Comprueba que
  la parada en el L298N corta realmente la corriente del motor.
- ¿Estás probando con **ruedas en el aire**? Un rover que gira libre "parece"
  que sigue moviéndose por inercia aunque los motores estén parados.

**Prueba de verificación (la de la tabla RF-24).** Con el rover en marcha,
desconecta el cable D9/D10 y cronometra: los motores deben parar en menos de
400 ms.

---

## 15. Cambiar el nombre o la contraseña de la Wi-Fi

Los valores están en **un solo fichero**: `firmware/esp32cam/RovertCam/config.h`.

```cpp
#define ROVERT_WIFI_SSID      "Rovert-Cam"
#define ROVERT_WIFI_PASSWORD  "rovert2026"     // WPA2, mínimo 8 caracteres
#define ROVERT_WIFI_CHANNEL   6
```

1. Edita esas tres líneas.
2. Vuelve a flashear la ESP32 (`Docs/06-Instrucciones-Flash-ESP32.md`).
3. La red antigua desaparece y aparece la nueva.

**Recordatorio.** Si cambias el SSID, actualiza también el campo **Red Wi-Fi
del rover** en los Ajustes de la app (es solo informativo: quien se conecta a la
red es el sistema operativo del móvil, no la app). Si cambias la contraseña,
**no** hay nada que tocar en la app.

---

## 16. Cambiar la IP del rover

**En la app (no requiere recompilar nada).** Ajustes → **IP del rover** →
escribe la nueva dirección → **Guardar**. La app construye sola las URLs
(`http://<ip>:<puerto>/stream`, `ws://<ip>:<puerto>/control`).

**En el firmware.** Aquí hay que ser honesto: **la ESP32 del proyecto no
configura una IP personalizada**. El sketch solo hace:

```cpp
WiFi.mode(WIFI_AP);
WiFi.softAP(ROVERT_WIFI_SSID, ROVERT_WIFI_PASSWORD, ROVERT_WIFI_CHANNEL);
IPAddress ip = WiFi.softAPIP();     // → 192.168.4.1
```

Es decir, la IP `192.168.4.1` es la que el ESP32 asigna por defecto a su
SoftAP, y **cambiarla requiere tocar el código**: habría que añadir en
`setup()` una llamada a `WiFi.softAPConfig()` con la IP, la puerta de enlace y
el DNS que quieras, y volver a flashear.

**Antes de hacer nada de eso**, comprueba que el problema no es otro: en
Ajustes, el campo **IP del rover** debería decir `192.168.4.1` y el **Puerto**
`80`. Si es así, la app ya apunta a la dirección correcta.

---

## 17. Cambiar la resolución de la cámara

En `firmware/esp32cam/RovertCam/config.h`:

```cpp
#define CAMERA_QUALITY      12     // 0-63. 10-14 = buena calidad y rápido
#define CAMERA_FB_COUNT     2      // 2 con PSRAM (WROVER). 1 si da problemas
#define CAMERA_FRAMESIZE    FRAMESIZE_VGA
#define CAMERA_MAX_FRAMESIZE FRAMESIZE_SVGA
```

| Valor | Resolución | Cuándo usarlo |
|---|---|---|
| `FRAMESIZE_QVGA` | 320×240 | Wi-Fi flojo o stream muy pesado |
| `FRAMESIZE_HVGA` | 400×296 | Fallback rápido |
| `FRAMESIZE_VGA` | **640×480** | **Valor por defecto: el punto dulce** (10–15 FPS) |
| `FRAMESIZE_SVGA` | 800×600 | Si el vídeo va a trompicones y sobra memoria |
| `FRAMESIZE_XGA` / `FRAMESIZE_UXGA` | 1024×768 / 1280×1024 | **No usar**: sin PSRAM la cámara se queda sin buffers y la ESP32 se reinicia en bucle |

Para cambiarlo: edita `CAMERA_FRAMESIZE`, reflashea la ESP32 y comprueba que en
el Monitor Serie sigue apareciendo `[ROVERT] Camara OV2640 inicializada OK`.
Si la imagen sale borrosa o entrecortada, ajusta también `CAMERA_QUALITY`
(más bajo = más calidad pero menos FPS).

**También se puede cambiar el brillo desde el propio firmware** con
`s->set_brightness(s, n)`, pero el proyecto no lo expone por HTTP: hay que
reprogramar.

---

## 18. Consultar el nivel de batería

**No está implementado.** Seamos claros:

- El JSON de diagnóstico de `http://192.168.4.1/` **no incluye** ningún campo
  de batería ni de tensión.
- El endpoint `/status` tampoco lo incluye.
- La app **no muestra** ninguna lectura de batería.
- El firmware **no mide** la tensión de las pilas: no hay divisor resistivo
  conectado a ningún ADC, y el ESP32 no puede leer la tensión del portapilas
  tal como está montado.

**Qué puedes hacer hoy:**

1. **Medir con el polímetro** los voltios del portapilas con el rover
   encendido (o en la entrada de 5 V de la ESP32).
2. **Estimar por autonomía**: con el pack de 4× AA, la ESP32 + cámara +
   Wi-Fi consumen bastante; mide cuánto aguanta el rover con la luz encendida
   para calibrar la autonomía.
3. Para **implementarlo** haría falta: un divisor resistivo hacia un ADC del
   ESP32 (por ejemplo, GPIO34, que es de solo entrada y está libre), leer el
   voltaje en el firmware y añadir un campo al JSON de `/status`.

---

## 19. Actualizar el firmware

**Para la ESP32 (sistema, cámara, Wi-Fi, WebSocket):**

1. Abre `firmware/esp32cam/RovertCam/RovertCam.ino` en el Arduino IDE 2.x.
2. Revisa la configuración de placa (§5 de `Docs/06-Instrucciones-Flash-ESP32.md`).
3. **▶ Subir**. La placa se reinicia y ya tiene la versión nueva.
4. Comprueba en el Monitor Serie (115200) que aparece el banner
   `ROVERT — ESP32-CAM  v1.0.0` y que la red `Rovert-Cam` aparece.

**Para el Arduino (motores, polaridad, watchdog):**

1. Abre `firmware/arduino_uno/MotorController/MotorController.ino`.
2. Revisa las constantes de polaridad.
3. **▶ Subir** (ruedas en el aire).
4. Comprueba que el Monitor Serie (115200) muestra
   `[ROVERT] Controlador de motors listo (motores detenidos)`.

**Para la app Android:** la app se actualiza instalando el APK nuevo encima
del anterior (desinstalando antes la versión vieja si cambiaste el nombre del
paquete). No hay nada que recompilar en el rover.

**Orden recomendado.** Actualiza primero el **Arduino** y después la
**ESP32**: así, si la ESP32 nueva habla con un Arduino viejo, el UART sigue
funcionando (mismo protocolo a 38400) y el rover no se queda a medias.

---

## 20. Desinstalar la app

1. **Ajustes → Aplicaciones → Rovert Control → Desinstalar**
   (o mantener pulsado el icono en el cajón → *Desinstalar*).
2. En Android 10 o superior puede pedir confirmación: *¿Quieres desinstalar la
   app y sus datos?* → **Desinstalar**.
3. Los ajustes (IP, velocidad, polaridad, zona muerta) se borran con la app:
   no queda rastro en el teléfono.

Desinstalar la app **no afecta al rover**: ni la ESP32 ni el Arduino guardan
estado de la app.

---

## 21. El vídeo va a trompicones o se congela

**Síntoma.** La imagen se ve a saltos, el contador de FPS es bajo o se queda
congelada.

**Diagnóstico y solución:**

1. **Baja la resolución** a `FRAMESIZE_HVGA` o `FRAMESIZE_QVGA` (entrada 17) y
   reflashea. Es la solución más rápida.
2. **Acerca el móvil al rover.** La red del SoftAP es de 2,4 GHz y se degrada
   rápido con paredes y gente alrededor.
3. **Comprueba la distancia al router**: si el router está en el mismo canal
   (canal 6 por defecto), hay interferencias. Cambia
   `ROVERT_WIFI_CHANNEL` en `config.h` (por ejemplo a 1 o 11) y reflashea.
4. **Baja la calidad JPEG**: `CAMERA_QUALITY` de 12 a 15 reduce el tamaño de
   cada fotograma.
5. **Cierra otras apps que usen la cámara o la red** del móvil.
6. **Comprueba la alimentación**: si además el rover se frena solo, es un
   problema de corriente (entrada 12), no de vídeo.

**Prueba de referencia.** En `http://192.168.4.1/stream` el navegador muestra
el stream; si ahí va fluido y en la app no, el problema es del móvil (app o
decodificador), no del firmware.

---

## 22. El rover tarda mucho en arrancar

**Síntoma.** Al encender, la red `Rovert-Cam` no aparece hasta bastante
después, o el vídeo tarda en verse.

**Comportamiento normal.** El arranque incluye: iniciar la PSRAM, inicializar
la cámara OV2640 (que negocia por SCCB con el sensor) y levantar el servidor
HTTP. En un par de segundos debería estar todo listo.

**Si tarda más de 10 segundos:**

1. **Mira el Monitor Serie a 115200**: ahí se ve en qué punto se queda.
2. **`[ROVERT] PSRAM detectada` que no aparece** → revisa
   Herramientas → PSRAM → Enabled y la placa seleccionada.
3. **`[ROVERT] ERROR al iniciar la camara`** → la cámara no responde: revisa
   los pines de `config.h` y que la placa sea la WROVER (entrada 4).
4. **Batería baja** → con pilas casi agotadas la ESP32 puede reiniciarse en
   bucle (brownout). Carga las pilas.
5. **La red aparece pero el vídeo no** → es un problema de stream, no de
   arranque (entradas 3, 4 y 21).

---

## 23. La web app en iPhone / iPad no conecta

**Síntoma.** Abres `http://192.168.4.1` en Safari y se queda en *"Conectando…"*,
o la página ni siquiera carga.

**Solución, en este orden:**

1. **¿Estás conectado al Wi-Fi `Rovert-Cam`?** Es lo primero. Si no estás, la
   página no puede encontrar al rover.
2. **¿Has apagado los datos móviles?** Es la causa número uno. Con los datos
   activos, iOS detecta que `Rovert-Cam` no tiene Internet y **te saca de esa
   red** para pasarte a la de la operadora. Ajustes → Datos móviles → **Datos
   móviles → Desactivado**.
3. **Pulsa "Mantener conectado"** cuando iOS avise de que la red no tiene
   Internet. Si eliges "Usar cellular", todo lo anterior pierde el sentido.
4. **Recarga la página** (Safari → el botón ↻ de la barra de direcciones).

Si la página carga pero **el vídeo va a trompicones**, baja el ajuste
**"Fotogramas por segundo del vídeo"** a 5–6 en el panel ⚙.

Si tienes **iOS 16.4 o superior**, el mando DualShock 4 se detecta solo por
Bluetooth. Por debajo de esa versión, la web app funciona pero **solo con los
botones táctiles**.

La guía completa está en
**[13 — Control desde iPhone, iPad o cualquier navegador](13-Guia-Control-Web-iOS.md)**.

---

## 24. ¿Dónde está la app para iOS? ¿Puedo instalarla?

**No hay app nativa para iOS, y no hace falta.** La ESP32-CAM sirve su propia
interfaz web en `http://192.168.4.1/`, así que desde cualquier navegador
(iPhone, iPad, Android, PC) se controla el rover igual que desde la app.

El porqué está argumentado con fuentes en
**[12 — Evaluación de iOS](12-Evaluacion-iOS.md)**: publicar una app nativa
para iOS exigiría un Mac con Xcode y una cuenta del Apple Developer Program
(99 USD/año), y compilar Kotlin para iOS desde Windows no es posible.

**Diferencias entre la app Android y la web app:**

| | App Android | Web app |
|---|---|---|
| Vídeo | Stream continuo (~12 FPS) | Sondeo de fotogramas (~10 FPS) |
| Instalación | Sí (APK) | No |
| Mando | Sí, en cualquier Android 7+ | Sí, en iOS 16.4+ / navegadores modernos |
| Ajustes de polaridad y velocidad | Sí | Sí |

---

## Si nada de esto funciona

1. **Vuelve al punto de partida conocido.** Flashea los dos firmwares otra vez
   desde el repositorio, sin tocar `config.h`. Si el problema desaparece, el
   cambio que hiciste era la causa.
2. **Repasa la checklist de verificación** de
   `Docs/03-Tabla-de-Conexiones-y-Cableado.md`: masa común, divisor de tensión
   y alimentación de los motores son los tres errores que explican el 90 % de
   los fallos "misteriosos".
3. **Aísla por partes**: ESP32 sola (que cree la red), Arduino solo (que
   imprima su mensaje de arranque) y luego juntos. Así sabrás qué bloque falla.
4. **Repasa las pruebas de aceptación** (`Docs/08-Pruebas-de-Aceptacion.md`):
   las pruebas RF-01…RF-25 te dirán exactamente qué parte del sistema falla.
5. **Anota todo**: qué pillaste, qué esperabas, qué pasó. Con esa información
   el problema se resuelve en la mitad de tiempo.
