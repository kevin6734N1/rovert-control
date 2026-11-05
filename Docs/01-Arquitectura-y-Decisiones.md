# Arquitectura del proyecto y decisiones de diseño

> Este documento explica **qué** se construyó, **por qué** se eligió cada
> cosa y qué alternativas se descartaron. Es la referencia para mantener el
> proyecto coherente cuando haya que cambiarlo.

---

## 1. Vista general

```
┌──────────────────────────┐
│  Teléfono Android        │
│  ┌────────────────────┐  │
│  │ App "Rovert"       │  │
│  │  - Vista Compose   │  │
│  │  - MjpegStreamView │  │
│  │  - RoverClient     │  │
│  │  - GamepadController│ │
│  └────────────────────┘  │
└───────┬──────────┬───────┘
        │ Wi-Fi    │ Bluetooth
        │ (SoftAP) │ (DualShock 4)
        ▼          │
┌──────────────────────────┐
│  ESP32-CAM (Freenove)    │
│  - SoftAP "Rovert-Cam"   │
│  - HTTP :80  /stream     │
│  - WS   :80  /control    │
│  - UART2 -> Arduino      │
└───────────┬──────────────┘
            │ UART 38400 8N1
            ▼
┌──────────────────────────┐
│  Arduino UNO R3          │
│  - L298N -> 2 motores TT │
│  - LED frontal (D2)      │
│  - watchdog 400 ms       │
└──────────────────────────┘
```

---

## 2. Decisiones de diseño

### 2.1 El ESP32-CAM crea su propia red Wi-Fi (SoftAP)

**Decisión.** La ESP32-CAM levanta un punto de acceso propio (`Rovert-Cam`,
192.168.4.1) en lugar de conectarse a un router del aula.

**Por qué.**
- En una Multifunción de la universidad el router exige portal cautivo y WPA2
  con credenciales institutionales: el rover quedaría sin salida a Internet y
  la app no podría ni conectarse.
- El SoftAP no necesita contraseña institucional ni router: el rover es
  autónomo y funciona en cualquier aula, campo o exposición.
- El coste es que el móvil se queda sin datos móviles mientras esté conectado
  (es un comportamiento normal y el propio Android avisa).

**Alternativa descartada.** Conectar la ESP32 al Wi-Fi del aula: dependería
de la red, de las credenciales y de que el router no aísle clientes.

### 2.2 HTTP para el vídeo, WebSocket para el control

**Decisión.** El vídeo MJPEG va por HTTP GET a `/stream`; las órdenes de
movimiento van por un WebSocket en `/control`.

**Por qué.**
- El vídeo es un flujo unidireccional y continuo: HTTP con
  `multipart/x-mixed-replace` es el formato estándar para MJPEG y lo
  entienden todos los clientes, incluidos los navegadores y `curl`.
- El control necesita full-duplex (órdenes hacia el rover y estado de vuelta)
  con mensajes pequeños y parseables: un WebSocket con JSON es lo más simple
  de depurar (se puede ver el texto en el monitor serie).
- Alternativa descartada: mandar el control por HTTP POST en cada fotograma
  generaría una petición por cada 10 Hz de orden: demasiado tráfico y
  latencia irregular.

**Detalle.** El WebSocket y el HTTP comparten el puerto 80 gracias a
`ESPAsyncWebServer`, así que la app solo necesita un host.

### 2.3 El ESP32 no mueve motores: solo traduce

**Decisión.** La ESP32-CAM recibe órdenes y las reenvía por UART al Arduino,
que es quien maneja el L298N y los motores.

**Por qué.**
- El ESP32 tiene todas las interrupciones de Wi-Fi, cámara y WebSocket: si
  además generase PWM, los pulsos se deformarían con el tráfico de red.
- El UNO tiene timers hardware capaces de generar PWM muy estable sin
  interrupciones que lo molesten.
- La separación de responsabilidades hace que cada firmware sea simple y
  testeable por separado.

### 2.4 Nivel 5 V → 3,3 V con divisor resistivo

**Decisión.** Divisor de 1 kΩ en serie + 2 kΩ a GND en la línea
Arduino TX → ESP32 RX.

**Por qué.** El ESP32 no es tolerante a 5 V. Un divisor de 2/3 convierte
exactamente 5 V en 3,3 V. La línea contraria (ESP32 TX → Arduino RX) no
necesita nada: 3,3 V es un HIGH válido para el ATmega328P (su Vih es 0,6·Vcc
≈ 3 V a 5 V).

**Alternativa descartada.** Módulo "level shifter" de 4 canales: un módulo
bidireccional con MOSFET está pensado para I2C y cuesta más caro que dos resistencias de
0,1 USD.

### 2.5 `SoftwareSerial` en lugar del UART de hardware del UNO

**Decisión.** El enlace UART del UNO usa `SoftwareSerial` en D10/D9 a
38400 baudios.

**Por qué.**
- `D0`/`D1` del UNO están conectados al USB. Usarlos para la ESP32 obliga a
  desenchufar el USB para reprogramar el rover y, peor, hace que los datos
  de la ESP32 interfieran con la programación.
