/* ==========================================================================
   Rovert — ARCHIVO GENERADO AUTOMÁTICAMENTE. NO EDITAR A MANO.
   ------------------------------------------------------------------------
   Se genera con:  node firmware/esp32cam/webapp/build-webapp.js
   Edita los archivos en firmware/esp32cam/webapp/ y vuelve a ejecutar
   el script. El resultado está versionado para poder compilar el
   firmware sin necesidad de Node.js.
   ========================================================================== */

#pragma once
#include <pgmspace.h>

// index.html  (4575 bytes)
static const char WEBAPP_INDEX_HTML[] PROGMEM =
R"RAWLITERAL(<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover">
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
<meta name="theme-color" content="#0d1117">
<title>Rovert Control</title>
<link rel="stylesheet" href="app.css">
</head>
<body>

<!-- ======================= CABECERA ======================= -->
<header class="barra">
  <div class="barra-izq">
    <span id="luz" class="luz luz--off" aria-hidden="true"></span>
    <span id="estado" class="estado">Conectando…</span>
  </div>
  <div class="barra-der">
    <span id="fps" class="fps">-- fps</span>
    <button id="btn-ajustes" class="icono" aria-label="Ajustes">⚙</button>
  </div>
</header>

<!-- ======================= VÍDEO ======================= -->
<section class="video-caja">
  <canvas id="lienzo" width="640" height="480"></canvas>
  <div id="video-msg" class="video-msg">Esperando cámara…</div>
</section>

<!-- ======================= ESTADO ======================= -->
<section class="estado-rover">
  <div class="celda">
    <span class="celda-titulo">Rover</span>
    <span id="estado-rover" class="celda-valor">—</span>
  </div>
  <div class="celda">
    <span class="celda-titulo">Modo</span>
    <span id="modo" class="celda-valor">TÁCTIL</span>
  </div>
  <div class="celda">
    <span class="celda-titulo">Izq / Der</span>
    <span id="consigna" class="celda-valor">0 / 0</span>
  </div>
</section>

<!-- ======================= CONTROL ======================= -->
<main class="control">
  <div class="cruceta" role="group" aria-label="Control de movimiento">
    <button class="boton boton--cruceta boton--arriba"    data-dir="up"    aria-label="Adelante">▲</button>
    <button class="boton boton--cruceta boton--izquierda" data-dir="left"  aria-label="Izquierda">◀</button>
    <button class="boton boton--centro" id="btn-stop" aria-label="Parar">■</button>
    <button class="boton boton--cruceta boton--derecha"  data-dir="right" aria-label="Derecha">▶</button>
    <button class="boton boton--cruceta boton--abajo"    data-dir="down"  aria-label="Atrás">▼</button>
  </div>

  <button id="btn-mando" class="mando-chip" aria-pressed="false">
    <span id="mando-nombre">Mando: no conectado</span>
  </button>
</main>

<!-- ======================= AJUSTES ======================= -->
<aside id="ajustes" class="ajustes" hidden>
  <div class="ajustes-cabecera">
    <h2>Ajustes</h2>
    <button id="btn-cerrar-ajustes" class="icono" aria-label="Cerrar">✕</button>
  </div>

  <label class="campo">
    <span>Servidor del rover</span>
    <input id="cfg-host" type="text" inputmode="url" placeholder="192.168.4.1" autocomplete="off">
    <small>Por defecto se usa la dirección desde la que se abrió esta página.</small>
  </label>

  <label class="campo">
    <span>Puerto</span>
    <input id="cfg-puerto" type="number" min="0" max="65535" inputmode="numeric" placeholder="80">
    <small>Deja <b>0</b> para usar el mismo puerto que esta página (lo normal: 80 en el rover).</small>
  </label>

  <label class="campo">
    <span>Velocidad máxima: <b id="lbl-velocidad">100 %</b></span>
    <input id="cfg-velocidad" type="range" min="10" max="100" step="5" value="100">
  </label>

  <label class="campo">
    <span>Polaridad motor izquierdo</span>
    <select id="cfg-sign-izq">
      <option value="1">Normal (+ = adelante)</option>
      <option value="-1">Invertida</option>
    </select>
  </label>

  <label class="campo">
    <span>Polaridad motor derecho</span>
    <select id="cfg-sign-der">
      <option value="1">Normal (+ = adelante)</option>
      <option value="-1">Invertida</option>
    </select>
  </label>

  <label class="campo">
    <span>Zona muerta del stick: <b id="lbl-zona">0.18</b></span>
    <input id="cfg-zona" type="range" min="0" max="40" step="1" value="18">
  </label>

  <label class="campo">
    <span>Fotogramas por segundo del vídeo: <b id="lbl-fps">10</b></span>
    <input id="cfg-fps" type="range" min="2" max="20" step="1" value="10">
  </label>

  <div class="ajustes-acciones">
    <button id="btn-reconectar" class="boton boton--normal">Reconectar</button>
    <button id="btn-resetear" class="boton boton--normal boton--gris">Restablecer</button>
  </div>

  <p class="nota" id="nota-info"></p>
