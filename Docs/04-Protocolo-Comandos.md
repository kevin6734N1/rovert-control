# Protocolos de comunicación — Rovert

> **Estado:** convención cerrada (v1.0). Este documento es la **fuente de verdad** del contrato entre la
> app Android, el firmware ESP32-CAM y el firmware Arduino UNO. Cualquier cambio aquí obliga a
> actualizar los tres lados en el mismo commit.

---

## 1. Red Wi-Fi

| Parámetro | Valor | Nota |
|---|---|---|
| Modo | SoftAP (Access Point) generado por la ESP32-CAM | Sin router ni Internet |
| SSID | `Rovert-Cam` | Configurable en firmware (`config.h`) |
| Contraseña | `rovert2026` | WPA2-PSK, 11 caracteres |
| IP del AP | `192.168.4.1` | Estándar ESP32 SoftAP |
| Canal | 6 | Configurable |
| Gateway/DNS | `192.168.4.1` | |

La app **no** requiere conexión a Internet. AndroidUCE advierte "sin conexión a Internet"; hay que
pulsar "Mantener conectado". La app debe poder fijar la IP del rover desde ajustes
(valor por defecto `192.168.4.1`).

---

## 2. HTTP (ESP32-CAM → Android)

| Endpoint | Método | Respuesta | Uso |
|---|---|---|---|
| `/` | GET | JSON de diagnóstico | Comprobación rápida de que el rover responde |
| `/stream` | GET | `multipart/x-mixed-replace; boundary=frame` | Stream MJPEG de la OV2640 |
| `/status` | GET | JSON de estado | Estado de motores y uptime |
| `/control` | WS | WebSocket | Canal de control bidireccional |

### 2.1 Formato del stream MJPEG

Cada fotograma va precedido por una cabecera de boundary estándar (la que emite `ESPAsyncWebServer`):

```http
--frame\r\n
Content-Type: image/jpeg\r\n
Content-Length: 12345\r\n
\r\n
<bytes JPEG>\r\n
```

El cliente debe buscar la secuencia `\r\n\r\n` para localizar el inicio de los bytes JPEG y usar
`Content-Length` (o el boundary siguiente) para calcular el final del fotograma. **El firmware debe
emitir siempre `Content-Length`** para que el parser en Android sea determinista.

### 2.2 Objetivo de vídeo

- Resolución por defecto: **640x480 (VGA)**. Configurable a `FRAMESIZE_SVGA` (800x600) o
  `FRAMESIZE_HVGA` (400x296) según el campo de visión real del chasis.
- JPEG quality: 12 (calidad alta) con fb_count=2 cuando la memoria lo permita.
- Objetivo: 10–15 FPS.

---

## 3. WebSocket `/control`

### 3.1 App → ESP32 (mensajes JSON, texto UTF-8)

```json
{"type":"drive","left":0.0,"right":0.0,"seq":42}
{"type":"stop","seq":43}
{"type":"ping","seq":44}
```

| Campo | Tipo | Rango | Significado |
|---|---|---|---|
| `type` | string | — | `drive`, `stop`, `ping` |
| `left` | float | `-1.0 … 1.0` | Consigna del motor izquierdo (signo + = avance) |
| `right` | float | `-1.0 … 1.0` | Consigna del motor derecho (signo + = avance) |
| `seq` | int | 0…2³¹-1, incremental | Diagnóstico y detección de tramas desordenadas |

Reglas:
- `type:"stop"` **no** lleva `left`/`right`; ambos motores a 0.
- La app envía `drive` **periódicamente (10 Hz)** mientras una orden esté activa.
- La app envía `stop` **de inmediato** al soltar el control, perder el foco, cerrar la app,
  desconectar el mando o perder la conexión.
- El ESP32 **ignora** cualquier `left`/`right` fuera de `-1.0…1.0` (los recorta) y rechaza
  mensajes que no se puedan parsear.

### 3.2 ESP32 → App (mensajes JSON)

```json
{"type":"status","state":"stopped","left":0.0,"right":0.0,"seq":42,"uptimeMs":123456}
{"type":"pong","seq":44}
```

| `state` | Significado |
|---|---|
| `stopped` | Ambos motores a cero |
| `driving` | Hay una consigna de movimiento activa |

Si el ESP32 detecta la caída del WebSocket, envía `stop` al Arduino y actualiza `state` a `stopped`.

### 3.3 Movimiento diferencial (conversión a consignas por motor)

| Acción | `left` | `right` |
|---|---|---|
| Adelante | `+1.0` | `+1.0` |
| Retroceso | `-1.0` | `-1.0` |
| Giro izquierda (sobre el sitio) | `-1.0` | `+1.0` |
| Giro derecha (sobre el sitio) | `+1.0` | `-1.0` |
| STOP | `0.0` | `0.0` |

La app aplica además, **en el cliente**, dos correcciones configurables que el firmware no necesita
conocer:

- **Polaridad por motor** (`leftSign`, `rightSign` ∈ {+1, −1}): por si el cableado real invierte un motor.
- **Límite de velocidad** (`maxSpeed` ∈ [0.1, 1.0]): escala todas las consignas.

Esto evita reprogramar el firmware cada vez que se invierte un motor en el taller.

---

## 4. UART ESP32 ↔ Arduino UNO

- Velocidad: **115200 baud**, 8 bits de datos, 1 bit de parada, sin paridad (8N1).
- TX de la ESP32 → RX del Arduino (nivel 3,3 V, directo).
- TX del Arduino → RX de la ESP32 (**5 V → necesita adaptación de nivel**).
- GND común obligatorio.
- Toda trama termina en `\n` (LF). CR también se acepta y se descarta.

### 4.1 ESP32 → Arduino

```text
D,<left_milli>,<right_milli>,<seq>\n
S,<seq>\n
```

| Trama | Campos | Ejemplo |
|---|---|---|
| `D` (drive) | `left_milli`, `right_milli` enteros en milésimas (−1000…1000), `seq` | `D,500,500,42\n` |
| `S` (stop) | `seq` | `S,43\n` |

Se usan enteros en milésimas para que el parser en el UNO sea trivial y no dependa de `float`.

### 4.2 Arduino → ESP32 (telemetría, opcional)

```text
R,<left_milli>,<right_milli>,<seq>\n
W,<seq>\n
```

| Trama | Significado |
|---|---|
| `R` | Estado real de los motores tras aplicar la consigna |
| `W` | El watchdog venció: los motores se detuvieron por falta de tramas válidas |

### 4.3 Reglas de robustez del lado Arduino

1. Una trama mal formada **nunca** activa movimiento.
2. Los valores se recortan al rango ±1000.
3. Watchdog: si pasan **400 ms** sin una trama válida, ambos motores van a 0 y se registra `W`.
4. Tras un timeout, hace falta una orden válida nueva para volver a mover.
5. Al arrancar o reiniciar, los motores quedan **detenidos**.
6. El buffer de recepción no debe bloquear: tramas de longitud máxima 32 bytes.

---

## 5. Reglas de seguridad (normativas)

1. **Hold-to-move**: una orden de movimiento solo existe mientras el usuario mantiene el control.
2. **STOP tiene prioridad** sobre cualquier orden de movimiento.
3. La app manda `stop` al: soltar botón, gesto cancelado, stick en zona neutra, mando desconectado,
   WebSocket cerrado/error, app en segundo plano, pantalla apagada, error de stream.
4. La app **nunca** deja un comando activo si no ha podido enviarlo: en ese caso se considera STOP.
5. El watchdog del Arduino es la **última línea de defensa**, no la única.
6. Ningún firmware arranca con los motores en marcha.