- A 38400 baudios `SoftwareSerial` es fiable; a 115200 no lo es.
- Con D10/D9 el rover se puede reprogramar **montado y con la batería puesta**,
  lo que es mucho más cómodo en una práctica.

**Alternativa disponible.** Mover el enlace al UART de hardware (D0/D1) y
desconectar el USB durante la demo, si en el banco de pruebas hay problemas
de ruido eléctrico.

### 2.6 Polaridad y límite de velocidad en la app, no en el firmware

**Decisión.** La inversión de polaridad por motor (`leftSign`, `rightSign`) y
el límite de velocidad (`maxSpeed`) se aplican en el cliente.

**Por qué.** El signo de un motor depende de cómo quedó cableado el L298N en
el chasis, y eso cambia cada vez que se desmonta el rover. Con la corrección
en la app, arreglar un motor invertido es cambiar un interruptor en los
ajustes, sin volver a compilar ni flashear la ESP32.

**Consecuencia.** El firmware solo entiende valores normalizados: el
"contrato" de la app hacia el rover es siempre el mismo.

### 2.7 Tres barreras de seguridad independientes

**Decisión.** La parada se decide en tres sitios distintos.

| Barrera | Dónde | Tiempo |
|---|---|---|
| 1 | App: `stop` al soltar, perder foco, ir a segundo plano, caer el socket | inmediato |
| 2 | ESP32: `S` al recibir `stop` o al pasar 500 ms sin órdenes | ≤ 500 ms |
| 3 | Arduino: watchdog de trama UART | 400 ms |

**Por qué.** Un rover mal detenido puede romper una máquina, un escritorio
o lastimar a alguien. Ninguna de las tres depende de que las otras dos
funcionen: si el Wi-Fi se cae, el Arduino para solo; si el UNO se cuelga, la
ESP32 manda `S`; si la app se cuelga, el ESP32 y el UNO paran.

### 2.8 Posesión del rover: "hold to move"

**Decisión.** No hay botones de "adelante" que se queden pulsados. El rover se
mueve **mientras** se mantiene pulsado.

**Por qué.** Un rover que arranca solo al abrir la app, o que sigue
avanzando porque se soltó el dedo a medias, es un riesgo inaceptable en una
demo delante de gente. El modelo "hold to move" hace que el comportamiento
por defecto sea siempre "parado".

### 2.9 El mando se conecta al teléfono, no al rover

**Decisión.** El DualShock 4 se empareja por Bluetooth con el **teléfono**.
La app lee sus ejes con la API de gamepad de Android (`InputDevice`,
`MotionEvent`).

**Por qué.**
- El ESP32-WROVER-CAM no tiene Bluetooth integrado. Añadir un módulo BT
  HC-05/HC-06 sería una pieza más, otro emparejamiento y otro punto de
  fallo.
- Android ya sabe hablar con el DualShock 4: el sistema lo reconoce como
  gamepad y entrega los ejes como eventos de movimiento.
- La app **no necesita permiso de Bluetooth** para leer un mando ya
  emparejado: solo hace falta emparejarlo en Ajustes del sistema.

### 2.10 MJPEG y no H.264

**Decisión.** Se transmite JPEG fotograma a fotograma.

**Por qué.**
- La ESP32-CAM no tiene codificador de vídeo; el JPEG sale directo del
  sensor OV2640 sin esfuerzo de CPU.
- Un decodificador H.264 por software en un móvil consume batería y CPU, y
  la ESP32 no puede generarlo.
- A 640x480 y 10–15 FPS el MJPEG va sobrado por el Wi-Fi de la placa
  (el ESP32 es el que va justo, y aun así aguanta).
- El parser del cliente es sencillo: buscar `Content-Length` y decodificar
  con `BitmapFactory`.

### 2.11 Resolución VGA (640×480) por defecto

**Decisión.** `FRAMESIZE_VGA`, calidad JPEG 12, 2 búferes en PSRAM.

**Por qué.**
- VGA con calidad 12 da unos 40–60 kB por fotograma: a 12 FPS son menos de
  600 kB/s, muy por debajo del límite real de la ESP32 en una red corta.
- UXGA (1600×1200) necesita 2 búferes de frame buffer completos y, sin PSRAM
  bien configurada, provoca reinicios en bucle. La WROVER sí tiene PSRAM,
  pero no hace falta arriesgarse.
- La resolución se cambia en **una línea** de `config.h`.

### 2.12 La ESP32-CAM es la "WROVER", no la "AI-Thinker"

**Decisión.** Los pines de cámara de `config.h` son los de la placa Freenove
ESP32-WROVER-CAM.

**Por qué.** Existen dos placas muy parecidas con tablas de pines
**incompatibles**:

| Señal | Freenove ESP32-WROVER-CAM | AI-Thinker ESP32-CAM |
|---|---|---|
| XCLK | GPIO21 | GPIO0 |
| Y2 | GPIO4 | GPIO5 |
| Y3 | GPIO5 | GPIO18 |
| Y4 | GPIO18 | GPIO19 |
| Y5 | GPIO19 | GPIO21 |
| SIOD | GPIO26 | GPIO26 |
| VSYNC | GPIO25 | GPIO25 |
| PWDN | −1 (libre) | GPIO32 |

