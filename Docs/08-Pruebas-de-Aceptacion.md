# Pruebas de aceptación del rover Rovert

> Esta es la lista que hay que pasar para dar el proyecto por bueno. Cada fila
> es un requisito con un identificador (**RF-01…RF-25**), cómo se comprueba a
> mano y qué tiene que verse exactamente. Cuando la tabla esté rellena, el
> rover está listo para la demostración.

---

## Cómo ejecutar cada prueba

### Preparación (hazla una vez antes de empezar)

1. Rover **montado y cableado** según `Docs/03-Tabla-de-Conexiones-y-Cableado.md`,
   con la **checklist de verificación** de ese documento superada.
2. Firmwares flasheados: ESP32 (`Docs/06-Instrucciones-Flash-ESP32.md`) y
   Arduino (`Docs/07-Instrucciones-Flash-Arduino.md`).
3. App instalada y funcionando (`Docs/02-Guia-de-Instalacion-APK.md`).
4. Teléfono conectado a la red **`Rovert-Cam`** y con la app en *Conectado*.
5. **Ruedas en el aire** salvo que la prueba diga lo contrario.
6. A mano: un **cronómetro** (el del móvil sirve) y, si es posible, un
   **multímetro** para las pruebas eléctricas.

### Reglas durante las pruebas

- **Una prueba a la vez.** Anota el resultado en la columna ✓/✗ en el momento,
  no al final de memoria.
- Si una prueba falla, **para la secuencia**, anota el síntoma y mira
  `Docs/09-FAQ-y-Problemas.md`. No sigas a la siguiente con un fallo abierto:
  casi todos los fallos se propagan.
- Las pruebas que implica **desconectar el UART** (RF-23) necesitan un
  destornillador o unos alicates para sacar el cable de D10/D9. Tenlo a mano.
- Nunca dejes el rover en el suelo con una orden activa: haz RF-07…RF-15 con
  las ruedas suspendidas.

### Cómo medir los tiempos

Para comprobar que el rover se detiene en menos de 400 ms:

1. Graba con la cámara del móvil la zona donde se ve la rueda (o la propia app).
2. Provoca la desconexión mientras grabas.
3. Cuenta los fotogramas entre el corte y la parada. A 30 fps, **400 ms ≈ 12
   fotogramas**.

---

## Tabla de pruebas

