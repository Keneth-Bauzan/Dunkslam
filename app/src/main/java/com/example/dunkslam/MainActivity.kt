package com.example.dunkslam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JuegoDunkCorregido()
        }
    }
}

@Composable
fun JuegoDunkCorregido() {

    val haptic = LocalHapticFeedback.current


    var bolaX by remember { mutableStateOf(500f) }
    var bolaY by remember { mutableStateOf(1400f) }
    var velocidadY by remember { mutableStateOf(0f) }
    var velocidadX by remember { mutableStateOf(0f) }
    var escala by remember { mutableStateOf(1.0f) }
    var rotacion by remember { mutableStateOf(0f) } // NUEVO: Para que la bola gire


    var puntos by remember { mutableStateOf(0) }
    var puntaje by remember { mutableStateOf(0) }
    var fallos by remember { mutableStateOf(0) }
    var velocidadAro by remember { mutableStateOf(8f) }
    val velocidadBase = 8f
    var bolaEnVuelo by remember { mutableStateOf(false) }
    var estaPresionando by remember { mutableStateOf(false) }


    var mostrarTutorial by remember { mutableStateOf(false) }


    val lineaSueloLejos = 900f
    var aroX by remember { mutableStateOf(300f) }
    val aroY = 500f
    var direccionAro by remember { mutableStateOf(1f) }


    val imagenPelota = ImageBitmap.imageResource(id = R.drawable.pelota_baloncesto)
    val imagenCanasta = ImageBitmap.imageResource(id = R.drawable.basket)


    val hitboxAncho = 100f
    val hitboxAlto = 35f
    val offsetXHitbox = 80f
    val offsetYHitbox = 185f


    LaunchedEffect(estaPresionando, bolaEnVuelo) {
        if (!estaPresionando && !bolaEnVuelo) {
            delay(10000)
            mostrarTutorial = true
        } else {
            mostrarTutorial = false
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)

            if (bolaEnVuelo && !estaPresionando) {
                velocidadY += 1.8f
                bolaY += velocidadY
                bolaX += velocidadX

                // NUEVO: La bola gira según su velocidad X
                rotacion += velocidadX * 2f

                if (velocidadY < 0) {
                    escala = Math.max(0.4f, escala - 0.025f)
                }

                val hitboxRect = Rect(
                    left = aroX + offsetXHitbox,
                    top = aroY + offsetYHitbox,
                    right = aroX + offsetXHitbox + hitboxAncho,
                    bottom = aroY + offsetYHitbox + hitboxAlto
                )

                if (escala < 0.65f && velocidadY > 0) {
                    if (hitboxRect.contains(Offset(bolaX, bolaY))) {
                        // --- ENCESTE ---
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress) // VIBRACIÓN
                        puntos++
                        puntaje++
                        velocidadAro += 0.5f
                        bolaEnVuelo = false
                        bolaY = 1400f
                        bolaX = 500f
                        escala = 1.0f
                        velocidadY = 0f
                        velocidadX = 0f
                        rotacion = 0f
                    }
                }

                if (bolaEnVuelo && escala < 0.75f && bolaY > lineaSueloLejos) {
                    // --- FALLO ---
                    puntos = 0
                    fallos++
                    velocidadAro = velocidadBase
                    bolaEnVuelo = false
                    bolaY = 1400f
                    bolaX = 500f
                    escala = 1.0f
                    velocidadY = 0f
                    velocidadX = 0f
                    rotacion = 0f
                }

                if (bolaY > 2200f) {
                    bolaEnVuelo = false
                    bolaY = 1400f
                    bolaX = 500f
                    escala = 1.0f
                    rotacion = 0f
                }
            }

            aroX += velocidadAro * direccionAro
            if (aroX > 800f || aroX < 100f) direccionAro *= -1
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { if (!bolaEnVuelo) estaPresionando = true },
            onDragEnd = {
                if (estaPresionando) {
                    estaPresionando = false
                    bolaEnVuelo = true
                    velocidadY = -48f
                    velocidadX = (bolaX - 500f) / 8f
                }
            },
            onDrag = { _, dragAmount ->
                if (estaPresionando) {
                    bolaX += dragAmount.x
                    bolaY += dragAmount.y
                }
            }
        )
    }) {

        drawRect(color = Color(0xFF121212), size = size)
        drawRect(color = Color(0xFF252525), topLeft = Offset(0f, lineaSueloLejos), size = Size(size.width, size.height - lineaSueloLejos))


        withTransform({ translate(left = aroX, top = aroY) }) {
            drawImage(image = imagenCanasta, dstSize = IntSize(260, 290))
        }


        val tamPelota = 270f
        withTransform({
            scale(escala, escala, pivot = Offset(bolaX, bolaY))
            rotate(rotacion, pivot = Offset(bolaX, bolaY)) // NUEVO: Aplicar rotación
            translate(bolaX - tamPelota/2, bolaY - tamPelota/2)
        }) {
            drawImage(image = imagenPelota, dstSize = IntSize(tamPelota.toInt(), tamPelota.toInt()), alpha = if (estaPresionando) 0.7f else 1f)
        }


        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                textSize = 60f
                isFakeBoldText = true
            }

            if (puntos >= 5) {
                paint.color = android.graphics.Color.RED
                paint.setShadowLayer(30f, 0f, 0f, android.graphics.Color.YELLOW)
                drawText("🔥 EN FUEGO: $puntos", 50f, 150f, paint)
            } else {
                paint.color = android.graphics.Color.WHITE
                paint.clearShadowLayer()
                drawText("Racha: $puntos", 50f, 150f, paint)
            }

            paint.color = android.graphics.Color.WHITE
            paint.clearShadowLayer()
            paint.textSize = 45f
            drawText("Acertados: $puntaje", 50f, 230f, paint)
            drawText("Fallados: $fallos", 50f, 300f, paint)

            if (mostrarTutorial) {
                paint.textAlign = android.graphics.Paint.Align.CENTER
                paint.alpha = 160
                drawText("↑ ARRASTRA PARA LANZAR ↑", bolaX, bolaY - 180f, paint)
            }
        }
    }
}