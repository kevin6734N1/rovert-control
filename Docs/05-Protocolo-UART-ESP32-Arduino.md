# Protocolo UART entre la ESP32-CAM y el Arduino UNO

> Documento complementario a `04-Protocolo-Comandos.md`. Aquí se detalla el
> enlace físico, el formato exacto de las tramas y el comportamiento del
> watchdog.

---

## 1. Parámetros del enlace

| Parámetro | Valor |
|---|---|
| Puerto en la ESP32 | UART2 (`Serial2`) |
| Puerto en el Arduino | `SoftwareSerial` (D10 = RX, D9 = TX) |
| Velocidad | **38400 baudios** |
| Formato | 8 bits de datos, 1 bit de parada, sin paridad (8N1) |
| Bits por segundo | 38400 / 10 bits = **3840 caracteres por segundo** |
| Fin de trama | `\n` (LF). Se acepta y se descarta un `\r` previo |

### 1.1 Por qué 38400 y no 115200

El Arduino UNO no tiene UART de hardware libre: `D0`/`D1` están conectados al
USB, así que usarlos para hablar con la ESP32 impediría reprogramar el rover
sin desenchufar nada. La alternativa es `SoftwareSerial`, que va por software
y a 115200 baudios es poco fiable en un ATmega328P de 16 MHz.

A **38400 baudios** la recepción por software es estable y el tráfico es
mínimo: la ESP32 manda unas 10 tramas por segundo de 10 bytes cada una
(100 bytes/s ≈ 2,6 % del ancho de banda). No hay problema de saturación.

Si en el banco de pruebas la lectura falla por el ruido de los motores, la
alternativa es pasar al UART de hardware (`D0`/`D1`) y desconectar el cable
USB durante la demostración.

---

## 2. Tramas ESP32 → Arduino

### 2.1 `D` — orden de movimiento

```text
D,<left_milli>,<right_milli>,<seq>\n
```

| Posición | Contenido | Rango | Ejemplo |
|---|---|---|---|
| 0 | Tipo de trama, siempre `D` | — | `D` |
| 1 | `left_milli` | −1000 … 1000 | `-500` |
| 2 | `right_milli` | −1000 … 1000 | `500` |
| 3 | `seq` | 0 … 2³¹−1 | `42` |

```text
D,500,500,42\n      ->  ambos motores al 50 % hacia delante
D,-1000,-1000,43\n   ->  ambos motores al máximo hacia atrás
D,0,0,44\n          ->  orden equivalente a STOP
```

### 2.2 `S` — parada inmediata

```text
S,<seq>\n
```

```text
S,45\n
```

### 2.3 `P` — keep-alive

```text
P,<seq>\n
```

La ESP32 envía un `P` cada segundo aunque no haya órdenes. Sirve para que el
watchdog del Arduino sepa que el enlace sigue vivo **sin** mover los motores.
El Arduino acepta `P` como trama válida (renueva el watchdog) pero **no**
cambia la posición de los motores.

### 2.4 Por qué enteros en milésimas

- El parser del Arduino es trivial: busca comas y llama a `atoi`.
- Un `float` de 32 bits en la trama exigiría `dtostrf` y una coma decimal
  dependiente de la configuración regional.
- 1000 valores posibles de velocidad son más que suficientes.

---

## 3. Tramas Arduino → ESP32 (telemetría)

### 3.1 `R` — estado real de los motores

```text
R,<left_milli>,<right_milli>,<seq>\n
```

Se emite cada 100 ms. La ESP32 la usa para conocer el estado real (por
ejemplo, si el watchdog del Arduino detuvo los motores).

### 3.2 `W` — el watchdog venció

```text
W,<seq>\n
```

Cuando la ESP32 recibe un `W` ejecuta un STOP inmediato y lo registra en el
monitor serie.

---

## 4. Reglas del parser del Arduino

1. Se acumulan caracteres en un búfer de 48 bytes hasta encontrar `\n`.
2. Una línea más larga que el búfer se **descarta entera**: nunca mueve los
   motores.
3. Si falta un campo, no es numérico o está fuera de rango, la trama se
   **ignora** y **no** renueva el watchdog.
4. Los valores se recortan al rango ±1000.
5. Sólo los tramas `D`, `S` y `P` renovan el watchdog.
6. Una trama `D` con `left=0` y `right=0` es un STOP, pero la trama `S` es la
   forma explícita y preferida de parar.

---

## 5. Watchdog (última línea de defensa)

```
  ESP32                                        Arduino UNO
    |                                                |
    |  D,500,500,42  ---- cada 100 ms ---->          |
    |                                                |  motors al 50 %
    |                                                |
    |  (se corta el cable / se apaga la ESP32)       |
    |                                                |
    |                                                |  400 ms sin trama
    |                                                |  --> STOP + trama W
```

- **Arduino**: 400 ms sin trama válida → ambos motores a 0 y emite `W`.
- **ESP32**: 500 ms sin orden de la app → envía `S` al Arduino.
- **App**: al soltar el control, al perder el socket o al ir a segundo plano,
  envía `stop` inmediatamente.

Son tres barreras independientes. Ninguna depende de que las otras funcionen.

---

## 6. Diagrama de tiempos

```text
App                ESP32                   Arduino            Motores
 |                   |                        |                 |
 |-- drive(1,1) --> | -- D,1000,1000,1 -----> | -- PWM 100% ---> | giran
 |                   |                        |                 |
 |-- drive(1,1) --> | -- D,1000,1000,2 -----> | -- PWM 100% ---> | giran
 |                   |                        |                 |
 |   (suelta)        |                        |                 |
 |-- stop --------> | -- S,3 ---------------> | -- PWM 0% -----> | paran
 |                   |                        |                 |
```

Cada línea `-->` es una trama completa terminada en `\n`.