</aside>

<script src="app.js"></script>
</body>
</html>
)RAWLITERAL";
static const size_t WEBAPP_INDEX_HTML_LEN = sizeof(WEBAPP_INDEX_HTML) - 1;

// app.css  (7005 bytes)
static const char WEBAPP_APP_CSS[] PROGMEM =
R"RAWLITERAL(/* ==========================================================================
   Rovert Control — hoja de estilos de la web app
   Diseño "móvil primero": la pantalla es de un teléfono en vertical.
   ========================================================================== */

:root {
  --fondo: #0d1117;
  --panel: #161b22;
  --panel-2: #1c2330;
  --borde: #2b3441;
  --texto: #e6edf3;
  --tenue: #8b949e;
  --verde: #2ea043;
  --rojo: #da3633;
  --ambar: #d29922;
  --acento: #1f6feb;
}

* { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }

html, body {
  margin: 0;
  padding: 0;
  background: var(--fondo);
  color: var(--texto);
  font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
  overscroll-behavior: none;
  -webkit-user-select: none;
  user-select: none;
  touch-action: manipulation;
}

body {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
  padding-top: env(safe-area-inset-top);
  padding-bottom: env(safe-area-inset-bottom);
}

/* ------------------------------ Cabecera ------------------------------ */
.barra {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
  background: var(--panel);
  border-bottom: 1px solid var(--borde);
}

.barra-izq, .barra-der { display: flex; align-items: center; gap: 8px; }

.luz {
  width: 11px; height: 11px;
  border-radius: 50%;
  background: var(--tenue);
  box-shadow: 0 0 0 3px rgba(139,148,158,.15);
  flex: 0 0 auto;
}
.luz--on   { background: var(--verde); box-shadow: 0 0 0 3px rgba(46,160,67,.20); }
.luz--warn { background: var(--ambar); box-shadow: 0 0 0 3px rgba(210,153,34,.20); }
.luz--err  { background: var(--rojo);   box-shadow: 0 0 0 3px rgba(218,54,51,.20); }

.estado { font-size: 13px; font-weight: 600; }
.fps { font-size: 12px; color: var(--tenue); font-variant-numeric: tabular-nums; }

.icono {
  background: transparent;
  border: 1px solid var(--borde);
  color: var(--texto);
  border-radius: 8px;
  width: 34px; height: 34px;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
}
.icono:active { background: var(--panel-2); }

/* ------------------------------ Vídeo ------------------------------ */
.video-caja {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  background: #000;
  overflow: hidden;
  border-bottom: 1px solid var(--borde);
}

.video-caja canvas {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: contain;
}

.video-msg {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--tenue);
  font-size: 14px;
  background: rgba(13,17,23,.75);
  text-align: center;
  padding: 16px;
  pointer-events: none;
}
.video-msg[hidden] { display: none; }

/* ------------------------------ Estado ------------------------------ */
.estado-rover {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  background: var(--panel);
  border-bottom: 1px solid var(--borde);
}

.celda {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  border-right: 1px solid var(--borde);
  min-width: 0;
}
.celda:last-child { border-right: none; }

.celda-titulo {
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: .06em;
  color: var(--tenue);
}
.celda-valor {
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.celda-valor.moviendo { color: var(--ambar); }
.celda-valor.parado   { color: var(--verde); }

/* ------------------------------ Control ------------------------------ */
.control {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  padding: 20px 16px;
}

.cruceta {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, 1fr);
  gap: 10px;
  width: min(78vw, 320px);
  aspect-ratio: 1 / 1;
}

