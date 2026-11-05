# Plan de pruebas — Fabián y Rodrigo

> Plan de trabajo en **dos sesiones** para que Fabián y Rodrigo monten, cableen,
> programen y prueben **su propio rover** con el hardware que cada uno trae.
> Cada sesión tiene una puerta de salida (criterio de aceptación) que hay que
> superar antes de dar la sesión por terminada. Al final hay una tabla de
> riesgos y qué hacer si el tiempo se queda corto.

---

## 1. Supuestos (léelos antes de empezar)

Este plan **da por hecho** lo siguiente. Si alguno no se cumple, dilo en la
primera sesión: el plan hay que ajustarlo, no improvisarlo.

1. **Cada uno tiene su propio material**, no comparten rover:
   - un **iPhone o iPad** (iOS 16.4 o superior si queréis usar el mando DualShock 4;
     con iOS anterior, la web app funciona igual pero **solo con los botones
     táctiles**). El control se hace con el **navegador**: no hay nada que
     instalar, se abre `http://192.168.4.1` (ver `Docs/13`);
   - el **chasis del rover ya montado** con sus ruedas (o con las piezas para
     montarlo);
   - **un L298N**;
   - **dos motores TT** con reductora;
   - **cables jumper** (macho-macho y macho-hembra);
   - un **cable USB de datos** (micro-USB para la ESP32 y USB-B para el Arduino).
2. Cada uno trae además, de su cuenta: portapilas, pilas, convertidor a 5 V,
   resistencias (1 kΩ, 2 kΩ, 220 Ω), LED, y herramientas (multímetro,
   pelacables, destornillador).
3. El **objetivo no es tener un rover**: es que **los dos sepáis montarlo,
   cablearlo, flashearlo y probarlo de forma independiente**, sin depender de
   la otra persona para explicarle algo.
4. Las **pilas y los motores se compran por separado** y se reparten a partes
   iguales.
5. El **firmware ya está escrito y es el del repositorio**: nadie programa la
   lógica del proyecto. El trabajo es cablear, flashear, ajustar polaridades y
   verificar.
6. Las sesiones duran **4 horas** cada una, con un descanso de 15 minutos. Si
   vuestras sesiones son más cortas, aplicad el plan B del §8.
7. **Antes de la Sesión 1, apagad los datos móviles** en el iPhone/iPad y
   comprobad que os conectáis a `Rovert-Cam`. Es el ajuste que más problemas
   causa si se deja para el día de la prueba.

---

## 2. Tarea previa (en casa, antes de la Sesión 1)

Esta tarea se hace **en casa** porque dos de sus partes tardan mucho y bloquearían
la sesión. **Es condición para empezar la Sesión 1.**

| # | Tarea | Tiempo aprox. | Responsable |
|---|---|---|---|
| 1 | Instalar **Arduino IDE 2.x** | 10 min | Cada uno por su lado |
| 2 | Instalar el **core de Espressif** (URL: `https://espressif.github.io/arduino-esp32/package_esp32_index.json`, versión 3.x estable) | 20–40 min (descarga de ~300 MB) | Cada uno por su lado |
| 3 | Copiar las **dos librerías** `ESP_Async_WebServer` (3.12.1) y `Async_TCP` (3.5.0) de `firmware/esp32cam/libraries/` a `C:\Users\<usuario>\Documents\Arduino\libraries` | 10 min | Cada uno por su lado |
| 4 | **Probar la web app en el móvil**: **apagar los datos móviles**, conectarse al Wi-Fi `Rovert-Cam` y abrir `http://192.168.4.1` en Safari. Debe verse la interfaz con el vídeo (guía: `Docs/13`) | 15 min | Cada uno por su lado |
| 5 | Comprobar la **versión de iOS** (16.4+ si queréis mando) y tener un **cable USB de datos** a mano | 5 min | Cada uno por su lado |
| 6 | **Instalar el driver CH340** (si tu placa lo lleva) y comprobar que el Arduino aparece como `COMx` en el Administrador de dispositivos | 10 min | Cada uno por su lado |
| 7 | Leer los tres documentos clave: `Docs/03-Tabla-de-Conexiones-y-Cableado.md`, `Docs/06-Instrucciones-Flash-ESP32.md`, `Docs/07-Instrucciones-Flash-Arduino.md` | 30 min | Los dos |

> Si alguien llega a la Sesión 1 sin la tarea previa hecha, pierde la primera
> parte de la sesión. **Avisa el día antes.**

---

## 3. Reglas de trabajo en las sesiones