Copiar un ejemplo de la placa equivocada **no da error de compilación**: la
cámara simplemente no funciona (imagen verde o negro). Por eso los pines
están en `config.h` con un comentario que lo advierte.

### 2.13 GPIO16/17 no se pueden usar

**Decisión.** El UART usa **GPIO32 (TX) y GPIO33 (RX)**.

**Por qué.** En el módulo ESP32-WROVER la PSRAM interna ocupa GPIO16 y GPIO17,
y la flash interna usa GPIO6–GPIO11. La documentación oficial de Freenove lo
indica de forma explícita. En muchas guías de ESP32-WROVER se suggests
GPIO16/17 para el UART porque se confunde con el ESP32-WROVER-KIT o con el
AI-Thinker ESP32-CAM, donde la PSRAM es un chip externo en GPIO16/17.

Además GPIO32/33 no son pines de *strapping* (0, 2, 5, 12, 15), así que el
arranque de la placa es seguro.

### 2.14 Sin librerías de terceros en la app

**Decisión.** La app usa sólo `OkHttp` como dependencia externa de red.

**Por qué.**
- OkHttp trae WebSocket, cliente HTTP y manejo de *cleartext* en un paquete
  solo, bien mantenido y con soporte de `Multipart`.
- El parser del MJPEG se implementa a mano (~120 líneas) porque el
  `MultipartReader` de OkHttp no está pensado para un flujo infinito de
  imágenes y añadir una librería de vídeo completa sería.
- Menos dependencias = APK más pequeño y menos riesgo de que a los compañeros
  les falle la instalación.

### 2.15 Idioma de la interfaz: español

**Decisión.** Todos los textos visibles están en español.

**Por qué.** La app la usan compañeros del curso en una demostración oral; los
mensajes tienen que poder leerse en voz alta durante la defensa del proyecto.

---

## 3. Estructura del repositorio

```
Robert - ESP32 Cam/
├── README.md                  -> punto de entrada para cualquiera
├── Docs/                      -> documentación (esta carpeta)
│   ├── 00-INDICE.md
│   ├── 01-Arquitectura-y-Decisiones.md   (este archivo)
│   ├── 02-Guia-de-Instalacion-APK.md
│   ├── 03-Tabla-de-Conexiones-y-Cableado.md
│   ├── 04-Protocolo-Comandos.md           (contrato de la app)
│   ├── 05-Protocolo-UART-ESP32-Arduino.md
│   ├── 06-Instrucciones-Flash-ESP32.md
│   ├── 07-Instrucciones-Flash-Arduino.md
│   ├── 08-Pruebas-de-Aceptacion.md
│   ├── 09-FAQ-y-Problemas.md
│   ├── 10-Plan-de-Pruebas-Fabian-Rodrigo.md
│   └── 11-Registro-de-Cambios.md
├── android/                   -> proyecto Android (Kotlin + Compose)
├── firmware/
│   ├── esp32cam/
│   │   ├── RovertCam/         -> sketch principal (+ config.h)
│   │   └── libraries/         -> ESPAsyncWebServer y ESPAsyncTCP
│   └── arduino_uno/
│       └── MotorController/   -> sketch del UNO
├── dist/                      -> APK compilados
└── tools/                     -> SDK de Android, Gradle (no se versiona)
```

---

## 4. Capas de la app Android

```
ui/          Compose: RoverScreen, ControlPanel, StatusBar, SettingsDialog
input/       TouchDpad (táctil) y GamepadController (mando)
protocol/    ProtocolCodec (JSON) y MotorMapping (acción -> left/right)
net/         RoverClient (OkHttp: WebSocket + HTTP)
video/       MjpegStreamView (parser multipart + decodificación)
model/       DriveCommand, ConnectionState, RoverSettings
data/        SettingsStore (SharedPreferences)
```

Regla de dependencia: `ui` conoce a `input`, `input` conoce a `protocol`, y
`net` conoce a `protocol`. **`protocol` no depende de nada**, así que se
puede probar con tests unitarios sin emulador ni hardware.

Las tres capas que la especificación pide separar —interfaz de usuario,
entrada de mando y cliente de red— están en paquetes distintos y no se
importan entre sí salvo a través de `protocol`.

---

## 5. Lo que NO hace la app (y por qué)

| No está | Motivo |
|---|---|
| Grabar vídeo o fotos | No lo pedía el alcance; añadiría almacenamiento y permisos |
| Control de la luz LED | El LED lo maneja el Arduino (D2) como faro/parpadeo |
| Telemetría de batería | Requiere un divisor de tensión y un pin ADC extra; fuera del alcance actual |
| Streaming a Internet | El rover es un punto de acceso local; no se expone a la red pública |
| Mapa / telemetría de posición | Requiere odometría o GPS; fuera del alcance actual |
| Control por voz | Fuera del alcance del curso |

Lo que sí está preparado para crecer: `RoverSettings` ya guarda velocidad
máxima, polaridad por motor y zona muerta del stick, que son los parámetros
que se tocan en una puesta a punto.
