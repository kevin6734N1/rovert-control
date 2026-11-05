# Tabla de conexiones y cableado del rover

> Este documento es la **biblia del cableado** del rover: qué pin va a qué, con
> qué cable, con qué alimentación y en qué orden montarlo. Todo lo que sigue
> está deducido del firmware real del proyecto
> (`firmware/esp32cam/RovertCam/config.h`,
> `firmware/esp32cam/RovertCam/RovertCam.ino` y
> `firmware/arduino_uno/MotorController/MotorController.ino`), no de suposiciones.
> Si algo del cableado no coincide con la tabla, **manda la tabla**.

---

## 1. Visión general

El rover tiene tres bloques y una masa común:

```
   ┌────────────────┐   UART 38400    ┌───────────────┐   PWM + dirección   ┌───────┐
   │ ESP32-WROVER-  │  8N1, 2 cables  │  ARDUINO UNO  │  6 cables + 2 GND   │ L298N │
   │ CAM (ESP32 +   │────────────────▶│      R3       │─────────────────────▶│       │
   │ OV2640 + PSRAM)│   + masa común  │  (lógica)     │                      └───┬───┘
   └────────┬───────┘                 └───────┬───────┘                          │
            │                                 │                             OUT1..OUT4
            │ Wi-Fi SoftAP                    │                                  │
            │ "Rovert-Cam"                    │                                 ▼
            ▼                                 │                        ┌──────────────────┐
   ┌────────────────┐                         │                        │ 2× motores TT    │
   │ Teléfono       │◀──── HTTP/WS ───────────┘ (el Arduino no         └──────────────────┘
   │ (app + mando)  │           (no va por cable)                        ▲
   └────────────────┘                                                 ▲
                                                   2× motores con reductora
```

Tres ideas que condicionan todo el cableado:

1. **El Arduino UNO es el que mueve los motores.** La ESP32 solo manda consignas
   por el puerto serie.
2. **Los motores nunca se alimentan desde la ESP32 ni desde el pin 5 V del
   Arduino.** La corriente del motor se lleva el pack de 18650 al L298N.
3. **Las masas (GND) de los tres bloques están unidas.** Con dos pilas
   distintas, esto no es opcional: sin masa común el UART no funciona y
   parece que "los datos llegan corruptos".

---

## 2. Tabla 1 — ESP32-WROVER-CAM: pines que usamos

| Módulo / pin de la ESP32-CAM | Conecta a | Función | Notas |
|---|---|---|---|
| **GPIO32** (TX de UART2) | **D10** del Arduino UNO (RX) | Envío de tramas `D,…`, `S,…`, `P,…` | Directo. 3,3 V es nivel HIGH para el UNO. No necesita adaptación |
| **GPIO33** (RX de UART2) | **D9** del Arduino UNO (TX) **a través del divisor** | Recepción de telemetría `R,…`, `W,…` | El Arduino_tx es de 5 V: **obligatorio el divisor de tensión** (sección 6) |
| **GND** | **GND** del Arduino UNO y **GND** del L298N | Referencia eléctrica común | Masa común obligatoria |
| **5V** (pin de alimentación) | Salida **5 V** del convertidor buck/boost | Alimentación de la ESP32 y de la cámara OV2640 | fuente estable de **≥ 1 A pico**. Alimentar desde el pack de 4× AA |
| **3V3** | — | — | **No lo uses** como fuente en este proyecto; no hay nada que alimentar ahí |
| **Botón BOOT** (GPIO0) | — | Forzar el modo bootloader | Botón de la placa, no se cablea |
| **Botón RESET** (EN) | — | Reinicio | Botón de la placa, no se cablea |

### 2.1 Pines de la ESP32 que ya están ocupados por la cámara

La OV2640 va **soldada** en la placa. No hay que cablear nada de esto, pero
tampoco hay que "ayudar" con un cable a otro sitio: esta tabla explica
precisamente por qué el UART usa GPIO32/33 y no otros pines.

| Señal de la cámara | GPIO | Señal de la cámara | GPIO |
|---|---|---|---|
| XCLK | 21 | Y5 | 19 |
| SIOD (SDA) | 26 | Y4 | 18 |
| SIOC (SCL) | 27 | Y3 | 5 |
| VSYNC | 25 | Y2 | 4 |
| HREF | 23 | Y9 | 35 |
| PCLK | 22 | Y8 | 34 |
| PWDN | −1 (no usado) | Y7 | 39 |
| RESET | −1 (no usado) | Y6 | 36 |