- **Ruedas en el aire** siempre que se prueben motores. Sin excepciones.
- **Uno programa, el otro verifica.** En cada compuerta, quien ha hecho el
  trabajo **no se firma su propia revisión**: la firma el otro. Es la mejor
  forma de detectar un cable mal puesto.
- **Nada de flashear con el rover en el suelo** ni con una orden activa.
- **Si un paso no se entiende, se para y se pregunta.** Es más rápido que
  seguir con un error que se propaga.
- Se trabaja con los documentos delante (`03`, `06`, `07`), no de memoria.

---

## 4. Sesión 1 — Montaje y cableado (4 h)

| Hora | Bloque | Fabián (línea 1) | Rodrigo (línea 2) | Puerta del bloque |
|---|---|---|---|---|
| 0:00–0:15 | **Inventario de material** | Cuenta y verifica su kit contra la *lista de materiales* del doc 03 | Cuenta y verifica su kit | Los dos kits completos. **Si falta algo, se pide ese mismo día**: no se sigue |
| 0:15–0:50 | **Montaje mecánico** | Fija los motores TT al chasis y monta la rueda izquierda | Fija los motores TT y monta la rueda derecha | Ruedas montadas, libres y sin rozar el chasis |
| 0:50–1:10 | **Alimentación** | Conecta el pack de 4× AA al convertidor buck/boost y comprueba la salida de 5 V con el polímetro | Conecta el pack de 18650 al terminal `+12V (Vs)` del L298N y el GND | 5 V medidos en la salida del convertidor; 7,4 V en `+12V` del L298N |
| 1:10–1:50 | **Cableado de señal** | Cablea **ESP32 ↔ Arduino**: GPIO32 → D10 directo, GPIO33 → [1 kΩ] → D9, y 2 kΩ de GPIO33 a GND | Cablea **Arduino → L298N**: D3→ENA, D6→ENB, D8→IN1, D7→IN2, D12→IN3, D11→IN4, D2→LED con 220 Ω | Todas las señales conectadas según las tablas del doc 03 |
| 1:50–2:20 | **Masa común y verificación eléctrica** | Une los tres GND (ESP32, Arduino, L298N) y mide continuidad | Mide el divisor de tensión y comprueba que **no hay 5 V en GPIO32/GPIO33** | Checklist §9 del doc 03 superada. **La firma el otro** |
| 2:20–2:35 | **Descanso** | — | — | — |
| 2:35–3:20 | **Coser el chasis y revisar el cableado** | Fija la ESP32, el Arduino y el L298N al chasis; comprueba que ningún cable pelado toca el chasis | Revisa el cableado de su kit contra las tablas del doc 03, punto por punto | Montaje terminado y ordenado; fotos del rover de cada uno |
| 3:20–4:00 | **Prueba en frío de la alimentación** (sin programar todavía) | Con las pilas puestas: comprueba que el LED de la ESP32 se enciende y que el LED frontal del Arduino se enciende | Vigila que la ESP32 **no se reinicie** (brownout) al conectar el convertidor | Ambos montajes enciende y se mantienen estables |

### Puerta de salida de la Sesión 1

- [ ] Los **dos** montajes están completos y el cableado coincide con las
      tablas del doc 03.
- [ ] La **checklist de verificación** (§9 del doc 03) está superada en los dos
      kits: masa común, divisor a 3,3 V, ningún 5 V en la ESP32, motores
      alimentados desde el L298N.
- [ ] Cada montaje ha sido **revisado y firmado por el otro compañero**.
- [ ] Los dos rover están **en pie, con las ruedas libres**, y aún **no** se
      han flasheado.

---

## 5. Sesión 2 — Programación y pruebas (4 h)

