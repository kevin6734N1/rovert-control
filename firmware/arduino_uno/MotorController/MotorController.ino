/*
 * ============================================================================
 *  Rovert — Controlador de motores (Arduino UNO R3)
 * ============================================================================
 *  Proyecto : Robótica — Rovert Post Sismos
 *  Archivo  : MotorController.ino
 *  Versión  : 1.0.0
 *
 *  Función
 *  -------
 *  Recibe del ESP32-CAM consignas de movimiento por UART (SoftwareSerial) y
 *  controla los dos motores DC a través de un L298N.
 *
 *  Protocolo UART (ver Docs/04-Protocolo-Comandos.md)
 *  -------------------------------------------------
 *    ESP32 -> Arduino :  D,<left_milli>,<right_milli>,<seq>\n
 *                         S,<seq>\n
 *    Arduino -> ESP32 :  R,<left_milli>,<right_milli>,<seq>\n   (telemetría)
 *                         W,<seq>\n                             (watchdog venció)
 *
 *  left_milli / right_milli : enteros en milésimas, rango -1000..1000
 *                              (+1000 = avance a tope, -1000 = retroceso)
 *
 *  Seguridad
 *  ---------
 *  1. Al arrancar o reiniciar, los motores quedan DETENIDOS.
 *  2. Una trama inválida NUNCA activa movimiento.
 *  3. Watchdog: si pasan 400 ms sin trama válida, los motores se detienen.
 *  4. Tras un timeout hace falta una orden válida nueva para volver a mover.
 *
 *  Cableado (UNO)                        Cableado (L298N)
 *  --------------------------------       ---------------------------
 *   D3  -> ENA                              IN1  <- D8
 *   D6  -> ENB                              IN2  <- D7
 *   D8  -> IN1                              IN3  <- D12
 *   D7  -> IN2                              IN4  <- D11
 *   D12 -> IN3                              OUT1/OUT2 -> motor izquierdo
 *   D11 -> IN4                              OUT3/OUT4 -> motor derecho
 *   D10 <- RX  (desde ESP32 TX)            ENA  <- D3 (PWM)
 *   D9  -> TX  (hacia ESP32 RX)            ENB  <- D6 (PWM)
 *   D2  -> LED frontal (+ resistencia 220 Ω a GND)
 *   GND -> GND del ESP32 (GND COMÚN OBLIGATORIO)
 *
 *  NOTA SOBRE LOS PINES D10/D9:
 *  Se usa SoftwareSerial para no ocupar el UART hardware (D0/D1), que en el
 *  UNO está conectado al USB. Así se puede reprogramar el UNO con el rover
 *  montado sin desconectar nada. Si en el banco de pruebas la lectura falla
 *  por ruido de los motores, la alternativa es pasar el UART hardware a
 *  D0/D1 (ver Docs/05-Protocolo-UART-ESP32-Arduino.md).
 * ============================================================================
 */

#include <SoftwareSerial.h>

// ---------------------------------------------------------------------------
//  CONFIGURACIÓN DE PINES
// ---------------------------------------------------------------------------
const uint8_t PIN_ENA   = 3;    // PWM motor izquierdo
const uint8_t PIN_ENB   = 6;    // PWM motor derecho
const uint8_t PIN_IN1   = 8;    // L298N entrada 1 (motor izquierdo)
const uint8_t PIN_IN2   = 7;    // L298N entrada 2 (motor izquierdo)
const uint8_t PIN_IN3   = 12;   // L298N entrada 3 (motor derecho)
const uint8_t PIN_IN4   = 11;   // L298N entrada 4 (motor derecho)
const uint8_t PIN_LED   = 2;    // LED frontal del rover

const uint8_t PIN_LINK_RX = 10;  // <- TX del ESP32
const uint8_t PIN_LINK_TX = 9;   // -> RX del ESP32

// ---------------------------------------------------------------------------
//  CONFIGURACIÓN DE COMPORTAMIENTO
// ---------------------------------------------------------------------------
const uint16_t UART_BAUD      = 38400;
const uint16_t WATCHDOG_MS    = 400;    // sin trama válida -> STOP
const uint16_t TELEMETRY_MS   = 100;    // periodicidad de la telemetría R
const int16_t  MAX_MILLI      = 1000;   // tope absoluto de consigna

/*
 *  Polaridad de los motores.
 *  Si al probar con las ruedas levantadas un motor gira al revés, cambia
 *  aquí el `true` por `false` (o al revés) y vuelve a cargar el programa.
 *  NO es necesario tocar nada más.
 */