> **Atención:** esta es la tabla de la placa **Freenove ESP32-WROVER-CAM**. Es
> **distinta** de la de la popular AI-Thinker ESP32-CAM (XCLK=0, Y2=5, Y3=18…).
> Si copias los pines de un tutorial de AI-Thinker, la cámara no funcionará y
> el ESP32 se reiniciará en bucle. Los valores correctos están en
> `config.h` (`CAM_PIN_*`).

---

## 3. Tabla 2 — Arduino UNO ↔ L298N

| Pin del Arduino UNO | Conecta a (L298N) | Función | Notas |
|---|---|---|---|
| **D3** | **ENA** | PWM (velocidad) del motor izquierdo | `analogWrite()` → es salida PWM |
| **D6** | **ENB** | PWM (velocidad) del motor derecho | `analogWrite()` → es salida PWM |
| **D8** | **IN1** | Dirección del motor izquierdo | Junto con IN2 |
| **D7** | **IN2** | Dirección del motor izquierdo | Junto con IN1 |
| **D12** | **IN3** | Dirección del motor derecho | Junto con IN4 |
| **D11** | **IN4** | Dirección del motor derecho | Junto con IN3 |
| **D2** | LED frontal (**220 Ω** en serie a GND) | Faro: fijo si está parado, parpadea si se mueve | `digitalWrite` |
| **GND** | GND del L298N | Masa común | |

### 3.1 L298N: salidas y alimentación

| Terminal del L298N | Conecta a | Notas |
|---|---|---|
| **OUT1 / OUT2** | Motor izquierdo (2 cables) | Polaridad invertida si el motor gira al revés |
| **OUT3 / OUT4** | Motor derecho (2 cables) | Idem |
| **+12V / Vs / 12V** | **+** del pack de 18650 | Alimentación de los motores |
| **GND** | GND del Arduino + GND de la ESP32 | Masa común |
| **5V** | (ver nota) | Solo una fuente de 5 V para la lógica del L298N |

> **Jumpers del L298N (importante).**
> - **ENA / ENB:** retira los jumpers. Si están puestos, el L298N ignora el PWM
>   de D3/D6 y los motores solo funcionan a velocidad fija.
> - **5V-enable:** quita el jumper de 5 V **y** alimenta la lógica del L298N
>   desde el **pin 5 V del Arduino**. Alternativa válida: dejar el jumper puesto
>   y **no** conectar el 5 V del Arduino, dejando que el regulador de la placa
>   alimente la lógica. Lo importante es **no hacer las dos cosas**: si el
>   jumper está puesto y además le metes 5 V del Arduino, tienes dos fuentes de
>   5 V enfrentadas.

### 3.2 Tabla 3 — Arduino ↔ ESP32 (enlace UART)

| Pin del Arduino UNO | Conecta a (ESP32) | Dirección | Nivel | Notas |
|---|---|---|---|---|
| **D10** (RX de `SoftwareSerial`) | **GPIO32** (TX) | ESP32 → Arduino | 3,3 V | Directo |
| **D9** (TX de `SoftwareSerial`) | **GPIO33** (RX) **vía divisor** | Arduino → ESP32 | 5 V → 3,3 V | 1 kΩ en serie + 2 kΩ a GND |
| **GND** | **GND** | — | — | Masa común |

Velocidad del enlace: **38400 baudios, 8N1** (`UART_BAUD` en el sketch del UNO y
`LINK_BAUD` en `config.h`). El divisor resistivo **no** cambia la velocidad.

> D9/D10 se usan a propósito (y no D0/D1) para que **el USB del Arduino pueda
> quedarse conectado**: así puedes reprogramar el UNO con el rover montado. Ver
> `Docs/05-Protocolo-UART-ESP32-Arduino.md`.

---

## 4. Tabla 4 — Alimentación (baterías)

| Origen | Tensión | Va a | pines/terminales | Notas |
|---|---|---|---|---|
| **4× AA** (portapilas de 4) | 6,0 V nominal (4×1,5 V) | Convertidor **buck/boost** | entrada `+`/`−` | 4 pilas AA dan entre 4 V y 6,4 V: hace falta regular a 5 V |
| Convertidor buck/boost | 5 Vregulated | **ESP32-WROVER-CAM** | pin **5V** y **GND** | Corriente **≥ 1 A pico**. Un brownout reinicia la ESP32 en bucle |
| **2× 18650** en serie (portapilas) | 7,4 V nominal (8,4 V recién cargada) | **Arduino UNO R3** | jack barrel (7–12 V) o entrada de 5 V | 1 sola 18650 (3,7 V) **no** alcanza para el regulador del UNO |
| **2× 18650** en serie | 7,4 V nominal | **L298N** | terminal **+12V (Vs)** y **GND** | Aquí es donde viven los motores |
| **5 V del Arduino** | 5 V | **L298N** (lógica) | pin **5V** y **GND** | Solo si has quitado el jumper de 5 V del L298N |