| Hora | Bloque | Fabián | Rodrigo | Puerta del bloque |
|---|---|---|---|---|
| 0:00–0:20 | **Revisión cruzada del montaje** | Presenta su montaje al otro y responde a sus preguntas (5 min cada uno) | Igual, en el otro orden | Los dos montajes revisados y sin sorpresas |
| 0:20–1:00 | **Flasheo del Arduino UNO** | Flashea **primero** su Arduino y deja el Monitor Serie visible a 115200 | Flashea su Arduino siguiendo la referencia | Ambos imprimen `[ROVERT] Controlador de motores listo (motores detenidos)` |
| 1:00–1:50 | **Flasheo de la ESP32** | Flashea su ESP32 con la configuración del doc 06 (placa, PSRAM, partición) | Flashea su ESP32 | Ambos imprimen el banner, `PSRAM detectada` y `Camara OV2640 inicializada OK` |
| 1:50–2:20 | **Prueba de los motores con las ruedas en el aire** | Comprueba que los dos motores giran hacia delante al pedir "Adelante"; si uno va al revés, cambia **una** constante y reflashea | Hace lo mismo en su kit | Los dos motores giran en el mismo sentido. **Si hay que corregir polaridad, se hace aquí** |
| 2:20–2:35 | **Descanso** | — | — | — |
| 2:35–3:00 | **Prueba del enlace UART** | Mira las tramas `R,…` en el Monitor Serie del Arduino mientras la app manda órdenes | Igual | Se ven tramas `R,<izq>,<der>,<seq>` con valores coherentes |
| 3:00–3:10 | **Prueba del watchdog** | Con el rover en marcha, desconecta el cable D9/D10 y cronometra la parada | Repite la prueba en su kit | Los motores paran en **menos de 400 ms** en los dos kits |
| 3:10–3:40 | **Pruebas de control (web app)** | Con los datos móviles apagados, conéctate al Wi-Fi `Rovert-Cam`, abre `http://192.168.4.1` en Safari y prueba vídeo, cruceta, STOP y (si es iOS 16.4+) el mando | Igual | Web app *Conectado*, vídeo visible, rover obedece y el STOP lo para en el acto |
| 3:40–4:00 | **Pruebas de aceptación y acta** | Ejecuta y anota las RF-01…RF-32 de `Docs/08-Pruebas-de-Aceptacion.md` (las RF-26…RF-32 son de la web app) | Idem, cada uno las suyas | Las 32 pruebas de cada kit superadas o documentadas las incidencias |

### Puerta de salida de la Sesión 2

- [ ] Los dos firmwares flasheados y verificados por el Monitor Serie.
- [ ] Los dos rover se mueven, giran y se paran correctamente.
- [ ] Las **pruebas de seguridad** (RF-18 desconexión, RF-19 segundo plano,
      RF-24 watchdog del Arduino) superadas en los dos kits.
- [ ] La tabla RF-01…RF-32 rellena y firmada en los dos kits
      (RF-26…RF-32: web app en el móvil).
- [ ] **Acta de la sesión** firmada por los dos (abajo).

---

## 6. Qué debe traer cada uno

| Material | Fabián | Rodrigo |
|---|---|---|
| Teléfono o tablet iOS (iPhone/iPad) con **datos móviles apagados** y Safari. No hace falta instalar nada: la ESP32 sirve la web app en `http://192.168.4.1` | ✔ | ✔ |
| Portátil con Arduino IDE 2.x, core esp32 3.x y las dos librerías instaladas (tarea previa) | ✔ | ✔ |
| Chasis del rover con ruedas | ✔ | ✔ |
| Módulo L298N (con sus jumpers) | ✔ | ✔ |
| Dos motores TT con reductora | ✔ | ✔ |
| Portapilas de 4× AA + 4 pilas AA | ✔ | ✔ |
| Portapilas de 18650 + 2 pilas 18650 (o 4, para dos portapilas) | ✔ | ✔ |
| Convertidor buck/boost a 5 V, ≥ 1 A | ✔ | ✔ |
| Resistencias 1 kΩ, 2 kΩ y 220 Ω | ✔ | ✔ |
| LED de 3 mm | ✔ | ✔ |
| Jumpers macho-macho (≈20) y macho-hembra (≈6) | ✔ | ✔ |
| Cable USB **de datos** micro-USB (ESP32) | ✔ | ✔ |
| Cable USB **de datos** USB-B (Arduino) | ✔ | ✔ |
| Multímetro (muy recomendable), alicates, pelacables, destornillador | ✔ | ✔ |
| Tornillería M3 para fijar las placas al chasis | ✔ | ✔ |

> **No es opcional:** un cable USB **de datos**. Uno solo de carga no permite
> programar y es la causa número uno de "no me aparece el puerto".

---

## 7. Tabla de riesgos

