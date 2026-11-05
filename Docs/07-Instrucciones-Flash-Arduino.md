# Instrucciones para flashear el Arduino UNO (controlador de motores)

> Esta guía deja programmed el **Arduino UNO R3** que gobierna los dos motores a
> través del L298N. Incluye la instalación del driver USB, la configuración de
> la placa, la comprobación de la polaridad de los motores y la prueba segura
> con las ruedas en el aire.

---

## 1. Qué necesitas

| Elemento | Detalle |
|---|---|
| **Arduino UNO R3** | 1, con su microcontrolador ATmega328P |
| **Cable USB de datos** (USB-A a USB-B) | Un cable solo de carga no transfiere datos |
| **Arduino IDE 2.x** | La misma versión que usas para la ESP32 |
| Rover montado | Chasis, motores, L298N y cableado según `Docs/03-Tabla-de-Conexiones-y-Cableado.md` |

---

## 2. Instalar el soporte de la placa

1. Abre **Arduino IDE 2.x**.
2. Ve a **Herramientas → Placa**.
3. Si no está, en **Herramientas → Boards Manager** busca `uno` e instala el
   paquete oficial **"Arduino AVR Boards"** (ID `arduino:avr`). En las
   instalaciones habituales de IDE 2.x ya viene incluido.
4. Comprueba que en **Herramientas → Placa** aparece **Arduino UNO**.

---

## 3. Driver del USB-serie

El Arduino se conecta al ordenador por un chip USB-serie. Hay tres casos:

| Chip de tu placa | Qué hacer |
|---|---|
| **CH340 / CH341** (lo llevan casi todos los clones UNO R3) | Hay que instalar el driver. Si no lo tienes, el placa aparecerá en el Administrador de dispositivos como *Dispositivo desconocido* o con un triángulo amarillo, y **no habrá puerto COM** |
| **ATmega16U2** (Arduino UNO oficial) | No hace falta nada: Windows 10/11 lo reconoce de serie |
| **CP210x / CP2102** (alguna revisión del UNO y muchas placas ESP32) | Instala el driver de Silicon Labs |

### Cómo instalar el driver CH340 (Windows)

1. Descárgalo del **sitio oficial de WCH**: entra en `wch.cn` → sección
   *Downloads* → **CH341SER_EXE** (es el paquete *CH340/CH341 Windows driver*).
   Si el enlace directo ha cambiado, busca en Google `CH340 driver WCH` y baja
   siempre el del sitio del fabricante, no de páginas de descarga aleatorias.
2. Instálalo: abre el `.exe` descargado, acepta la ruta y pulsa *Install*.
   En Windows 10/11 puede aparecer el aviso de *Driver Signature
   Enforcement*: es normal, el driver está firmado por WCH.
3. **Desenchufa y vuelve a enchufar** el Arduino.
4. Abre el **Administrador de dispositivos** → **Puertos (COM y LPT)**: debe
   aparecer algo como *USB-SERIAL CH340 (COM3)*. Ese `(COM3)` es tu puerto.

---

## 4. Seleccionar placa y puerto

1. Con el Arduino conectado por USB:
   - **Herramientas → Placa → Arduino UNO**
   - **Herramientas → Puerto → COMx** (el que te salió en el Administrador de
     dispositivos).
   - **Herramientas → Procesador → ATmega328P** (déjalo como está).
   - **Herramientas → Programador → AVR ISP** (déjalo como está; el UNO no
     necesita programador externo).
2. **Si la ESP32 está conectada a la vez**, aparecerán **dos puertos COM**.
   Desenchufa el USB de la ESP32 mientras flasheas el Arduino (o memoriza qué
   `COMx` es cuál). Mistiar de placas es el error clásico.

> Con el rover montado **no hace falta desconectar nada más**: el enlace con la
> ESP32 va por **D10/D9** (`SoftwareSerial`), no por el USB del UNO. Por eso el
> Arduino se puede reprogramar con el chasis montado. Los pines D0/D1 (los del
> USB) están libres a propósito.

---

## 5. Abrir y revisar el sketch