### 4.1 Esquema de alimentación

```
   4× AA (6,0 V)                      ESP32-WROVER-CAM
   ┌──────────────┐   +   ┌──────────────────┐   5 V   ┌──────────────┐
   │ portapilas   ├────────▶│ convertidor      ├───────▶│ pin 5V       │
   │ 4 pilas      │        │ buck/boost      │        │ (cámara y    │
   └──────────────┘   −   └────────┬─────────┘        │  ESP32)      │
                                   │ GND               └──────────────┘
                                   └────────────────────▶ GND

   2× 18650 (7,4 V)                  ARDUINO UNO R3
   ┌──────────────┐   +   ┌──────────────────┐  7-12 V ┌──────────────┐
   │ portapilas   ├───────▶│ jack barrel      ├────────▶│ VIN          │
   │ 18650        │        │ (o entrada 5 V)  │        │ (regulador)  │
   └──────────────┘   −   └────────┬─────────┘        └──────────────┘
                                   │ GND
                                   └────────────────────▶ GND

   2× 18650 (7,4 V)                  L298N
   ┌──────────────┐   +   ┌──────────────────┐        ┌──────────────┐
   │ portapilas   ├───────▶│ terminal +12V/Vs ├───────▶│ motores      │
   │ 18650        │        │                  │        │ (OUT1..OUT4) │
   └──────────────┘   −   └────────┬─────────┘        └──────────────┘
                                   │ GND
                                   └────────────────────▶ GND
```

> Todos los GND (ESP32, Arduino, L298N y los dos convertidores) van unidos a un
> mismo nodo. Es el punto de referencia de todo el sistema.

---

## 5. Diagrama del enlace UART con el divisor de tensión

```
   ESP32-WROVER-CAM                        ARDUINO UNO R3
  ┌───────────────────┐                 ┌───────────────────┐
  │                   │                 │                   │
  │ GPIO32 (TX2) 3,3V├─────────────────┤ D10 (RX)           │
  │                   │    directo      │  SoftwareSerial    │
  │                   │                 │                   │
  │ GPIO33 (RX2) 3,3V│◀──┬──[ 1 kΩ ]───┤ D9  (TX) 5,0V      │
  │                   │   │             │  SoftwareSerial    │
  │                   │  [ 2 kΩ ]       │                   │
  │                   │   │             │                   │
  │                   │   │             │                   │
  │ GND ──────────────┼───┴─────────────┤ GND               │
  │                   │   MASA COMÚN    │                   │
  └───────────────────┘                 └───────────────────┘
```

- **ESP32 TX → Arduino RX**: directo. 3,3 V ya es un "1" lógico para el UNO.
- **Arduino TX → ESP32 RX**: pasa por el divisor. El nodo de unión (entre la
  resistencia de 1 kΩ y el pin GPIO33) queda a 3,3 V.
- En la práctica: **1 kΩ entre D9 y GPIO33**, y **2 kΩ entre GPIO33 y GND**.

### 5.1 Diagrama del L298N

```
   ARDUINO UNO R3                    L298N                      MOTORES
  ┌────────────────┐            ┌─────────────────┐        ┌────────────────┐
  │                │            │                 │        │                │
  │ D3  (PWM) ─────┼───────────▶│ ENA         OUT1├───────▶│                │
  │ D6  (PWM) ─────┼───────────▶│ ENB         OUT2├───────▶│ MOTOR IZQUIERDO│
  │                │            │                 │        │                │
  │ D8 ────────────┼───────────▶│ IN1             │        └────────────────┘
  │ D7 ────────────┼───────────▶│ IN2             │
  │ D12 ───────────┼───────────▶│ IN3             │        ┌────────────────┐
  │ D11 ───────────┼───────────▶│ IN4         OUT3├───────▶│                │
  │                │            │             OUT4├───────▶│ MOTOR DERECHO  │
  │ GND ───────────┼───────────▶│ GND             │        │                │
  │                │            │                 │        └────────────────┘
  │ 5V ────────────┼─────┐      │ +12V (Vs) ← 7,4 V de las 18650
  └────────────────┘     │      │ 5V   ← del Arduino (si quitas el jumper)
                         └──────┤
```

Direcciones: IN1/IN2 controlan el motor de OUT1/OUT2 (izquierdo) e IN3/IN4 el
de OUT3/OUT4 (derecho). En los cuatro casos, "IN de la derecha en HIGH y la de
la izquierda en LOW" = un sentido de giro; al revés, el otro.

