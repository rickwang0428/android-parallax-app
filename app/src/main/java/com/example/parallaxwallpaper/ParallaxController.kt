package com.example.parallaxwallpaper

import android.widget.ImageView

data class LayerConfig(
    val view: ImageView,
    val depthFactor: Float
)

class ParallaxController(
    private val layers: List<LayerConfig>,
    private val movementRangePx: Float
) {

    private var intensity = 0.4f
    private var smoothedX = 0f
    private var smoothedY = 0f

    fun updateIntensity(value: Float) {
        intensity = value.coerceIn(0f, 1f)
    }

    fun onTiltChanged(x: Float, y: Float) {
        // simple low-pass filter for smoother motion
        val alpha = 0.15f
        smoothedX += alpha * (x - smoothedX)
        smoothedY += alpha * (y - smoothedY)

        layers.forEach { layer ->
            val offsetX = smoothedX * movementRangePx * layer.depthFactor * intensity
            val offsetY = smoothedY * movementRangePx * layer.depthFactor * intensity
            layer.view.translationX = offsetX
            layer.view.translationY = offsetY
        }
    }
}