1. **Archivo → Abrir…** → `firmware/arduino_uno/MotorController/MotorController.ino`.
2. Antes de subir, comprueba **las dos constantes de polaridad**:

   ```cpp
   const bool LEFT_MOTOR_INVERT  = false;  // false = positivo avanza
   const bool RIGHT_MOTOR_INVERT = true;   // true  = positivo avanza
   ```

   - `LEFT_MOTOR_INVERT = false` → el motor izquierdo avanza cuando la consigna
     es positiva.
   - `RIGHT_MOTOR_INVERT = true` → el motor derecho tiene el cableado invertido,
     así que el firmware compensa **en el propio código** y también avanza con
     consigna positiva.
3. Si al probar (paso 8) una rueda gira al revés, **cambia solo esa constante**
   (`false` ↔ `true`), vuelve a flashear y repite la prueba. No toques nada
   más: no hay que cambiar el cableado de los motores ni los pines.

---

## 6. Compilar y subir

1. **Herramientas → Verificar** (`Ctrl+--`): debe compilar sin errores.
2. **Herramientas → Subir** (`Ctrl+U`) o el botón **▶**.
3. El IDE compila y graba el programa. Tarda unos segundos y el LED `L` del
   UNO parpadea durante la grabación.
4. Cuando acaba, el LED `ON` queda fijo y el LED `TX/RX` parpadea al recibir
   datos del ESP32.

---

## 7. Verificación por el Monitor Serie

1. **Herramientas → Monitor Serie** (`Ctrl+Shift+M`), a **115200 baudios**
   (el firmware hace `Serial.begin(115200)`), final de línea *Both LF y CR*.
2. Pulsa **RESET** en el UNO.

Salida esperada:

```text
[ROVERT] Controlador de motores listo (motores detenidos)
```

Eso significa:

- El firmware arrancó.
- **Los motores están detenidos** (obligatorio: ningún firmware de este
  proyecto arranca con los motores en marcha).
- El pin D2 está en HIGH → el **LED frontal del rover está encendido** como
  faro.

Si en el Monitor Serie solo salen caracteres raros, el problema es la
velocidad: debe ser **115200**, no 38400. (Los **38400 baudios** son los del
enlace UART con la ESP32, no los del USB.)

---

## 8. Probar los motores (con las ruedas en el aire)

> **Regla de oro: pruebas de motor con el rover calzado, nunca en el suelo.**
> Un TT con reductora puede pasar de parado a ≈300 rpm en menos de un
> segundo: si el rover está en el suelo, se va a la pared o a la mesa.

### 8.1 Preparación

1. Pon el rover sobre un soporte (un libro, una caja) de forma que **las ruedas
   queden suspendidas** y puedan girar libremente.
2. Comprueba que nada bloquea las ruedas y que el LED frontal está encendido.
3. Ten a mano el botón **STOP** de la app (o el cable USB del Arduino listo para
   pulsar RESET, que también detiene los motores).

### 8.2 Prueba con la app (ruta normal)

1. Enciende la ESP32 y el Arduino.
2. Conecta el móvil a `Rovert-Cam` y abre la app (ver
   `Docs/02-Guia-de-Instalacion-APK.md`).
3. En el Monitor Serie del Arduino (o en el de la ESP32) verás tramar `R,…`.
4. Mantén pulsado **Adelante**:
   - **Los dos motores giran en el mismo sentido** (hacia delante del chasis).
   - Suelta: se paran.
5. Repite con **Retroceso** (deben ir al revés) y con los **giros** (un motor
   gira y el otro se para: giro sobre el sitio).
6. Si un motor gira al revés: vuelve al §5, cambia **una** constante y
   reflashea.

### 8.3 Prueba del enlace UART sin la app (opcional)

Si tienes un **adaptador USB-serie** (CP2102/CH340) puedes ver las tramas
crudas que van del ESP32 al Arduino:

1. **Desconecta de D10 y D9** los dos cables que van a la ESP32 (si no, dos
   salidas Pelean por el mismo pin).
2. Conecta el adaptador: **TX → D10**, **RX → D9**, **GND → GND**.
   (Lógica de 5 V: aquí no hace falta divisor, porque ambos lados son de 5 V.)