---

## 6. Por qué el divisor resistivo (y cómo calcularlo)

La ESP32 trabaja a **3,3 V** y sus pines **no son tolerantes a 5 V**: si le
metes 5 V en GPIO33, lo estás dañando de forma permanente. El Arduino UNO
trabaja a **5 V**, así que su TX entrega 5 V.

Un divisor resistivo baja ese nivel sin consumir casi corriente:

```text
   D9 (5 V) ────[ 1 kΩ ]────┬──── GPIO33 (ESP32)
                             │
                          [ 2 kΩ ]
                             │
                            GND
```

Cálculo (divisor de tensión):

```text
   V_salida = V_entrada × R_puesta_a_masa / (R_serie + R_puesta_a_masa)
             = 5 V × 2000 Ω / (1000 Ω + 2000 Ω)
             = 5 V × 0,666
             = 3,33 V        ← nivel perfecto para la ESP32
```

- Con **1 kΩ** en serie y **2 kΩ** a masa: 5 V → **3,33 V**.
- Si en tu placa el Arduino trabaja a 3,3 V (no debería en un UNO R3), el
  divisor no hace falta, pero **déjalo**: es inofensivo y protege.
- Corriente que circula: 5 V / 3 kΩ ≈ 1,7 mA. No le quita nada al Arduino.

---

## 7. Lista de materiales

| # | Material | Cantidad | Notas |
|---|---|---|---|
| 1 | Placa **Freenove ESP32-WROVER-CAM** | 1 | ESP32-WROVER + OV2640 + 4 MB PSRAM |
| 2 | Placa **Arduino UNO R3** | 1 | Con su chip USB-serie |
| 3 | Módulo **L298N** | 1 | Doble puente H; con regulador de 5 V y jumpers ENA/ENB/5V |
| 4 | **Motor TT con reductora** | 2 | 6 V nominal, el par depende de la reductora |
| 5 | Chasis de rover | 1 | 2 ruedas motrices + rueda libre o apoyo |
| 6 | Ruedas | 2 | Una por motor |
| 7 | Portapilas **18650** (2 huecos) | 1 | Para el Arduino **y** el L298N (2 packs si quieres separarlos) |
| 8 | Batería **18650** | 2 | Opcionalmente 4 si usas dos portapilas |
| 9 | Portapilas **4× AA** | 1 | Solo para la ESP32 |
| 10 | Pila **AA** | 4 | alkaline o Ni-MH |
| 11 | Convertidor **buck/boost** a 5 V | 1 | ≥ 1 A pico; con tornillos y cable |
| 12 | Cable USB **de datos** micro-USB | 1 | Para programar la ESP32 (**no** sirve un cable solo de carga) |
| 13 | Cable USB **de datos** USB-B | 1 | Para programar el Arduino UNO |
| 14 | Resistencias **1 kΩ** | 2 | Divisor de tensión (1 kΩ + 2 kΩ) |
| 15 | Resistencias **2 kΩ** | 2 | Divisor de tensión |
| 16 | Resistencias **220 Ω** | 2 | LED frontal (1) y spare |
| 17 | LED 3 mm | 1 | Faro frontal (cualquier color) |
| 18 | Cables **jumper** M-M | ~20 | Macho-macho de 20 cm |
| 19 | Cables **jumper** M-H | ~6 | Macho-hembra para los pines de la ESP32 |
| 20 | Bridas, tornillos y separadores M3 | 1 juego | Para fijar ESP32, Arduino y L298N al chasis |
| 21 | Pelacables o alicates | 1 | Para pelar los cables de los motores |

> Compra o reserva todo **antes** de la sesión de montaje: si falta un jumper o
> un portapilas, la sesión se va. Ver `Docs/10-Plan-de-Pruebas-Fabian-Rodrigo.md`.

---

## 8. Pines prohibidos en la ESP32-WROVER-CAM

| Pines | Motivo | Qué pasa si los tocas |
|---|---|---|
| **GPIO16, GPIO17** | Los usa la **PSRAM interna** del módulo ESP32-WROVER | La cámara no puede usar memoria para los fotogramas: el stream MJPEG no funciona y el ESP32 se reinicia en bucle |
| **GPIO6 … GPIO11** | Los usa la **flash interna** del módulo | El ESP32 ni siquiera arranca (o se cuelga al flashear) |
| **GPIO0, GPIO2, GPIO5, GPIO12, GPIO15** | **Pines de strapping**: el ESP32 los lee durante el arranque para decidir el modo de boot | Si los forces a un nivel incorrecto, la placa arranca en modo bootloader o no arranca. GPIO0 es además el botón BOOT y GPIO2 lleva el LED integrado en muchas placas |
| **GPIO34 … GPIO39** | **Solo entrada**, sin salida ni resistencias internas de pull-up | No pueden dar salida de nivel alto ni servir para PWM ni para el UART |
| **GPIO1, GPIO3** | UART0: es por donde va el USB de la placa | Confunden al flasheo y al monitor serie |