.boton {
  border: 1px solid var(--borde);
  border-radius: 14px;
  background: var(--panel-2);
  color: var(--texto);
  font-size: 26px;
  cursor: pointer;
  touch-action: none;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background .08s linear, transform .08s linear;
}
.boton:active { transform: scale(.96); }
.boton.activo { background: var(--acento); border-color: var(--acento); }

.boton--cruceta   { background: var(--panel-2); }
.boton--arriba    { grid-area: 1 / 2; }
.boton--izquierda { grid-area: 2 / 1; }
.boton--centro    { grid-area: 2 / 2; background: var(--rojo); border-color: var(--rojo); font-size: 24px; }
.boton--centro:active { background: #b62324; }
.boton--derecha   { grid-area: 2 / 3; }
.boton--abajo     { grid-area: 3 / 2; }

.mando-chip {
  padding: 10px 16px;
  border-radius: 999px;
  border: 1px solid var(--borde);
  background: var(--panel);
  color: var(--tenue);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  max-width: 90vw;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mando-chip.conectado { color: var(--verde); border-color: rgba(46,160,67,.45); }

/* ------------------------------ Ajustes ------------------------------ */
.ajustes {
  position: fixed;
  inset: 0;
  z-index: 20;
  background: var(--fondo);
  padding: 14px 16px calc(24px + env(safe-area-inset-bottom));
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}
.ajustes[hidden] { display: none; }

.ajustes-cabecera {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.ajustes-cabecera h2 { margin: 0; font-size: 18px; }

.campo {
  display: block;
  margin-bottom: 18px;
  font-size: 13px;
  color: var(--tenue);
}
.campo > span { display: block; margin-bottom: 6px; color: var(--texto); font-weight: 600; }
.campo b { color: var(--acento); font-variant-numeric: tabular-nums; }
.campo small { display: block; margin-top: 5px; font-size: 11px; line-height: 1.4; }

.campo input[type="text"],
.campo input[type="number"],
.campo select {
  width: 100%;
  padding: 11px 12px;
  background: var(--panel);
  border: 1px solid var(--borde);
  border-radius: 10px;
  color: var(--texto);
  font-size: 16px; /* evita el zoom automático en iOS */
  font-family: inherit;
}

.campo input[type="range"] {
  width: 100%;
  accent-color: var(--acento);
  height: 32px;
}

.ajustes-acciones { display: flex; gap: 10px; margin-top: 6px; }

.boton--normal {
  flex: 1;
  padding: 13px 10px;
  font-size: 15px;
  font-weight: 700;
  background: var(--acento);
  border-color: var(--acento);
  border-radius: 12px;
  cursor: pointer;
}
.boton--gris { background: var(--panel-2); border-color: var(--borde); color: var(--texto); }

.nota {
  margin-top: 18px;
  padding: 12px;
  border-radius: 10px;
  background: var(--panel);
  border: 1px solid var(--borde);
  color: var(--tenue);
  font-size: 12px;
  line-height: 1.5;
}
.nota:empty { display: none; }

@media (min-height: 780px) {
  .control { gap: 26px; }
  .cruceta { width: min(72vw, 360px); }
}
)RAWLITERAL";
static const size_t WEBAPP_APP_CSS_LEN = sizeof(WEBAPP_APP_CSS) - 1;

// app.js  (20226 bytes)
static const char WEBAPP_APP_JS[] PROGMEM =
R"RAWLITERAL(/* ==========================================================================
   Rovert Control — web app servida por la propia ESP32-CAM
   --------------------------------------------------------------------------
   Permite controlar el rover desde CUALQUIER navegador (iPhone, iPad,
   Android, PC) sin instalar nada: el rover ya es un servidor HTTP + WebSocket.

   Protocolo: ver Docs/04-Protocolo-Comandos.md
     App -> ESP32 : {"type":"drive","left":0.0,"right":0.0,"seq":42}
                    {"type":"stop","seq":43}
     ESP32 -> App : {"type":"status","state":"stopped|driving",...}

   Reglas de seguridad (idénticas a la app Android):
     - El rover solo se mueve mientras se mantiene pulsado el control.
     - Al soltar, al perder el foco, al ocultar la pestaña o al caerse el
       WebSocket, se envía STOP de inmediato.
     - El watchdog del firmware (500 ms) es la última barrera.
   ========================================================================== */
'use strict';

/* ------------------------------------------------------------------ */
/* Ajustes persistentes                                               */
/* ------------------------------------------------------------------ */

const CLAVE_ALMACEN = 'rovert.settings';

const ajustesPorDefecto = {
  host: '',            // vacío = usar el host desde el que se abrió la página
  puerto: 0,           // 0 = mismo puerto que el de la página (normalmente 80)
  velocidad: 100,      // % de velocidad máxima permitida (10..100)
  signoIzq: 1,         // +1 normal, -1 invertido
  signoDer: 1,
  zonaMuerta: 0.18,    // 0..0.40
  fps: 10              // fotogramas por segundo pedidos al vídeo
};

let ajustes = Object.assign({}, ajustesPorDefecto);

function cargarAjustes() {
  try {
    const guardado = JSON.parse(localStorage.getItem(CLAVE_ALMACEN) || '{}');
    ajustes = Object.assign({}, ajustesPorDefecto, guardado);
  } catch (e) {
    ajustes = Object.assign({}, ajustesPorDefecto);
  }
  // Saneado defensivo: un valor corrupto en localStorage no debe romper nada.
  ajustes.puerto = clamp(parseInt(ajustes.puerto, 10) || 0, 0, 65535);
  ajustes.velocidad = clamp(parseInt(ajustes.velocidad, 10) || 100, 10, 100);
  ajustes.signoIzq = ajustes.signoIzq === -1 ? -1 : 1;
  ajustes.signoDer = ajustes.signoDer === -1 ? -1 : 1;
  ajustes.zonaMuerta = clamp(Number(ajustes.zonaMuerta) || 0.18, 0, 0.40);
  ajustes.fps = clamp(parseInt(ajustes.fps, 10) || 10, 2, 20);
}

function guardarAjustes() {
  try {
    localStorage.setItem(CLAVE_ALMACEN, JSON.stringify(ajustes));
  } catch (e) { /* modo privado: se pierde, no es crítico */ }
}

function clamp(v, min, max) { return v < min ? min : (v > max ? max : v); }

/* ------------------------------------------------------------------ */
/* Referencias del DOM                                                */
/* ------------------------------------------------------------------ */

const el = {
  luz: document.getElementById('luz'),
  estado: document.getElementById('estado'),
  fps: document.getElementById('fps'),
  lienzo: document.getElementById('lienzo'),
  videoMsg: document.getElementById('video-msg'),
  estadoRover: document.getElementById('estado-rover'),
  modo: document.getElementById('modo'),
  consigna: document.getElementById('consigna'),
  btnStop: document.getElementById('btn-stop'),
  btnAjustes: document.getElementById('btn-ajustes'),
  btnCerrar: document.getElementById('btn-cerrar-ajustes'),
  btnReconectar: document.getElementById('btn-reconectar'),
  btnResetear: document.getElementById('btn-resetear'),
  btnMando: document.getElementById('btn-mando'),
  mandoNombre: document.getElementById('mando-nombre'),
  ajustes: document.getElementById('ajustes'),
  nota: document.getElementById('nota-info'),
  cfgHost: document.getElementById('cfg-host'),
  cfgPuerto: document.getElementById('cfg-puerto'),
  cfgVelocidad: document.getElementById('cfg-velocidad'),
  cfgSignoIzq: document.getElementById('cfg-sign-izq'),
  cfgSignoDer: document.getElementById('cfg-sign-der'),
  cfgZona: document.getElementById('cfg-zona'),
  cfgFps: document.getElementById('cfg-fps'),
  lblVelocidad: document.getElementById('lbl-velocidad'),
  lblZona: document.getElementById('lbl-zona'),
  lblFps: document.getElementById('lbl-fps')
};

/* ------------------------------------------------------------------ */
/* Direcciones y mapeo a los dos motores                               */
/* ------------------------------------------------------------------ */

const DIRECCIONES = {
  //  left / right en [-1..1], tal y como define Docs/04-Protocolo-Comandos.md
  up:    { left:  1.0, right:  1.0 },   // adelante
  down:  { left: -1.0, right: -1.0 },   // atrás
  left:  { left: -1.0, right:  1.0 },   // giro a la izquierda
  right: { left:  1.0, right: -1.0 }    // giro a la derecha
};

function aplicarAjustes(dir) {
  const escala = ajustes.velocidad / 100;
  return {
    left: dir.left * ajustes.signoIzq * escala,
    right: dir.right * ajustes.signoDer * escala
  };
}

/* ------------------------------------------------------------------ */
/* Estado de la conexión                                              */
/* ------------------------------------------------------------------ */

let ws = null;
let seq = 0;
let reconexionMs = 1000;
let temporizadorReconexion = null;
let conectado = false;

function hostEfectivo() {
  return (ajustes.host || location.hostname || '192.168.4.1').trim();
}

/** 0 = el mismo puerto que el de la página (lo normal: 80 en la ESP32). */
function puertoEfectivo() {
  if (ajustes.puerto > 0) return ajustes.puerto;
  const p = parseInt(location.port, 10);
  return (p && p > 0) ? p : 80;
}

function urlBase() { return 'http://' + hostEfectivo() + ':' + puertoEfectivo(); }
function urlWs() { return 'ws://' + hostEfectivo() + ':' + puertoEfectivo() + '/control'; }

function siguienteSeq() { seq = (seq + 1) & 0x7fffffff; return seq; }

function ponerLuz(clase, texto) {
  el.luz.className = 'luz ' + clase;
  if (texto !== undefined) el.estado.textContent = texto;
}

function conectar() {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return;

  limpiarReconexion();
  ponerLuz('luz--warn', 'Conectando…');

  try {
    ws = new WebSocket(urlWs());
  } catch (e) {
    programarReconexion();
    return;
  }

  ws.onopen = function () {
    conectado = true;
    reconexionMs = 1000;
    ponerLuz('luz--on', 'Conectado');
    el.nota.textContent = 'Servidor: ' + hostEfectivo() + ':' + puertoEfectivo();
    enviarPing();
  };

  ws.onmessage = function (ev) {
    let datos;
    try { datos = JSON.parse(ev.data); } catch (e) { return; }
    if (!datos || datos.type !== 'status') return;
    const estado = datos.state === 'driving' ? 'MOVIENDO' : 'DETENIDO';
    el.estadoRover.textContent = estado;
    el.estadoRover.className = 'celda-valor ' + (datos.state === 'driving' ? 'moviendo' : 'parado');
    if (!direccionActiva) {
      el.consigna.textContent = fmt(datos.left) + ' / ' + fmt(datos.right);
    }
  };

  ws.onerror = function () { /* onclose se encarga */ };

  ws.onclose = function () {
    conectado = false;
    ws = null;
    liberarTodo();               // si estaba moviéndose, manda STOP
    ponerLuz('luz--err', 'Sin conexión');
    programarReconexion();
  };
}

function programarReconexion() {
  limpiarReconexion();
  temporizadorReconexion = setTimeout(conectar, reconexionMs);
  // Backoff suave: 1 s -> 10 s
  reconexionMs = Math.min(reconexionMs * 1.6, 10000);
}

function limpiarReconexion() {
  if (temporizadorReconexion) { clearTimeout(temporizadorReconexion); temporizadorReconexion = null; }
}

function enviar(obj) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    try { ws.send(JSON.stringify(obj)); } catch (e) { /* se reconectará */ }
  }
}

