/*
 * ============================================================================
 *  Rovert — Firmware ESP32-CAM (Freenove ESP32-WROVER-CAM)
 * ============================================================================
 *  Archivo  : RovertCam.ino
 *  Versión  : 1.0.0
 *
 *  FUNCIONES
 *  ----------
 *   1. Crea una red Wi-Fi propia (SoftAP) "Rovert-Cam" -> 192.168.4.1
 *   2. Sirve el vídeo MJPEG de la cámara OV2640 en  http://192.168.4.1/stream
 *   3. Sirve un WebSocket de control en               ws://192.168.4.1/control
 *   4. Traduce las órdenes de la app a tramas UART para el Arduino UNO
 *   5. Envía telemetría y estado de vuelta a la app
 *
 *  PROTOCOLO (fuente de verdad: Docs/04-Protocolo-Comandos.md)
 * -----------------------------------------------------------
 *   App -> ESP32 (JSON por WebSocket)
 *     {"type":"drive","left":0.0,"right":0.0,"seq":42}
 *     {"type":"stop","seq":43}
 *     {"type":"ping","seq":44}
 *
 *   ESP32 -> App (JSON por WebSocket)
 *     {"type":"status","state":"stopped","left":0.0,"right":0.0,"seq":42,"uptimeMs":1234}
 *     {"type":"pong","seq":44}
 *
 *   ESP32 -> Arduino (UART2, 38400 8N1)
 *     D,<left_milli>,<right_milli>,<seq>\n
 *     S,<seq>\n
 *
 *   Arduino -> ESP32 (telemetría)
 *     R,<left_milli>,<right_milli>,<seq>\n
 *     W,<seq>\n
 *
 *  LIBRERÍAS NECESARIAS (incluidas en firmware/esp32cam/libraries/)
 *    - ESPAsyncWebServer 3.6.0 (mathieucarbou)  -> HTTP + WebSocket
 *    - ESPAsyncTCP       2.0.0 (esphome)        -> dependencia del anterior
 *
 *  PLACA: Freenove ESP32-WROVER-CAM  (¡OJO! los pines de cámara NO son los
 *         de AI-Thinker; ver config.h)
 * ============================================================================
 */

#include <Arduino.h>
#include <WiFi.h>
#include <esp_camera.h>
#include <ESPAsyncWebServer.h>
#include <ESPAsyncTCP.h>

#include "config.h"

// ---------------------------------------------------------------------------
//  OBJETOS GLOBALES
// ---------------------------------------------------------------------------
AsyncWebServer server(ROVERT_HTTP_PORT);
AsyncWebSocket ws(ROVERT_WS_PATH);

bool cameraReady = false;

// Estado del rover
float    cmdLeft = 0.0f;
float    cmdRight = 0.0f;
uint32_t cmdSeq = 0;
uint32_t lastCommandMs = 0;
bool     clientConnected = false;

// Buffer para el flujo MJPEG (se usa dentro del callback de streaming)
std::vector<uint8_t> mjpegChunk;
size_t              mjpegPos = 0;
camera_fb_t        *mjpegFrame = nullptr;

// ---------------------------------------------------------------------------
//  UTILIDADES DE PARSEO JSON (sin ArduinoJson para no añadir dependencias)
// ---------------------------------------------------------------------------

/** Busca "key" en el texto JSON y devuelve el valor numérico (double). */
static bool jsonGetNumber(const char *json, const char *key, double *out) {
  if (!json || !key || !out) return false;
  char pattern[48];
  snprintf(pattern, sizeof(pattern), "\"%s\"", key);
  const char *p = strstr(json, pattern);
  if (!p) return false;
  p += strlen(pattern);
  while (*p == ' ' || *p == ':' || *p == '\t') p++;
  if (*p == '\0') return false;
  char *end = nullptr;
  double v = strtod(p, &end);
  if (end == p) return false;
  *out = v;
  return true;
}

/** Busca "key" y devuelve el valor de cadena (sin comillas). */
static bool jsonGetString(const char *json, const char *key, char *out, size_t outLen) {
  if (!json || !key || !out || outLen == 0) return false;
  char pattern[48];
  snprintf(pattern, sizeof(pattern), "\"%s\"", key);
  const char *p = strstr(json, pattern);
  if (!p) return false;
  p += strlen(pattern);
  while (*p == ' ' || *p == ':' || *p == '\t') p++;
  if (*p != '"') return false;
  p++;
  size_t i = 0;
  while (*p != '\0' && *p != '"' && i < outLen - 1) out[i++] = *p++;
  out[i] = '\0';
  return true;
}