Por eso el UART del proyecto usa **GPIO32 (TX) y GPIO33 (RX)**: libres, sin
conflicto con la cámara, sin PSRAM, sin flash y sin strapping. Y la alternativa
de reserva que aparece en `config.h` es **GPIO13/GPIO14** (cambiando también
`CAM_PIN_Y2` / `CAM_PIN_Y3` y recalculando).

---

## 9. Puntos de verificación (checklist antes de encender)

> Haz esta lista **con el rover sobre la mesa, sin ruedas en el suelo y sin las
> pilas puestas**. Si algo falla, repítelo antes de seguir. Cada punto es
> binario: o lo cumples o no avanzas.
>
> Si un punto falla y no sabes por qué, `Docs/09-FAQ-y-Problemas.md` tiene el
> diagnóstico de los síntomas más comunes.

### 9.1 Comprobaciones de cableado

- [ ] **Masa común**: el GND de la ESP32, el del Arduino y el del L298N están
      unidos (compruébalo con el polímetro en modo continuidad).
- [ ] **Divisor correcto**: 1 kΩ entre **D9** y **GPIO33**; 2 kΩ entre
      **GPIO33** y **GND**.
- [ ] **TX directo**: GPIO32 → D10, sin resistencias de por medio.
- [ ] **Sin 5 V en la ESP32**: mide con el polímetro en GPIO33 y en GPIO32. Debes
      medir **3,3 V** como máximo. Si mides 5 V, para: hay un error de cableado.
- [ ] **Señal de 3,3 V con 5 V de fondo**: con el Arduino HIGH y el divisor bien
      puesto, GPIO33 debe marcar ≈ **3,3 V**, no 5 V.
- [ ] **Alimentación**: el pin **5V** de la ESP32 recibe 5 V del convertidor
      (mide 4,9–5,1 V con las pilas puestas).
- [ ] **No hay 5 V en el pin 3V3 de la ESP32** (salida del regulador de la placa;
      déjala libre).
- [ ] **Motores alimentados desde el L298N**, nunca desde la ESP32 ni desde el
      5 V del Arduino.
- [ ] **Jumpers ENA y ENB del L298N retirados** (si no, no hay PWM).
- [ ] **Una sola fuente de 5 V para la lógica del L298N**: jumper 5V retirado **y**
      5 V del Arduino, o jumper puesto **y** sin 5 V del Arduino.
- [ ] **Ninguna resistencia de pull-up o pull-down de más**: el UART funciona con
      las de serie.

### 9.2 Comprobaciones de módulos

- [ ] **Cámara**: el conector de la OV2640 está bien enchufado en la placa
      (en la Freenove viene soldada; no hay que hacer nada).
- [ ] **Motores**: los dos motores están en OUT1/OUT2 (izquierdo) y OUT3/OUT4
      (derecho), y las ruedas están **libres** (roden al girar a mano).
- [ ] **LED frontal**: D2 → resistencia 220 Ω → ánodo del LED → cátodo a GND.
- [ ] **Tornillería**: ESP32, Arduino y L298N bien fijados al chasis; **nada de
      cables pelados tocando el chasis metálico**.

### 9.3 Primera puesta en marcha (en este orden)

- [ ] Con **solo** la ESP32 alimentada: se crea la red `Rovert-Cam` y el monitor
      serie muestra `[ROVERT] Camara OV2640 inicializada OK`.
- [ ] Con **solo** el Arduino alimentado: el monitor serie (115200) muestra
      `[ROVERT] Controlador de motores listo (motores detenidos)`.
- [ ] Con **las tres** piezas: la ESP32 imprime `[ROVERT] UART2 listo: TX=32
      RX=33 a 38400 baudios` y el rover responde en `http://192.168.4.1/`.
- [ ] **Ruedas en el aire**: los dos motores giran en el mismo sentido al
      pulsar *Adelante*. Si uno va al revés, cambia **una sola** constante
      (`LEFT_MOTOR_INVERT` / `RIGHT_MOTOR_INVERT`) y vuelve a flashear. Ver
      `Docs/07-Instrucciones-Flash-Arduino.md`.