function enviarPing() { enviar({ type: 'ping', seq: siguienteSeq() }); }

function fmt(v) {
  const n = Number(v) || 0;
  return (Math.round(n * 100) / 100).toFixed(2);
}

/* ------------------------------------------------------------------ */
/* Órdenes de movimiento                                               */
/* ------------------------------------------------------------------ */

let direccionActiva = null;     // 'up' | 'down' | 'left' | 'right' | null
let temporizadorOrden = null;
let enviadoUltimaVez = 0;

function enviarOrden() {
  if (!direccionActiva) return;
  const d = aplicarAjustes(DIRECCIONES[direccionActiva]);
  enviar({ type: 'drive', left: round3(d.left), right: round3(d.right), seq: siguienteSeq() });
  el.consigna.textContent = fmt(d.left) + ' / ' + fmt(d.right);
  el.estadoRover.textContent = 'MOVIENDO';
  el.estadoRover.className = 'celda-valor moviendo';
  enviadoUltimaVez = Date.now();
}

function round3(v) { return Math.round(v * 1000) / 1000; }

function iniciarOrden(dir) {
  if (direccionActiva === dir) return;
  direccionActiva = dir;
  marcarBoton(dir, true);
  el.modo.textContent = 'TÁCTIL';
  enviarOrden();
  if (!temporizadorOrden) {
    // 10 Hz: refresco de la consigna mientras se mantiene pulsado
    temporizadorOrden = setInterval(enviarOrden, 100);
  }
}