/** Recorta un valor a [-1.0, 1.0]. */
static float clampUnit(float v) {
  if (v > 1.0f) return 1.0f;
  if (v < -1.0f) return -1.0f;
  return v;
}

// ---------------------------------------------------------------------------
//  PUENTE UART -> ARDUINO
// ---------------------------------------------------------------------------

/** Envía una orden de movimiento al Arduino (milésimas -1000..1000). */
static void sendDriveToArduino(float left, float right, uint32_t seq) {
  long l = lroundf(clampUnit(left) * 1000.0f);
  long r = lroundf(clampUnit(right) * 1000.0f);
  char buf[40];
  int n = snprintf(buf, sizeof(buf), "D,%ld,%ld,%lu\n", l, r, (unsigned long)seq);
  Serial2.write((const uint8_t *)buf, n);
}

/** Envía un STOP explícito al Arduino. */
static void sendStopToArduino(uint32_t seq) {
  char buf[24];
  int n = snprintf(buf, sizeof(buf), "S,%lu\n", (unsigned long)seq);
  Serial2.write((const uint8_t *)buf, n);
}

/** Envía un ping dekeep-alive al Arduino. */
static void sendPingToArduino(uint32_t seq) {
  char buf[24];
  int n = snprintf(buf, sizeof(buf), "P,%lu\n", (unsigned long)seq);
  Serial2.write((const uint8_t *)buf, n);
}

/** Detiene todo y lo deja en estado seguro. */
static void emergencyStop(const char *motivo) {
  cmdLeft = 0.0f;
  cmdRight = 0.0f;
  sendStopToArduino(cmdSeq);
  Serial.printf("[ROVERT] STOP: %s\n", motivo);
}

// ---------------------------------------------------------------------------
//  WEBSOCKET DE CONTROL
// ---------------------------------------------------------------------------

static void broadcastStatus() {
  char json[160];
  const char *state = (cmdLeft != 0.0f || cmdRight != 0.0f) ? "driving" : "stopped";
  int n = snprintf(json, sizeof(json),
                   "{\"type\":\"status\",\"state\":\"%s\",\"left\":%.3f,\"right\":%.3f,\"seq\":%lu,\"uptimeMs\":%lu}",
                   state, cmdLeft, cmdRight, (unsigned long)cmdSeq,
                   (unsigned long)millis());
  if (n > 0) ws.textAll(json, n);
}

/** Procesa un mensaje JSON recibido de la app. */
static void handleControlMessage(const char *json) {
  char type[24];
  if (!jsonGetString(json, "type", type, sizeof(type))) {
    Serial.println(F("[ROVERT] Mensaje ignorado: sin campo 'type'"));
    return;
  }

  double seq = 0;
  if (!jsonGetNumber(json, "seq", &seq)) seq = (double)(++cmdSeq);
  cmdSeq = (uint32_t)seq;

  if (strcmp(type, "drive") == 0) {
    double left = 0.0, right = 0.0;
    if (!jsonGetNumber(json, "left", &left) || !jsonGetNumber(json, "right", &right)) {
      Serial.println(F("[ROVERT] 'drive' ignorado: faltan left/right"));
      return;
    }
    cmdLeft = clampUnit((float)left);
    cmdRight = clampUnit((float)right);
    lastCommandMs = millis();
    sendDriveToArduino(cmdLeft, cmdRight, cmdSeq);
  }
  else if (strcmp(type, "stop") == 0) {
    cmdLeft = 0.0f;
    cmdRight = 0.0f;
    lastCommandMs = millis();
    sendStopToArduino(cmdSeq);
    Serial.println(F("[ROVERT] STOP recibido de la app"));
  }
  else if (strcmp(type, "ping") == 0) {
    lastCommandMs = millis();
    char pong[48];
    int n = snprintf(pong, sizeof(pong), "{\"type\":\"pong\",\"seq\":%lu}", (unsigned long)cmdSeq);
    if (n > 0) ws.textAll(pong, n);
  }
  else {
    Serial.printf("[ROVERT] Tipo desconocido: %s\n", type);
  }
}

