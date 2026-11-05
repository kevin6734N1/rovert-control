# Guía de instalación de la app Rovert Control (APK)

> Esta guía explica, paso a paso, cómo instalar en tu teléfono Android la app
> **Rovert Control**, conectarla a la red Wi-Fi que crea el rover y empezar a
> controlarlo. Está pensada para quien nunca ha instalado un APK a mano
> (Fabián, Rodrigo): sigue los pasos en orden y no hace falta saber nada
> especial de Android.

---

## 1. Qué necesitas

| Elemento | Detalle |
|---|---|
| Teléfono Android | Versión **7.0 (Nougat) o superior** |
| APK de la app | `Rovert-Control-v1.0.0.apk` (unos pocos MB) |
| Red Wi-Fi | La que crea el rover: **`Rovert-Cam`** / contraseña **`rovert2026`** |
| Mando (opcional) | Un **DualShock 4** (PS4) emparejado **con el teléfono**, no con el rover |
| Conexión a Internet | Solo para **descargar** el APK. Para usar el rover **no** hace falta |

> La app **no necesita Internet**. El rover crea su propia red Wi-Fi y la app
> habla con él dentro de esa red. Si el móvil te avisa de que esa red "no tiene
> Internet", es normal (te lo explicamos en el paso 7).

---

## 2. Descargar el APK

1. Abre el navegador del teléfono (Chrome, Samsung Internet, Firefox…).
2. Entra en el repositorio del proyecto:
   **`https://github.com/kevin6734N1/rovert-control`**
3. Ve a la pestaña **Releases** (es la segunda, a la derecha de *Code*).
4. Baja hasta el bloque de la última versión publicada y descarga el fichero
   **`Rovert-Control-v1.0.0.apk`**.
   - Si el botón dice *Source code (zip)* en lugar de *Assets*, estás en la
     lista de commits: sube a la pestaña **Releases**.
5. Android te preguntará si quieres abrir el fichero con el instalador de apps.
   Continúa cuando llegue el paso 4.

> También puedes descargar el APK en el ordenador y pasarlo al móvil por cable
> USB, correo o Bluetooth. Da igual cómo llegue al teléfono: lo que importa es
> que termine en la carpeta **Descargas**.

### 2.1 Alternativa: Google Drive

El APK también está subido a la carpeta de Google Drive del proyecto
(**Taller de Robótica**), por si el acceso a GitHub desde el móvil da problemas:

**`https://drive.google.com/open?id=1bEzPmtJGLL9wSy7pMydhVEz8jJ0VDAkB`**

Ábrelo en el navegador del móvil, pulsa el botón de descarga y acepta el
"instalar apps desconocidas" que pido Android.


---

## 3. Comprobar la versión de Android

Antes de instalar, comprueba que tu teléfono es compatible:

1. Abre **Ajustes** → **Acerca del teléfono** → **Información de software**.
2. Busca la fila **Versión de Android**.
3. Debe decir **7.0** o superior.

| Versión de Android | ¿Funciona la app? |
|---|---|
| 7.0 – 7.1 (Nougat) | Sí, es el mínimo (`minSdk 24`) |
| 8.0 – 13 (Oreo … Tiramisu) | Sí |
| 14 o superior | Sí |
| 6.0 o inferior (Marshmallow) | **No**: la app no se instala |

La razón es técnica: la app usa funciones de red y de Bluetooth disponibles
desde Android 7.0 (`minSdk = 24` en el proyecto).

---

## 4. Permitir la instalación de apps desconocidas

Android bloquea, por seguridad, la instalación de apps que no vienen de Google
Play. Hay que conceder ese permiso **solo** a la app desde la que descargas el
APK (el navegador o el explorador de archivos).

### Android 8.0 o superior

1. **Ajustes** → **Apps** (o *Aplicaciones*) → **Acceso especial**.
   - En algunos modelos: **Ajustes** → **Seguridad y privacidad** → **Acceso especial**.
2. Entra en **Instalar apps desconocidas** (*Install unknown apps*).
3. Elige la app desde la que hiciste la descarga (normalmente **Chrome**, o
   **Archivos** / *Mis archivos* si lo copiaste por USB).
4. Activa el interruptor **Permitir de esta fuente**.
5. Repite los pasos 3–4 si descargaste el APK con **dos** apps distintas.

### Android 7.0 / 7.1

1. **Ajustes** → **Seguridad**.
2. Activa **Fuentes desconocidas** (*Unknown sources*).
3. Si aparece un aviso, pulsa **Aceptar**.

> Permiso puntual y reversible: una vez instalada la app puedes volver a
> desactivar esta opción.

---

## 5. Instalar el APK

1. Abre la carpeta **Descargas** del teléfono (en Samsung: *Mis archivos →
   Descargas*).
2. Toca el fichero **`Rovert-Control-v1.0.0.apk`**.
   - Si lo descargaste con el navegador, ábrelo desde la notificación de
     descarga o desde **Chrome → ⋮ → Descargas**.