const bool LEFT_MOTOR_INVERT  = false;  // false = positivo avanza
const bool RIGHT_MOTOR_INVERT = true;   // true  = positivo avanza

// ---------------------------------------------------------------------------
//  ESTADO
// ---------------------------------------------------------------------------
SoftwareSerial Link(PIN_LINK_RX, PIN_LINK_TX);

int16_t lastLeft  = 0;        // consigna aplicada (milésimas)
int16_t lastRight = 0;
uint32_t lastValidMs = 0;     // instante de la última trama válida
uint32_t lastTelemetryMs = 0;
uint32_t lastSeq = 0;

bool watchdogFired = false;
bool driving = false;

char rxBuffer[48];
uint8_t rxLen = 0;

// ---------------------------------------------------------------------------
//  UTILIDADES
// ---------------------------------------------------------------------------

/** Convierte milésimas (-1000..1000) en PWM (0..255). */
uint8_t milliToPwm(int16_t milli) {
  int32_t v = milli;
  if (v > MAX_MILLI) v = MAX_MILLI;
  if (v < -MAX_MILLI) v = -MAX_MILLI;
  if (v < 0) v = -v;                       // el signo lo maneja la dirección
  return (uint8_t)((v * 255L) / MAX_MILLI);
}

/** Aplica una consigna (milésimas) a un motor. */
void setMotor(uint8_t inA, uint8_t inB, uint8_t enPin, int16_t milli, bool invert) {
  if (milli == 0) {
    digitalWrite(inA, LOW);
    digitalWrite(inB, LOW);
    digitalWrite(enPin, LOW);
    return;
  }

  bool forward = (milli > 0);
  if (invert) forward = !forward;

  if (forward) {
    digitalWrite(inA, HIGH);
    digitalWrite(inB, LOW);
  } else {
    digitalWrite(inA, LOW);
    digitalWrite(inB, HIGH);
  }

  analogWrite(enPin, milliToPwm(milli));
}

/** Detiene ambos motores de inmediato. */
void stopMotors() {
  setMotor(PIN_IN1, PIN_IN2, PIN_ENA, 0, LEFT_MOTOR_INVERT);
  setMotor(PIN_IN3, PIN_IN4, PIN_ENB, 0, RIGHT_MOTOR_INVERT);
  lastLeft = 0;
  lastRight = 0;
  driving = false;
}

/** Parsea un entero con signo dentro de un texto CSV. Devuelve false si no puede. */
bool parseInt(const char* s, const char* end, long& out) {
  while (s < end && (*s == ' ' || *s == '\t')) s++;
  bool neg = false;
  if (s < end && (*s == '-' || *s == '+')) {
    neg = (*s == '-');
    s++;
  }
  if (s >= end || *s < '0' || *s > '9') return false;
  long v = 0;
  while (s < end && *s >= '0' && *s <= '9') {
    v = v * 10 + (*s - '0');
    if (v > 1000000L) return false;        // número absurdo -> trama inválida
    s++;
  }
  out = neg ? -v : v;
  return true;
}

/** Recorta una consigna al rango permitido. */
int16_t clampMilli(long v) {
  if (v > MAX_MILLI) return MAX_MILLI;
  if (v < -MAX_MILLI) return -MAX_MILLI;
  return (int16_t)v;
}

// ---------------------------------------------------------------------------
//  PARSER DE TRAMAS
// ---------------------------------------------------------------------------
void handleLine(char* line, uint8_t len) {
  // Quitar posible '\r'
  if (len > 0 && line[len - 1] == '\r') line[--len] = '\0';
  if (len == 0) return;

  char type = line[0];

  if (type == 'D') {
    // D,<left>,<right>,<seq>
    const char* p = line + 1;
    const char* fields[3] = {0, 0, 0};
    int field = 0;
    const char* start = p;

    for (uint8_t i = 0; i <= len && field < 3; i++) {
      if (i == len || line[i] == ',') {
        fields[field++] = start;
        // Guardar el terminador temporalmente para parsear
        char saved = line[i];
        line[i] = '\0';
        start = line + i + 1;
        long a, b, c;
        if (!parseInt(fields[0], fields[0] + strlen(fields[0]), a) ||
            !parseInt(fields[1], fields[1] + strlen(fields[1]), b) ||
            !parseInt(fields[2], fields[2] + strlen(fields[2]), c)) {
          line[i] = saved;
          return;                         // trama inválida -> NO mover
        }
        line[i] = saved;
        if (field == 3) {
          lastSeq = (uint32_t)c;
          lastValidMs = millis();
          watchdogFired = false;
          lastLeft  = clampMilli(a);
          lastRight = clampMilli(b);
          setMotor(PIN_IN1, PIN_IN2, PIN_ENA, lastLeft, LEFT_MOTOR_INVERT);
          setMotor(PIN_IN3, PIN_IN4, PIN_ENB, lastRight, RIGHT_MOTOR_INVERT);
          driving = (lastLeft != 0 || lastRight != 0);
        }
      }
    }
  }
  else if (type == 'S') {
    // S,<seq>
    long s = 0;
    const char* p = line + 1;
    if (*p == ',') p++;
    if (parseInt(p, p + strlen(p), s)) {
      lastSeq = (uint32_t)s;
      lastValidMs = millis();
      watchdogFired = false;
      stopMotors();
    }
  }
  else if (type == 'P') {
    // P,<seq> -> ping, solo renueva el contacto
    long s = 0;
    const char* p = line + 1;
    if (*p == ',') p++;
    if (parseInt(p, p + strlen(p), s)) {
      lastSeq = (uint32_t)s;
      lastValidMs = millis();
      watchdogFired = false;
    }
  }
  // Cualquier otro tipo se ignora (y no renueva el watchdog).
}