function detenerOrden() {
  if (!direccionActiva) return;
  const dir = direccionActiva;
  direccionActiva = null;
  marcarBoton(dir, false);
  if (temporizadorOrden) { clearInterval(temporizadorOrden); temporizadorOrden = null; }
  enviar({ type: 'stop', seq: siguienteSeq() });
  el.consigna.textContent = '0 / 0';
  el.estadoRover.textContent = conectado ? 'DETENIDO' : '—';
  el.estadoRover.className = 'celda-valor ' + (conectado ? 'parado' : '');
}

/** Suelta cualquier control activo y asegura el STOP. */
function liberarTodo() {
  if (direccionActiva) {
    detenerOrden();
  } else {
    enviar({ type: 'stop', seq: siguienteSeq() });
  }
  marcaMando = null;
}

function marcarBoton(dir, activo) {
  document.querySelectorAll('[data-dir]').forEach(function (b) {
    b.classList.toggle('activo', !!activo && b.getAttribute('data-dir') === dir);
  });
}

/* ------------------------------------------------------------------ */
/* Cruceta táctil                                                     */
/* ------------------------------------------------------------------ */

function conectarCruceta() {
  document.querySelectorAll('[data-dir]').forEach(function (boton) {
    const dir = boton.getAttribute('data-dir');

    boton.addEventListener('pointerdown', function (ev) {
      ev.preventDefault();
      try { boton.setPointerCapture(ev.pointerId); } catch (e) { /* no crítico */ }
      iniciarOrden(dir);
    });

    const soltar = function () { if (direccionActiva === dir) detenerOrden(); };
    boton.addEventListener('pointerup', soltar);
    boton.addEventListener('pointercancel', soltar);
    boton.addEventListener('lostpointercapture', soltar);
    boton.addEventListener('pointerleave', function (ev) {
      // Si el dedo se sale del botón con el puntero capturado, seguimos
      // controlando; sólo soltamos si no hay captura activa.
      if (boton.hasPointerCapture && !boton.hasPointerCapture(ev.pointerId)) soltar();
    });

    boton.addEventListener('contextmenu', function (ev) { ev.preventDefault(); });
  });

  el.btnStop.addEventListener('pointerdown', function (ev) {
    ev.preventDefault();
    detenerOrden();
    enviar({ type: 'stop', seq: siguienteSeq() });
  });
}