3. Android muestra el aviso de seguridad:
   > *"¿Quieres instalar esta app? Esta app procede de fuera de Google Play…"*
   (el texto exacto depende de la versión).
4. Pulsa **Instalar**. Espera a que la barra de progreso llegue al final.
5. Android muestra un aviso de *Play Protect* del tipo *"App no verificada"*.
   Es **normal**: la app no está en Google Play porque es un proyecto
   universitario. Pulsa **Instalar de todos modos** / **Más información →
   Instalar de todos modos**.
6. Cuando termine, aparece el botón **Abrir**. Tócalo.
   - Si no quieres abrirla todavía, puedes hacerlo más tarde desde el cajón de
     apps buscando **Rovert Control**.

### Comprobación rápida de que la instalación fue bien

- En el cajón de aplicaciones (o en *Apps recientes*) aparece el icono
  **Rovert Control**.
- Al abrirla, la barra superior muestra **Sin conexión** hasta que te conectes
  a la red del rover. Es lo normal en el primer arranque.

---

## 6. Permisos que pide la app (y por qué)

| Permiso | Para qué lo necesita |
|---|---|
| **INTERNET** | La app abre el WebSocket de control (`ws://192.168.4.1:80/control`) y descarga el vídeo MJPEG (`http://192.168.4.1:80/stream`). Sin este permiso no hay forma de hablar con el rover. |
| **ACCESS_NETWORK_STATE** | La app consulta el estado de la red para detectar si el móvil está en la Wi-Fi `Rovert-Cam` y para distinguir "sin cobertura" de "rover apagado" en los mensajes de error. |

Además, el permiso de **cámara NO se pide**: la app no usa la cámara del
teléfono, solo reproduce el vídeo que envía la OV2640 del rover.

> **Tráfico HTTP sin cifrar (cleartext).** El rover sirve HTTP plano dentro de su
> propia red, sin HTTPS. El proyecto lo declara de forma explícita en
> `android:usesCleartextTraffic="true"` y en `network_security_config.xml`.
> Es aceptable porque el tráfico nunca sale de la red Wi-Fi cerrada del rover y
> no contiene datos personales.

---

## 7. Conectarse a la red Wi-Fi del rover

Este paso es el que más confunde la primera vez, así que léelo entero antes de
quejarse de que la app "no conecta".

1. Enciende el rover (el LED de la ESP32 se enciende y en unos segundos se
   crea la red).
2. Abre **Ajustes** → **Wi-Fi**.
3. Busca la red **`Rovert-Cam`** en la lista y púlsala.
4. Escribe la contraseña: **`rovert2026`** (todo en minúsculas) y pulsa
   **Conectar**.
5. Android te va a avisar de algo parecido a esto:
   > *"Esta red no tiene acceso a Internet. ¿Quieres mantener la conexión?"*
   o *"Sin Internet, se te desconectará más adelante"*.
6. **Pulsa «Mantener conectado» / «Mantener la conexión» / «Sí, quiero
   conectarme»**. Si eliges la opción de *desconectarse*, el móvil volverá a
   tirar de los datos móviles y la app no verá al rover.

### Qué cambia al usar esa red

- **El móvil deja de usar datos móviles** mientras esté conectado a
  `Rovert-Cam`: todo el tráfico sale por la red del rover. Es lo esperado.
- Los mensajes de WhatsApp, el correo y el navegador se quedarán sin
  Internet hasta que vuelvas a una red con cobertura.
- Para salir: **Ajustes → Wi-Fi → Rovert-Cam → Olvidar / Desconectar**, y
  vuelve a tu red habitual.

### Comprobación de que la red funciona

Con el móvil conectado a `Rovert-Cam`, abre el navegador y entra en:

```
http://192.168.4.1/
```

Deberías ver un JSON de diagnóstico parecido a este:

```json
{"device":"Rovert ESP32-CAM","fw":"1.0.0","ssid":"Rovert-Cam","ip":"192.168.4.1",
 "camera":true,"clients":1,"uptimeMs":42813,"state":"stopped","left":0.000,"right":0.000}
```

Si ves ese JSON, la red, el firmware y el vídeo están bien: lo que falla, si
falla algo, es la app.

---

## 8. Emparejar el mando DualShock 4 (opcional)

El mando se empareja **con el teléfono**, no con el rover. El rover no tiene
Bluetooth: la app recibe los botones del mando y los traduce a órdenes que
envía por Wi-Fi.

1. En el DualShock 4, **mantén pulsado el botón PS** (el botón central con el
   logo de PlayStation).
2. Suéltalo cuando la **barra de luz parpadee en blanco rápidamente** (dos
   destellos rápidos): eso significa "buscando dispositivos".
3. En el teléfono: **Ajustes → Conexiones → Bluetooth** (en algunos modelos
   **Ajustes → Bluetooth**) y activa el Bluetooth si lo estaba apagado.