3. Abre el Monitor Serie del adaptador a **38400 baudios**.
4. Con la ESP32 conectada por Wi-Fi y la app en marcha, verás tramas así:

   ```text
   D,500,500,42
   D,500,500,43
   S,44
   P,44
   R,500,500,43
   ```

### 8.4 Qué significan las tramas

| Trama | Dirección | Significado |
|---|---|---|
| `D,<izq>,<der>,<seq>` | ESP32 → Arduino | Orden de movimiento. Los valores van en **milésimas**: `D,500,500,42` = ambos motores al 50 % hacia delante; `D,-1000,-1000,43` = al 100 % hacia atrás; `D,0,0,44` = parada |
| `S,<seq>` | ESP32 → Arduino | **STOP explícito**. El firmware manda esta trama al soltar el control, al perder la conexión y en cada watchdog |
| `P,<seq>` | ESP32 → Arduino | *Keep-alive*. Se envía **cada segundo** aunque no haya órdenes. **Renueva el watchdog pero no mueve los motores** |
| `R,<izq>,<der>,<seq>` | Arduino → ESP32 | **Telemetría**: el estado real de los motores tras aplicar la consigna. Se emite **cada 100 ms**. Ejemplos: `R,500,500,42` = ambos al 50 %; `R,0,0,7` = detenidos |
| `W,<seq>` | Arduino → ESP32 | **El watchdog del Arduino venció**: pasaron **400 ms** sin ninguna trama válida, así que los motores se detuvieron a la fuerza. En el USB verás además `[ROVERT] WATCHDOG: motores detenidos por falta de tramas` |

### 8.5 Prueba del watchdog (importante)

Con las ruedas en el aire:

1. Desde la app, mantén pulsado **Adelante** hasta que los motores giren.
2. **Desconecta el cable de datos** entre la ESP32 y el Arduino (o apaga la
   ESP32).
3. Los motores deben **pararse solos en menos de 400 ms** (el watchdog del
   Arduino) y en el UART debe aparecer la trama `W,<seq>`.

Este es el requisito de seguridad más crítico del proyecto: si se pierde el
enlace, el rover **no** puede seguir moviéndose.

---

## 9. Tabla de problemas

| Síntoma | Causa probable | Solución |
|---|---|---|
| **No aparece el puerto COM** | Falta el driver del USB-serie | Instala el driver CH340 (§3) o el que corresponda a tu placa; reinicia el Administrador de dispositivos |
| **`avrdude: stk500_recv() error`** o `programmer is not responding` | Cable de solo carga, o placa mal alimentada al flashear | Usa un cable de datos. Si la placa se alimenta por USB, eso debería bastar; si tienes algo raro en el robot, desconecta el resto |
| **El Monitor Serie sale con caracteres raros** | Baudios incorrectos | Pon **115200** (no 38400) y final de línea *Both LF y CR* |
| **El UNO se reinicia al flashear y el rover se mueve** | Había una orden activa | Nunca flashees con el rover en el suelo: con las ruedas en el aire y el rover calzado |
| **Un motor gira al revés** | Polaridad del motor | Cambia **una sola** constante (`LEFT_MOTOR_INVERT` / `RIGHT_MOTOR_INVERT`) y reflashea (§5) |
| **Los motores no giran pero el rover está bien** | Jumpers ENA/ENB del L298N puestos, o el L298N sin alimentación | Quita los jumpers ENA/ENB (para que llegue el PWM de D3/D6) y comprueba la alimentación de `+12V (Vs)` |
| **Los motores giran pero muy lento o solo a un ritmo fijo** | El PWM no llega | Revisa que D3 → ENA y D6 → ENB y que los jumpers ENA/ENB están retirados |
| **No llega nada por el UART** | Divisor mal puesto, GND no común o baudios distintos | Mide con el polímetro (ver `Docs/03-Tabla-de-Conexiones-y-Cableado.md` §6) y comprueba que los dos firmwares usan **38400** |

---

## Si algo falla

Si el UNO compila y sube pero el rover no se mueve, el problema casi nunca está
en el firmware: está en la alimentación, en el L298N o en el divisor de tensión.
Los diagnósticos paso a paso están en **`Docs/09-FAQ-y-Problemas.md`**, y el
esquema de cableado en `Docs/03-Tabla-de-Conexiones-y-Cableado.md`.