/* ------------------------------------------------------------------ */
/* Mando (Gamepad API)                                                */
/* ------------------------------------------------------------------ */

let marcaMando = null;          // 'up' | 'down' | 'left' | 'right' | null
let nombreMando = '';

function actualizarMando() {
  if (nombreMando) {
    el.mandoNombre.textContent = 'Mando: ' + nombreMando;
    el.btnMando.classList.add('conectado');
  } else {
    el.mandoNombre.textContent = 'Mando: no conectado';
    el.btnMando.classList.remove('conectado');
  }
}

/**
 * Traduce el estado del mando a una dirección.
 * Se usa el stick izquierdo (axes 0 y 1) y, si existen, los botones de la
 * cruceta (12-15 en el mapeo estándar). Tiene prioridad la componente
 * vertical: el rover solo define cuatro movimientos, no diagonales.
 */
function direccionDelMando(gp) {
  if (!gp || !gp.connected) return null;

  const zona = ajustes.zonaMuerta;

  // Botones de la cruceta (mapeo estándar del Gamepad API)
  const btn = gp.buttons || [];
  if (btn[12] && btn[12].pressed) return 'up';
  if (btn[13] && btn[13].pressed) return 'down';
  if (btn[14] && btn[14].pressed) return 'left';
  if (btn[15] && btn[15].pressed) return 'right';

  // Stick izquierdo
  const ejes = gp.axes || [];
  const x = ejes.length > 0 ? ejes[0] : 0;
  const y = ejes.length > 1 ? ejes[1] : 0;

  if (y <= -zona) return 'up';
  if (y >= zona) return 'down';
  if (x <= -zona) return 'left';
  if (x >= zona) return 'right';

  return null;
}