4. Toca **Emparejar dispositivo nuevo** y espera a que aparezca
   **Wireless Controller**.
5. Si te pide un PIN, el del DualShock 4 es **0000** o **1234**.
6. Cuando el emparejamiento termina, la barra de luz del mando queda en
   **blanco fijo** (o azul) y el nombre aparece como *paired / conectado* en la
   lista de ajustes de Bluetooth.
7. Android 10 o superior puede pedir permiso de **ubicación** para "dispositivos
   cercanos": hay que aceptarlo (o permitirlo en *Ajustes → Aplicaciones →
   Rovert Control → Permisos*), si no el teléfono no ve el mando.

> **El mando sigue estando emparejado con el teléfono aunque apagues y
> enciendas el rover.** Solo hay que volver a encenderlo (pulsar el botón PS una
> vez para despertarlo).

---

## 9. Uso básico

### Modo TÁCTIL (por defecto)

- **D-pad en pantalla**: cuatro flechas (▲ ▲ ▼). Es un control *hold-to-move*:
  **mantén pulsado** la flecha para que el rover se mueva y **suelta** para que
  se detenga. La propia app lo recuerda: *"Mantén pulsado para mover el rover.
  Al soltar, se detiene."*
- **Botón rojo STOP**: detiene los motores **de inmediato**, tiene prioridad
  sobre cualquier otra orden. Úsalo si el rover se va hacia donde no quieres.
- **Barra superior**: indica el estado de la conexión (*Conectando…*,
  *Conectado*, *Sin conexión*, *Error de conexión*), los FPS del vídeo, si el
  rover está *Detenido* o *En marcha*, el tiempo de actividad (*uptime*) y la
  orden activa.

### Modo MANDO

- Con el mando emparejado, **mueve el stick**: la app cambia sola de **TÁCTIL**
  a **MANDO** (en la barra de modo verás "MANDO / Control por mando activo").
  También puedes elegir el modo a mano en el selector.
- **Stick izquierdo**: hacia arriba = adelante, hacia abajo = retroceso,
  izquierda/derecha = giro sobre el sitio. El stick aplica *gas + dirección*
  mezclados: en las cuatro posiciones cardinales el resultado es exactamente
  adelante / atrás / giro izq. / giro der.
- **Cruceta del mando**: si el stick está en el centro, la cruceta (hat) actúa
  como respaldo y también mueve el rover.
- **Zona muerta**: por defecto es del **18 %**. Si el rover avanza solo con el
  stick en el centro, sube la zona muerta en *Ajustes* (hasta un 40 %).
- La app avisa con *"Mando: ninguno"* si no hay ningún mando emparejado.

### Apagado ordenado

1. Suelta cualquier control.
2. Pulsa **STOP**.
3. Opcionalmente pulsa **Desconectar**: la app manda un `stop` explícito y
   cierra el WebSocket.
4. Apaga el rover.

---

## 10. Si la app muestra «Sin conexión»

1. **¿Estás en la red correcta?** Revisa el nombre de la red Wi-Fi en la barra
   de notificaciones: debe ser **`Rovert-Cam`**.
2. **¿El rover está encendido y con la ESP32 alimentada?** Sin alimentación
   estable la ESP32 se reinicia en bucle y no crea la red
   (ver `Docs/09-FAQ-y-Problemas.md`).
3. **Prueba la IP en el navegador**: `http://192.168.4.1/`.
   - Si responde, el rover está bien y el problema es la app → sigue en 4.
   - Si no responde, el problema es la red o el firmware → revisa
     `Docs/06-Instrucciones-Flash-ESP32.md`.
4. **Comprueba la IP en los ajustes de la app**: toca el icono de **Ajustes**
   (engranaje) y verifica el campo **IP del rover**. El valor por defecto es
   **`192.168.4.1`** y el puerto **80**. Si lo cambiaste, vuelve a `192.168.4.1`.
5. Pulsa **Conectar** de nuevo. La app reintenta sola con backoff (1 s → 10 s),
   así que a veces basta con esperar unos segundos.

### Referencias útiles

| Qué | Dónde |
|---|---|
| Contrato de mensajes app ↔ rover | `Docs/04-Protocolo-Comandos.md` |
| Tramas UART ESP32 ↔ Arduino | `Docs/05-Protocolo-UART-ESP32-Arduino.md` |
| Cableado completo | `Docs/03-Tabla-de-Conexiones-y-Cableado.md` |
| Problemas frecuentes | `Docs/09-FAQ-y-Problemas.md` |

---

## Si algo falla

Esta guía cubre el camino normal. Si algo no cuadra (la app no conecta, el
vídeo va en negro, el mando no aparece, el rover va al revés…), el documento
**`Docs/09-FAQ-y-Problemas.md`** recoge las 20 incidencias más frecuentes con su
diagnóstico y su solución. Échale un vistazo antes de tocar nada: casi siempre
el arreglo está ahí.