// ---------------------------------------------------------------------------
//  SETUP
// ---------------------------------------------------------------------------
void setup() {
  // Los pines de control deben quedar en un estado conocido ANTES de configurar
  // el UART: primero parada, luego comunicación.
  pinMode(PIN_ENA, OUTPUT);
  pinMode(PIN_ENB, OUTPUT);
  pinMode(PIN_IN1, OUTPUT);
  pinMode(PIN_IN2, OUTPUT);
  pinMode(PIN_IN3, OUTPUT);
  pinMode(PIN_IN4, OUTPUT);
  pinMode(PIN_LED, OUTPUT);

  digitalWrite(PIN_ENA, LOW);
  digitalWrite(PIN_ENB, LOW);
  digitalWrite(PIN_IN1, LOW);
  digitalWrite(PIN_IN2, LOW);
  digitalWrite(PIN_IN3, LOW);
  digitalWrite(PIN_IN4, LOW);
  digitalWrite(PIN_LED, HIGH);        // LED frontal encendido como faro

  lastLeft = 0;
  lastRight = 0;
  lastValidMs = millis();
  driving = false;

  Serial.begin(115200);               // solo para depuración por USB
  Serial.println(F("[ROVERT] Controlador de motores listo (motores detenidos)"));

  Link.begin(UART_BAUD);
  Link.println(F("R,0,0,0"));         // saludo: estado detenido
}

// ---------------------------------------------------------------------------
//  LOOP
// ---------------------------------------------------------------------------
void loop() {
  // --- Recepción de tramas -------------------------------------------------
  while (Link.available() > 0) {
    char c = (char)Link.read();

    if (c == '\n') {
      rxBuffer[rxLen] = '\0';
      handleLine(rxBuffer, rxLen);
      rxLen = 0;
    } else if (rxLen < sizeof(rxBuffer) - 1) {
      rxBuffer[rxLen++] = c;
    } else {
      // Trama demasiado larga: se descarta entera (nunca mueve los motores).
      rxLen = 0;
    }
  }

  // --- Watchdog ------------------------------------------------------------
  uint32_t now = millis();
  if ((uint32_t)(now - lastValidMs) > WATCHDOG_MS) {
    if (lastLeft != 0 || lastRight != 0) {
      stopMotors();
      watchdogFired = true;
      char msg[24];
      snprintf(msg, sizeof(msg), "W,%lu", (unsigned long)lastSeq);
      Link.println(msg);
      Serial.println(F("[ROVERT] WATCHDOG: motores detenidos por falta de tramas"));
    }
    // Tras el timeout se exige una orden válida nueva (ver handleLine).
  }

  // --- Telemetría periódica ------------------------------------------------
  if ((uint32_t)(now - lastTelemetryMs) >= TELEMETRY_MS) {
    lastTelemetryMs = now;
    char msg[32];
    snprintf(msg, sizeof(msg), "R,%d,%d,%lu", (int)lastLeft, (int)lastRight,
             (unsigned long)lastSeq);
    Link.print(msg);
    Link.write('\n');
  }

  // --- LED frontal ---------------------------------------------------------
  // Faro fijo + parpadeo rápido mientras el rover se mueve, para verlo en la
  // demostración a distancia.
  if (driving) {
    digitalWrite(PIN_LED, ((now / 120) % 2) == 0 ? HIGH : LOW);
  } else {
    digitalWrite(PIN_LED, HIGH);
  }
}