function bucleMando() {
  const pads = (navigator.getGamepads ? navigator.getGamepads() : []) || [];
  let activo = null;
  let nombre = '';

  for (let i = 0; i < pads.length; i++) {
    const gp = pads[i];
    if (!gp || !gp.connected) continue;
    if (!nombre) nombre = gp.id || 'Mando';
    const dir = direccionDelMando(gp);
    if (dir) { activo = dir; break; }
  }

  if (nombre !== nombreMando) { nombreMando = nombre; actualizarMando(); }

  if (activo && !marcaMando) {
    marcaMando = activo;
    el.modo.textContent = 'MANDO';
    iniciarOrden(activo);
  } else if (!activo && marcaMando) {
    marcaMando = null;
    if (direccionActiva) detenerOrden();
  }

  requestAnimationFrame(bucleMando);
}

/* ------------------------------------------------------------------ */
/* Vídeo: peticiones de /capture y pintado en el lienzo                */
/* ------------------------------------------------------------------ */

let frames = 0;
let ultimoConteo = Date.now();
let videoActivo = true;

function dormir(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }

async function bucleVideo() {
  while (videoActivo) {
    const inicio = Date.now();
    try {
      const res = await fetch(urlBase() + '/capture', { cache: 'no-store' });
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const blob = await res.blob();
      if (blob.size === 0) throw new Error('fotograma vacío');
      await pintar(blob);
      frames++;
      el.videoMsg.hidden = true;
    } catch (e) {
      el.videoMsg.hidden = false;
      el.videoMsg.textContent = 'Sin señal de vídeo (' + (e && e.message ? e.message : 'error') + ')';
    }

    const objetivo = Math.max(1000 / ajustes.fps, 100);
    const gastado = Date.now() - inicio;
    await dormir(Math.max(0, objetivo - gastado));
  }
}

async function pintar(blob) {
  const lienzo = el.lienzo;
  const ctx = lienzo.getContext('2d');
  let bitmap = null;

  if (typeof createImageBitmap === 'function') {
    bitmap = await createImageBitmap(blob);
  } else {
    // Reserva para navegadores antiguos: se decodifica con un <img>.
    const url = URL.createObjectURL(blob);
    try {
      const img = new Image();
      await new Promise(function (res, rej) {
        img.onload = res; img.onerror = rej; img.src = url;
      });
      if (lienzo.width !== img.naturalWidth || lienzo.height !== img.naturalHeight) {
        lienzo.width = img.naturalWidth; lienzo.height = img.naturalHeight;
      }
      ctx.drawImage(img, 0, 0);
      return;
    } finally {
      URL.revokeObjectURL(url);
    }
  }

  if (lienzo.width !== bitmap.width || lienzo.height !== bitmap.height) {
    lienzo.width = bitmap.width;
    lienzo.height = bitmap.height;
  }
  ctx.drawImage(bitmap, 0, 0);
  if (bitmap.close) bitmap.close();
}

/* Contador de FPS */
setInterval(function () {
  const ahora = Date.now();
  const seg = (ahora - ultimoConteo) / 1000;
  if (seg >= 0.5) {
    el.fps.textContent = Math.round(frames / seg) + ' fps';
    frames = 0;
    ultimoConteo = ahora;
  }
}, 500);

/* ------------------------------------------------------------------ */
/* Ajustes: interfaz                                                  */
/* ------------------------------------------------------------------ */

function pintarAjustes() {
  el.cfgHost.value = ajustes.host || '';
  el.cfgPuerto.value = ajustes.puerto;
  el.cfgVelocidad.value = ajustes.velocidad;
  el.cfgSignoIzq.value = String(ajustes.signoIzq);
  el.cfgSignoDer.value = String(ajustes.signoDer);
  el.cfgZona.value = Math.round(ajustes.zonaMuerta * 100);
  el.cfgFps.value = ajustes.fps;
  actualizarEtiquetas();
}