static void onWebSocketEvent(AsyncWebSocket *srv, AsyncWebSocketClient *client,
                             AwsEventType type, void *arg, uint8_t *data, size_t len) {
  (void)srv; (void)arg;

  switch (type) {
    case WS_EVT_CONNECT: {
      clientConnected = true;
      cmdLeft = 0.0f;
      cmdRight = 0.0f;
      lastCommandMs = millis();
      Serial.printf("[ROVERT] Cliente WebSocket conectado (id %u)\n", client->id());
      broadcastStatus();
      break;
    }
    case WS_EVT_DISCONNECT: {
      clientConnected = false;
      emergencyStop("cliente WebSocket desconectado");
      break;
    }
    case WS_EVT_DATA: {
      AwsWebSocketMessage *msg = (AwsWebSocketMessage *)arg;
      if (msg && msg->data && msg->len > 0) {
        // Se copia a un buffer local y se corta en '\0' para poder usar strstr.
        char buf[256];
        size_t n = msg->len < sizeof(buf) - 1 ? msg->len : sizeof(buf) - 1;
        memcpy(buf, msg->data, n);
        buf[n] = '\0';
        handleControlMessage(buf);
      }
      break;
    }
    case WS_EVT_ERROR:
      Serial.println(F("[ROVERT] Error en el WebSocket"));
      break;
    default:
      break;
  }
}

// ---------------------------------------------------------------------------
//  FLUJO MJPEG
// ---------------------------------------------------------------------------
//  La respuesta se declara "multipart/x-mixed-replace" y se va rellenando
//  fotograma a fotograma. Cada bloque lleva su propia cabecera con
//  Content-Length para que el parser de la app sea determinista.

static size_t fillMjpegChunk(uint8_t *buffer, size_t maxLen, size_t index) {
  (void)index;

  if (mjpegChunk.empty()) {
    if (mjpegFrame) {
      esp_camera_fb_return(mjpegFrame);
      mjpegFrame = nullptr;
    }

    camera_fb_t *fb = esp_camera_fb_get();
    if (!fb) {
      Serial.println(F("[ROVERT] camera_fb_get() devolvió NULL: fin del stream"));
      return 0;   // 0 => fin de la respuesta
    }

    char header[80];
    int hlen = snprintf(header, sizeof(header),
                        "--frame\r\nContent-Type: image/jpeg\r\nContent-Length: %u\r\n\r\n",
                        (unsigned)fb->len);
    mjpegChunk.resize((size_t)hlen + fb->len + 2);
    memcpy(mjpegChunk.data(), header, hlen);
    memcpy(mjpegChunk.data() + hlen, fb->buf, fb->len);
    mjpegChunk[hlen + fb->len] = '\r';
    mjpegChunk[hlen + fb->len + 1] = '\n';
    mjpegPos = 0;
    mjpegFrame = fb;
  }

  size_t remaining = mjpegChunk.size() - mjpegPos;
  size_t n = remaining < maxLen ? remaining : maxLen;
  if (n == 0) return 0;
  memcpy(buffer, mjpegChunk.data() + mjpegPos, n);
  mjpegPos += n;

  if (mjpegPos >= mjpegChunk.size()) {
    mjpegChunk.clear();
    mjpegPos = 0;
  }
  return n;
}

// ---------------------------------------------------------------------------
//  CÁMARA
// ---------------------------------------------------------------------------

