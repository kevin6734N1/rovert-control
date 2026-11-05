#!/usr/bin/env node
/* ==========================================================================
   Rovert — generador del encabezado del firmware con la web app
   --------------------------------------------------------------------------
   Lee los archivos de `firmware/esp32cam/webapp/` y genera
   `firmware/esp32cam/RovertCam/webapp_generated.h` con el contenido como
   literales de cadena en PROGMEM, para que la ESP32 pueda servirlo sin
   necesidad de un sistema de archivos.

   Uso:
     node firmware/esp32cam/webapp/build-webapp.js

   El archivo generado está versionado en Git, así que NO hace falta ejecutar
   este script para compilar el firmware. Solo hace falta después de editar
   index.html, app.css o app.js.
   ========================================================================== */

'use strict';

const fs = require('fs');
const path = require('path');

const dirWebapp = __dirname;
const destino = path.join(dirWebapp, '..', 'RovertCam', 'webapp_generated.h');

const archivos = [
  { entrada: 'index.html', simbolo: 'WEBAPP_INDEX_HTML', tipo: 'text/html' },
  { entrada: 'app.css',    simbolo: 'WEBAPP_APP_CSS',    tipo: 'text/css' },
  { entrada: 'app.js',     simbolo: 'WEBAPP_APP_JS',     tipo: 'application/javascript' }
];

const delimitador = 'RAWLITERAL';

function literalSeguro(contenido) {
  // Un literal de cadena "crudo" no puede contener la secuencia de cierre.
  if (contenido.indexOf(')' + delimitador + '"') !== -1) {
    throw new Error(
      'El contenido contiene la secuencia de cierre del literal crudo ' +
      '(")' + delimitador + '"). Cambia el delimitador en build-webapp.js.'
    );
  }
  return 'R"' + delimitador + '(' + contenido + ')' + delimitador + '"';
}

let salida = '';
salida += '/* ==========================================================================\n';
salida += '   Rovert — ARCHIVO GENERADO AUTOMÁTICAMENTE. NO EDITAR A MANO.\n';
salida += '   ------------------------------------------------------------------------\n';
salida += '   Se genera con:  node firmware/esp32cam/webapp/build-webapp.js\n';
salida += '   Edita los archivos en firmware/esp32cam/webapp/ y vuelve a ejecutar\n';
salida += '   el script. El resultado está versionado para poder compilar el\n';
salida += '   firmware sin necesidad de Node.js.\n';
salida += '   ========================================================================== */\n';
salida += '\n';
salida += '#pragma once\n';
salida += '#include <pgmspace.h>\n';
salida += '\n';

let total = 0;

for (const archivo of archivos) {
  const ruta = path.join(dirWebapp, archivo.entrada);
  if (!fs.existsSync(ruta)) {
    console.error('FALTA: ' + ruta);
    process.exit(1);
  }
  const contenido = fs.readFileSync(ruta, 'utf8');
  total += contenido.length;

  const tam = Buffer.byteLength(contenido, 'utf8');
  salida += '// ' + archivo.entrada + '  (' + tam + ' bytes)\n';
  salida += 'static const char ' + archivo.simbolo + '[] PROGMEM =\n';
  salida += literalSeguro(contenido);
  salida += ';\n';
  salida += 'static const size_t ' + archivo.simbolo + '_LEN = sizeof(' + archivo.simbolo + ') - 1;\n';
  salida += '\n';
}

salida += '// Tipo MIME de cada recurso (para las cabeceras Content-Type).\n';
for (const archivo of archivos) {
  salida += '// ' + archivo.entrada + ' -> ' + archivo.tipo + '\n';
}
salida += '\n';

fs.writeFileSync(destino, salida, 'utf8');

console.log('OK  ' + destino);
for (const archivo of archivos) {
  const tam = fs.statSync(path.join(dirWebapp, archivo.entrada)).size;
  console.log('    ' + archivo.entrada.padEnd(12) + String(tam).padStart(7) + ' bytes');
}
console.log('    ' + 'TOTAL'.padEnd(12) + String(total).padStart(7) + ' bytes en flash');
