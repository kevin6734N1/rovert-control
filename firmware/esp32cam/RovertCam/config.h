/*
 * ============================================================================
 *  Rovert — Configuración central del firmware ESP32-CAM
 * ============================================================================
 *  Archivo  : config.h
 *  Versión  : 1.0.0
 *
 *  Este archivo concentra TODAS las constantes ajustables del rover.
 *  Si el taller necesita cambiar un pin, una contraseña o una velocidad,
 *  se hace aquí y solo aquí.
 * ============================================================================
 */

#ifndef ROVERT_CONFIG_H
#define ROVERT_CONFIG_H

// ---------------------------------------------------------------------------
//  RED WI-FI (SoftAP / punto de acceso propio)
// ---------------------------------------------------------------------------
//  El rover NO necesita router ni Internet: la ESP32-CAM crea su propia red.
//  La app Android se conecta a esta red y habla con 192.168.4.1
#define ROVERT_WIFI_SSID      "Rovert-Cam"
#define ROVERT_WIFI_PASSWORD  "rovert2026"     // WPA2, mínimo 8 caracteres
#define ROVERT_WIFI_CHANNEL   6

// ---------------------------------------------------------------------------
//  PINES DE LA CÁMARA OV2640 — PLACA FREENOVE ESP32-WROVER-CAM
// ---------------------------------------------------------------------------
//  Fuente: documentación oficial Freenove (fnk0060) y el ejemplo "Camera"
//  de la librería ESP32. LA TABLA ES DISTINTA a la de la placa AI-Thinker:
//  NO cambies estos valores por los de AI-Thinker (XCLK=0, Y2=5...), son
//  incompatibles y la cámara no funcionaría.
//
//   XCLK  = GPIO21   SIOD = GPIO26   SIOC = GPIO27
//   VSYNC = GPIO25   HREF = GPIO23   PCLK  = GPIO22
//   Y9=35  Y8=34  Y7=39  Y6=36  Y5=19  Y4=18  Y3=5  Y2=4
//   PWDN  = -1 (no usado)     RESET = -1 (no usado)
//
//  En la placa Freenove el pin 0 (GPIO0) queda libre para el botón BOOT.
// ---------------------------------------------------------------------------
#define CAM_PIN_PWDN    -1
#define CAM_PIN_RESET   -1
#define CAM_PIN_XCLK    21
#define CAM_PIN_SIOD    26
#define CAM_PIN_SIOC    27
#define CAM_PIN_Y9      35
#define CAM_PIN_Y8      34
#define CAM_PIN_Y7      39
#define CAM_PIN_Y6      36
#define CAM_PIN_Y5      19
#define CAM_PIN_Y4      18
#define CAM_PIN_Y3      5
#define CAM_PIN_Y2      4
#define CAM_PIN_VSYNC   25
#define CAM_PIN_HREF    23
#define CAM_PIN_PCLK    22

// ---------------------------------------------------------------------------
//  CÁMARA — CALIDAD Y RESOLUCIÓN
// ---------------------------------------------------------------------------
//  VGA (640x480) es el punto dulce: ~10-15 FPS en una WROVER con PSRAM.
//  Si la imagen va a trompicones, sube FRAMESIZE a SVGA; si se congela,
//  bájalo a QQVGA. NO uses UXGA (800x600+) sin PSRAM: la cámara se queda
//  sin buffers y el ESP32 se reinicia en bucle.
#define CAMERA_XCLK_HZ      20000000
#define CAMERA_QUALITY      12     // 0-63. 10-14 = buena calidad y rápido
#define CAMERA_FB_COUNT     2      // 2 con PSRAM (WROVER). 1 si da problemas
#define CAMERA_FRAMESIZE    FRAMESIZE_VGA
#define CAMERA_MAX_FRAMESIZE FRAMESIZE_SVGA

// ---------------------------------------------------------------------------
//  UART ESP32 -> ARDUINO UNO
// ---------------------------------------------------------------------------
//  UART2 es totalmente reconfigurable por la matriz de GPIO del ESP32.
//
//  Elección: GPIO32 (TX) y GPIO33 (RX).
//    - No son pines de la cámara (21,26,27,25,23,35,34,39,36,4,19,5,18,22).
//    - No son pines de strapping (0, 2, 5, 12, 15) -> arranque seguro.
//    - No son GPIO16/17: en el módulo ESP32-WROVER la PSRAM interna los
//      ocupa (confirmado en la documentación oficial de Freenove).
//    - No son GPIO6..11: los usa la flash interna del módulo.
//
//  Alternativas válidas si en tu placa hay otro conflicto: 13/14 o 4/5
//  (cambia también CAM_PIN_Y2 / CAM_PIN_Y3 y recalcula).
#define LINK_UART_NUM      2
#define LINK_TX_PIN        32      // ESP32 TX  ->  Arduino RX
#define LINK_RX_PIN        33      // ESP32 RX  <-  Arduino TX (5 V: ver divisor)
#define LINK_BAUD          38400   // 8N1

// ---------------------------------------------------------------------------
//  SERVIDOR
// ---------------------------------------------------------------------------
#define ROVERT_HTTP_PORT     80
#define ROVERT_WS_PATH       "/control"
#define ROVERT_STREAM_PATH   "/stream"
#define ROVERT_STATUS_PATH   "/status"
#define ROVERT_INFO_PATH     "/"

// ---------------------------------------------------------------------------
//  TIEMPOS / SEGURIDAD
// ---------------------------------------------------------------------------
//  Watchdog de la ESP32: si el cliente no manda ninguna orden durante este
//  tiempo, se envía STOP al Arduino. Es una segunda barrera de seguridad
//  (la primera es el watchdog de 400 ms del propio Arduino).
#define ROVERT_CMD_TIMEOUT_MS   500

//  Periodo del envío de "status" por WebSocket hacia la app.
#define ROVERT_STATUS_PERIOD_MS 1000

//  Periodo de lectura del UART (telemetría del Arduino).
#define ROVERT_TELEMETRY_PERIOD_MS 200

// ---------------------------------------------------------------------------
//  NOTA SOBRE LOS NIVELES DE VOLTAJE
// ---------------------------------------------------------------------------
//  ESP32 = 3.3 V ; Arduino UNO = 5 V.
//   - ESP32 TX -> UNO RX : conexión directa (3.3 V es nivel HIGH para el UNO).
//   - UNO TX   -> ESP32 RX : NECESITA divisor resistivo (1 kΩ serie + 2 kΩ a GND)
//                          para bajar los 5 V a 3.3 V y no dañar el ESP32.
#endif // ROVERT_CONFIG_H