| # | Riesgo | Probabilidad | Impacto | Mitigación | Plan B |
|---|---|---|---|---|---|
| R1 | El core de Espressif no baja (300 MB) o no hay Internet en casa | Media | Alto | Instalar el core en la **tarea previa**; llevar el IDE 2.x instalado en un pendrive | Flashear el Arduino en la Sesión 1 y la ESP32 en la Sesión 3 |
| R2 | Falta material (un jumper, un portapilas, las resistencias) | Alta | Alto | **Inventario en los primeros 15 min** de la Sesión 1 contra la lista del doc 03; lo que falte se pide ese mismo día | Pedirlo al profesor y montar solo la parte de señal (no se puede probar el UART sin el otro lado) |
| R3 | Descuido: divisor de tensión mal puesto o masa común sin unir | Media | **Crítico** | Checklist §9 del doc 03 con polímetro **antes** de flashear; la firma el otro compañero | Rehacer el cableado del enlace UART (30 min) |
| R4 | Brownout: la ESP32 se reinicia al arrancar los motores | Media | Medio | Convertidor ≥ 1 A, condensador de 470–1000 µF en el 5 V de la ESP32, motores alimentados desde las 18650 | Bajar el límite de velocidad al 50 %; probar sin motores conectados |
| R5 | El Arduino o la ESP32 no aparecen en el puerto USB | Media | Medio | Driver CH340 instalado en la tarea previa; **cable de datos**; llevar un cable de repuesto | Probarlos en el portátil del otro compañero |
| R6 | Alguien cambia los pines de la cámara por los de AI-Thinker | Media | Medio | No tocar `config.h`; si la cámara falla, **restaurar el fichero del repositorio** | `git checkout` del fichero (o volver a descargarlo) y reflashear |
| R7 | El rover se mueve solo con una orden activa (pérdida de Wi-Fi) | Baja | **Crítico** | Ruedas en el aire en todas las pruebas; STOP a mano; nunca flashear con el rover en el suelo | Desconectar la batería de los motores mientras se depura |
| R8 | Los dos se lían trabajando en el mismo rover y los dos kits quedan a medias | Media | Medio | Cada uno trabaja **en su kit y con su ordenador**; el rover de la otra persona solo se toca con permiso | Reunión de 10 min para repartirse las tareas pendientes |
| R9 | Se acaba el tiempo de la sesión | Alta | Medio | Prioridades claras: **seguridad > funcionamiento > vídeo > estética** (§8) | Quedan pendientes para una Sesión 3 |
| R10 | El móvil se queda sin batería a mitad de la sesión | Media | Bajo | Cargar el móvil al 100 % antes; usar el USB como cargador | El Arduino y la ESP32 se prueban sin la app (tramas por UART) |
| R11 | Pilas agotadas a mitad de la prueba | Media | Medio | Pilas de repuesto; medir tensión antes de empezar | Cambiar el pack y repetir solo las pruebas de motor |

---

## 8. Plan B: si os falta tiempo

**Orden de prioridad innegociable.** Si hay que recortar, se recorta por este
orden (de abajo hacia arriba se queda):

1. **Decoración, riesgo y componentes personalizados.** Nada de esto cuenta.
2. **Pruebas de streaming de vídeo con resolución alta.** VGA (640×480) ya está
   bien: es el valor por defecto y no hay que cambiar nada.
3. **Emparejar el mando.** Es opcional: el D-pad en pantalla cubre el control.
4. **Ajuste fino de la velocidad.** Se puede dejar al 100 %.

**Nunca se recorta esto:**

- La **checklist de verificación eléctrica** (doc 03 §9).
- Las **pruebas de seguridad** RF-18, RF-19 y RF-24.
- La **polaridad de los motores** con las ruedas en el aire.

Si al final de la Sesión 2 faltan pruebas, se documentan como **pendientes** en
la tabla de incidencias del doc 08 y se cierran en una Sesión 3 breve (90 min)
centrada solo en eso.

---

## 9. Acta de las sesiones

### Sesión 1 — Montaje y cableado

| | Nombre | Fecha | Firma |
|---|---|---|---|
| Montaje y cableado | Fabián | ____/____/______ | ______________________ |
| Revisión del montaje | Rodrigo | ____/____/______ | ______________________ |

**Materiales pendientes de conseguir:** _________________________________________

### Sesión 2 — Programación y pruebas

| | Nombre | Fecha | Firma |
|---|---|---|---|
| Programación y pruebas | Fabián | ____/____/______ | ______________________ |
| Revisión y firmas de las RF | Rodrigo | ____/____/______ | ______________________ |

**Incidencias abiertas (RF fallidas):** ______________________________________

**Acuerdo sobre la demostración final:** _______________________
(por ejemplo: quién maneja el rover y quién explica, día y hora)

---

## Si algo falla

Este plan está pensado para que las incidencias se resuelvan **dentro** de la
sesión. Cuando una puerta de salida no se puede superar, no se sigue
avanzando a ciegas: busca el síntoma en **`Docs/09-FAQ-y-Problemas.md`** (22
entradas con diagnóstico paso a paso) y, si sigue sin resolverse, anótalo en el
acta de arriba y llévalo a la siguiente sesión como punto de partida.
