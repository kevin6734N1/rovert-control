package com.rovert.rovercontrol.input

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

/**
 * Vista que captura los eventos del mando.
 *
 * Android envía los eventos de joystick y de teclas a la vista que tiene el
 * foco, por lo que esta vista se enfoca al adjuntarse al árbol y reenvía los
 * eventos a [GamepadController].
 */
class GamepadCaptureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    /** Recipiente de los eventos de tecla del mando. */
    var onGamepadKey: ((KeyEvent) -> Boolean)? = null

    /** Recipiente de los eventos de movimiento (ejes) del mando. */
    var onGamepadMotion: ((MotionEvent) -> Boolean)? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = false
        contentDescription = context.getString(com.rovert.rovercontrol.R.string.gamepad_capture)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        requestFocus()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = onGamepadKey?.invoke(event) == true
        return handled || super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val handled = onGamepadMotion?.invoke(event) == true
        return handled || super.onGenericMotionEvent(event)
    }
}