static bool initCamera() {
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = CAM_PIN_Y2;
  config.pin_d1 = CAM_PIN_Y3;
  config.pin_d2 = CAM_PIN_Y4;
  config.pin_d3 = CAM_PIN_Y5;
  config.pin_d4 = CAM_PIN_Y6;
  config.pin_d5 = CAM_PIN_Y7;
  config.pin_d6 = CAM_PIN_Y8;
  config.pin_d7 = CAM_PIN_Y9;
  config.pin_xclk = CAM_PIN_XCLK;
  config.pin_pclk = CAM_PIN_PCLK;
  config.pin_vsync = CAM_PIN_VSYNC;
  config.pin_href = CAM_PIN_HREF;
  config.pin_sccb_sda = CAM_PIN_SIOD;
  config.pin_sccb_scl = CAM_PIN_SIOC;
  config.pin_pwdn = CAM_PIN_PWDN;
  config.pin_reset = CAM_PIN_RESET;
  config.xclk_freq_hz = CAMERA_XCLK_HZ;
  config.pixel_format = PIXFORMAT_JPEG;
  config.grab_mode = CAMERA_GRAB_LATEST;
  config.fb_location = CAMERA_FB_IN_PSRAM;
  config.frame_size = CAMERA_FRAMESIZE;
  config.jpeg_quality = CAMERA_QUALITY;
  config.fb_count = CAMERA_FB_COUNT;

  // La PSRAM es obligatoria en la WROVER: si el módulo no la expone, la
  // cámara no puede usar JPEG y el ESP32 se reinicia en bucle.
  if (psramFound()) {
    config.fb_location = CAMERA_FB_IN_PSRAM;
    config.fb_count = CAMERA_FB_COUNT;
    Serial.println(F("[ROVERT] PSRAM detectada"));
  } else {
    Serial.println(F("[ROVERT] AVISO: no se detecta PSRAM, el stream MJPEG no funcionara bien"));
    config.fb_location = CAMERA_FB_IN_DRAM;
    config.fb_count = 1;
  }

  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("[ROVERT] ERROR al iniciar la camara: 0x%x\n", (unsigned)err);
    return false;
  }

  sensor_t *s = esp_camera_sensor_get();
  if (s) {
    s->set_vflip(s, 1);      // la cámara viene invertida en vertical
    s->set_hmirror(s, 0);
  }
  cameraReady = true;
  Serial.println(F("[ROVERT] Camara OV2640 inicializada OK"));
  return true;
}

// ---------------------------------------------------------------------------
//  SETUP
// ---------------------------------------------------------------------------

void setup() {
  Serial.begin(115200);
  delay(200);
  Serial.println();
  Serial.println(F("========================================="));
  Serial.println(F("  ROVERT — ESP32-CAM  v1.0.0"));
  Serial.println(F("========================================="));

  // --- Red Wi-Fi propia ----------------------------------------------------
  WiFi.mode(WIFI_AP);
  WiFi.softAP(ROVERT_WIFI_SSID, ROVERT_WIFI_PASSWORD, ROVERT_WIFI_CHANNEL);
  IPAddress ip = WiFi.softAPIP();
  Serial.printf("[ROVERT] SoftAP '%s' creado. IP = %s\n",
                ROVERT_WIFI_SSID, ip.toString().c_str());

  // --- UART hacia el Arduino ----------------------------------------------
  Serial2.setPins(LINK_RX_PIN, LINK_TX_PIN);
  Serial2.begin(LINK_BAUD);
  Serial.printf("[ROVERT] UART2 listo: TX=%d RX=%d a %d baudios\n",
                LINK_TX_PIN, LINK_RX_PIN, LINK_BAUD);

  // --- Cámara --------------------------------------------------------------
  initCamera();

  // --- WebSocket -----------------------------------------------------------
  ws.onEvent(onWebSocketEvent);
  server.addHandler(&ws);

  // --- Diagnóstico ---------------------------------------------------------
  server.on(ROVERT_INFO_PATH, HTTP_GET, [](AsyncWebServerRequest *request) {
    char json[420];
    int n = snprintf(json, sizeof(json),
        "{\"device\":\"Rovert ESP32-CAM\",\"fw\":\"1.0.0\",\"ssid\":\"%s\","
        "\"ip\":\"%s\",\"camera\":%s,\"clients\":%u,\"uptimeMs\":%lu,"
        "\"state\":\"%s\",\"left\":%.3f,\"right\":%.3f}",
        ROVERT_WIFI_SSID, WiFi.softAPIP().toString().c_str(),
        cameraReady ? "true" : "false", ws.count(),
        (unsigned long)millis(),
        (cmdLeft != 0.0f || cmdRight != 0.0f) ? "driving" : "stopped",
        cmdLeft, cmdRight);
    request->send(200, "application/json", json);
  });

  // --- Estado --------------------------------------------------------------
  server.on(ROVERT_STATUS_PATH, HTTP_GET, [](AsyncWebServerRequest *request) {
    char json[200];
    int n = snprintf(json, sizeof(json),
        "{\"type\":\"status\",\"state\":\"%s\",\"left\":%.3f,\"right\":%.3f,\"seq\":%lu,\"uptimeMs\":%lu}",
        (cmdLeft != 0.0f || cmdRight != 0.0f) ? "driving" : "stopped",
        cmdLeft, cmdRight, (unsigned long)cmdSeq, (unsigned long)millis());
    request->send(200, "application/json", json);
  });

  // --- Fotograma suelto (útil para depurar) --------------------------------
  server.on("/capture", HTTP_GET, [](AsyncWebServerRequest *request) {
    camera_fb_t *fb = esp_camera_fb_get();
    if (!fb) {
      request->send(503, "text/plain", "camara no disponible");
      return;
    }
    AsyncWebServerResponse *response =
        request->beginResponse_P(200, "image/jpeg", (const uint8_t *)fb->buf, fb->len);
    request->send(response);
    esp_camera_fb_return(fb);
  });

  // --- Flujo MJPEG ---------------------------------------------------------
  server.on(ROVERT_STREAM_PATH, HTTP_GET, [](AsyncWebServerRequest *request) {
    AsyncWebServerResponse *response =
        request->beginChunkedResponse("multipart/x-mixed-replace; boundary=frame",
                                      [](uint8_t *buffer, size_t maxLen, size_t index) -> size_t {
                                        return fillMjpegChunk(buffer, maxLen, index);
                                      });
    response->addHeader("Access-Control-Allow-Origin", "*");
    request->send(response);
    Serial.println(F("[ROVERT] Cliente de video conectado"));
  });

  server.begin();
  Serial.println(F("[ROVERT] Servidor HTTP/WebSocket en marcha"));

  // Estado inicial: siempre detenido.
  cmdLeft = 0.0f;
  cmdRight = 0.0f;
  lastCommandMs = millis();
  sendStopToArduino(cmdSeq);
}