| ID | Requisito | Cómo se prueba | Resultado esperado | ✓/✗ | Firma |
|---|---|---|---|---|---|
| **RF-01** | La app se instala en el teléfono | Descargar `Rovert-Control-v1.0.0.apk`, permitir "Instalar apps desconocidas" e instalarlo | La instalación termina sin errores y aparece el icono **Rovert Control** en el cajón de apps | | |
| **RF-02** | La app se abre y no se cierra sola | Abrir la app y dejarla en pantalla 2 minutos | Permanece abierta, sin cierres inesperados | | |
| **RF-03** | El móvil se conecta a la red del rover | Ajustes → Wi-Fi → elegir `Rovert-Cam` con la contraseña `rovert2026` | El móvil se conecta; Android avisa de "sin Internet" y se elige **Mantener conectado** | | |
| **RF-04** | La app conecta con el rover | Abrir la app con el rover encendido | La barra superior pasa a **Conectado** | | |
| **RF-05** | La IP del rover es la correcta | Navegador → `http://192.168.4.1/` | Responde con el JSON de diagnóstico (no con un error de página) | | |
| **RF-06** | El vídeo de la cámara se ve en la app | Mirar el panel de vídeo de la app | Se ve la imagen de la cámara, no un cuadro negro ni un error | | |
| **RF-07** | El vídeo es fluido | Observar el contador **FPS** de la barra superior durante 10 s | **10–15 FPS** (por debajo de 5 FPS hay que revisar alimentación o PSRAM) | | |
| **RF-08** | El D-pad hace avanzar el rover | Mantener pulsado **▲** (ruedas en el aire) | Los dos motores giran **hacia delante** | | |
| **RF-09** | El D-pad hace retroceder al rover | Mantener pulsado **▼** | Los dos motores giran **hacia atrás** | | |
| **RF-10** | El D-pad hace girar a la izquierda | Mantener pulsado **◀** | El rover gira **sobre el sitio** hacia la izquierda (un motor avanza, el otro retrocede) | | |
| **RF-11** | El D-pad hace girar a la derecha | Mantener pulsado **▶** | El rover gira **sobre el sitio** hacia la derecha | | |
| **RF-12** | El control es *hold-to-move* | Pulsar **▲** y **soltar** | El rover se para en cuanto se suelta el botón | | |
| **RF-13** | El botón STOP detiene al instante | Con el rover en marcha, pulsar **STOP** (rojo) | Los motores se paran en el acto; en la barra de arriba el estado pasa a *Detenido* | | |
| **RF-14** | STOP tiene prioridad | Con el rover en marcha por el D-pad, pulsar **STOP** | Se detiene; no queda ninguna orden activa pendiente | | |
| **RF-15** | El mando se empareja con el teléfono | Ajustes → Bluetooth → emparejar el DualShock 4 (barra de luz en blanco) | En la app aparece **"Mando: \<nombre del mando\>"**, no "Mando: ninguno" | | |
| **RF-16** | El mando controla el rover | Modo **MANDO**, stick izquierdo hacia arriba / abajo / lados | El rover avanza, retrocede y gira igual que con el D-pad | | |
| **RF-17** | El mando se desconecta y el rover para | Con el rover en marcha, apagar el mando | La app detecta la pérdida del mando, manda el `stop` y el rover se detiene | | |
| **RF-18** | **Al perder la conexión, el rover se detiene en ≤ 400 ms** | Con el rover en marcha, **desconectar el Wi-Fi del móvil** (o apagar el router) y cronometrar hasta la parada | El rover se detiene en **menos de 400 ms** (en la práctica antes: la app y la ESP32 avisan al Arduino). Si no se para, hay un fallo crítico de seguridad | | |
| **RF-19** | **Poner la app en segundo plano detiene el rover** | Con el rover en marcha, pulsar el botón **Inicio** del móvil (o bloquear la pantalla) | El rover se detiene inmediatamente | | |
| **RF-20** | El límite de velocidad funciona | Ajustes → **Velocidad máxima** = 50 %, y comparar con 100 % | El rover gira más despacio al 50 % que al 100 %; los motores no giran al máximo | | |
| **RF-21** | La inversión de polaridad funciona (en la app) | Ajustes → **Invertir motor izquierdo** y hacer avanzar el rover | El rover **gira sobre el sitio** (o se va de lado) en lugar de avanzar recto; al desactivar la casilla, vuelve a avanzar recto | | |
| **RF-22** | La polaridad del firmware funciona | Con las ruedas en el aire, avanzar y comprobar que **ambos motores giran en el mismo sentido**; si uno va al revés, cambiar `LEFT_MOTOR_INVERT` / `RIGHT_MOTOR_INVERT` y reflashear | Los dos motores giran hacia delante a la vez | | |
| **RF-23** | El diagnóstico por HTTP devuelve JSON | Navegador (o `curl`) → `http://192.168.4.1/` | Un JSON con `"device":"Rovert ESP32-CAM"`, `"fw":"1.0.0"`, `"ip":"192.168.4.1"`, `"camera":true` | | |
| **RF-24** | El watchdog del Arduino detiene los motores si se corta el UART | Con el rover en marcha, **desconectar el cable de datos entre la ESP32 y el Arduino** (D9/D10) | Los motores se paran en **≤ 400 ms**; en el UART aparece la trama `W,<seq>` y en el USB del UNO el mensaje `[ROVERT] WATCHDOG: motores detenidos por falta de tramas` | | |
| **RF-25** | Ningún firmware arranca con los motores en marcha | Con el rover en el suelo, **desconectar y volver a conectar la alimentación** de la ESP32 y del Arduino | En ambos arranques los motores quedan **detenidos**; el LED frontal se enciende fijo | | |

---

## Resumen de la tanda

| Resultado | Cantidad |
|---|---|
| Pruebas superadas (✓) | |
| Pruebas fallidas (✗) | |
| Pruebas no ejecutadas | |
| **Total** | **25** |

**Criterio de aceptación:** las 25 pruebas superadas. Las pruebas críticas de
seguridad son **RF-18**, **RF-19** y **RF-24**: si alguna falla, el rover **no**
se usa en la demostración ni en la exposición, con independencia del resultado de las
demás.

### Registro de incidencias

| ID fallida | Síntoma observado | Causa probable | Acción correctiva | Rehecha (✓) |
|---|---|---|---|---|
| | | | | |
| | | | | |
| | | | | |

---

## Si algo falla

Cada fallo de esta tabla tiene su entrada en **`Docs/09-FAQ-y-Problemas.md`**
con el diagnóstico paso a paso. Empieza por la sección del síntoma que
corresponda: es más rápido que improvisar cableado a ciegas.