function actualizarEtiquetas() {
  el.lblVelocidad.textContent = ajustes.velocidad + ' %';
  el.lblZona.textContent = ajustes.zonaMuerta.toFixed(2);
  el.lblFps.textContent = ajustes.fps;
}

function leerAjustes() {
  ajustes.host = el.cfgHost.value.trim();
  ajustes.puerto = clamp(parseInt(el.cfgPuerto.value, 10) || 0, 0, 65535);
  ajustes.velocidad = clamp(parseInt(el.cfgVelocidad.value, 10) || 100, 10, 100);
  ajustes.signoIzq = el.cfgSignoIzq.value === '-1' ? -1 : 1;
  ajustes.signoDer = el.cfgSignoDer.value === '-1' ? -1 : 1;
  ajustes.zonaMuerta = clamp(parseInt(el.cfgZona.value, 10) / 100, 0, 0.40);
  ajustes.fps = clamp(parseInt(el.cfgFps.value, 10) || 10, 2, 20);
  guardarAjustes();
  actualizarEtiquetas();
}

function conectarAjustes() {
  el.btnAjustes.addEventListener('click', function () {
    pintarAjustes();
    el.ajustes.hidden = false;
  });
  el.btnCerrar.addEventListener('click', function () { el.ajustes.hidden = true; });

  [el.cfgHost, el.cfgPuerto, el.cfgVelocidad, el.cfgSignoIzq, el.cfgSignoDer, el.cfgZona, el.cfgFps]
    .forEach(function (campo) {
      campo.addEventListener('change', function () {
        const hostAntes = hostEfectivo();
        leerAjustes();
        if (hostEfectivo() !== hostAntes) {
          // Cambió el servidor: hay que reconectar y recargar el vídeo.
          if (ws) { try { ws.close(); } catch (e) { /* ignorar */ } }
          window.location.reload();
        }
      });
    });

  el.btnReconectar.addEventListener('click', function () {
    if (ws) { try { ws.close(); } catch (e) { /* ignorar */ } }
    ws = null;
    reconexionMs = 1000;
    conectar();
  });

  el.btnResetear.addEventListener('click', function () {
    ajustes = Object.assign({}, ajustesPorDefecto);
    guardarAjustes();
    pintarAjustes();
    window.location.reload();
  });

  el.btnMando.addEventListener('click', function () {
    el.nota.textContent = nombreMando
      ? 'Mando detectado: ' + nombreMando
      : 'Conecta el mando por Bluetooth en los ajustes del sistema y vuelve a pulsar aquí.';
  });
}

/* ------------------------------------------------------------------ */
/* Seguridad: STOP ante cualquier pérdida de control                  */
/* ------------------------------------------------------------------ */

function conectarSeguridad() {
  document.addEventListener('visibilitychange', function () {
    if (document.hidden) liberarTodo();
  });

  window.addEventListener('pagehide', function () { liberarTodo(); });
  window.addEventListener('beforeunload', function () { liberarTodo(); });

  // Si el usuario cambia de app o de ventana, el rover debe detenerse.
  window.addEventListener('blur', function () { liberarTodo(); });

  // Pausa larga: el rover no debe quedarse moviéndose solo.
  setInterval(function () {
    if (direccionActiva && (Date.now() - enviadoUltimaVez) > 1500) detenerOrden();
  }, 500);
}

/* ------------------------------------------------------------------ */
/* Arranque                                                           */
/* ------------------------------------------------------------------ */

function arrancar() {
  cargarAjustes();
  pintarAjustes();
  conectarCruceta();
  conectarAjustes();
  conectarSeguridad();
  conectar();
  bucleVideo();
  requestAnimationFrame(bucleMando);

  el.mandoNombre.textContent = 'Mando: no conectado';
  el.nota.textContent = 'Si no se ve el vídeo, revisa que el Wi-Fi del rover esté conectado.';
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', arrancar);
} else {
  arrancar();
}
)RAWLITERAL";
static const size_t WEBAPP_APP_JS_LEN = sizeof(WEBAPP_APP_JS) - 1;

// Tipo MIME de cada recurso (para las cabeceras Content-Type).
// index.html -> text/html
// app.css -> text/css
// app.js -> application/javascript