// ---------------------------------------------------------------------------
//  LOOP
// ---------------------------------------------------------------------------

void loop() {
  uint32_t now = millis();

  // --- Watchdog de la ESP32 ------------------------------------------------
  //  Si el cliente deja de mandar órdenes (se perdió el Wi-Fi, se cerró la
  //  app, el móvil se quedó sin batería), el rover se detiene solo.
  if ((uint32_t)(now - lastCommandMs) > ROVERT_CMD_TIMEOUT_MS) {
    if (cmdLeft != 0.0f || cmdRight != 0.0f) {
      emergencyStop("timeout de ordenes (watchdog de la ESP32)");
    } else {
      lastCommandMs = now;   // sigue vigilando sin reenviar STOP en bucle
    }
  }

  // --- Keep-alive hacia el Arduino ----------------------------------------
  static uint32_t lastPingMs = 0;
  if ((uint32_t)(now - lastPingMs) >= 1000) {
    lastPingMs = now;
    sendPingToArduino(cmdSeq);
  }

  // --- Telemetría del Arduino -> la app ------------------------------------
  static uint32_t lastTelemetryMs = 0;
  if ((uint32_t)(now - lastTelemetryMs) >= ROVERT_TELEMETRY_PERIOD_MS) {
    lastTelemetryMs = now;
    static char line[64];
    static uint8_t idx = 0;

    while (Serial2.available()) {
      char c = (char)Serial2.read();
      if (c == '\n') {
        line[idx] = '\0';
        if (idx > 0) {
          // "R,<l>,<r>,<seq>" -> estado real de los motores.
          // "W,<seq>"           -> el watchdog del Arduino venció.
          if (line[0] == 'W') {
            emergencyStop("watchdog del Arduino (sin tramas validas)");
          }
          else if (line[0] == 'R') {
            const char *p = line + 2;   // apunta al primer valor tras "R,"
            char *end = nullptr;
            double rl = strtod(p, &end);
            if (end && *end == ',') {
              double rr = strtod(end + 1, nullptr);
              cmdLeft = clampUnit((float)(rl / 1000.0));
              cmdRight = clampUnit((float)(rr / 1000.0));
            }
          }
        }
        idx = 0;
      } else if (idx < sizeof(line) - 1) {
        line[idx++] = c;
      } else {
        idx = 0;   // línea demasiado larga: se descarta
      }
    }
  }

  // --- Estado hacia la app --------------------------------------------------
  static uint32_t lastStatusMs = 0;
  if ((uint32_t)(now - lastStatusMs) >= ROVERT_STATUS_PERIOD_MS) {
    lastStatusMs = now;
    if (clientConnected) broadcastStatus();
  }

  delay(5);
}
