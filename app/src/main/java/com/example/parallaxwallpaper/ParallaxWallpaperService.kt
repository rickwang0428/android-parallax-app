package com.example.parallaxwallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class ParallaxWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = ParallaxEngine()

    private inner class ParallaxEngine : Engine(), SensorEventListener {

        private val handler = Handler(Looper.getMainLooper())
        private val drawRunner = Runnable { drawFrame() }

        private val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        private val layers: List<Bitmap> by lazy {
            listOf(
                BitmapFactory.decodeResource(resources, R.drawable.layer_background),
                BitmapFactory.decodeResource(resources, R.drawable.layer_mid),
                BitmapFactory.decodeResource(resources, R.drawable.layer_front)
            )
        }

        private val layerDepth = listOf(0.35f, 0.65f, 1f)
        private var visible = false
        private var xTilt = 0f
        private var yTilt = 0f
        private var smoothedX = 0f
        private var smoothedY = 0f

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                registerSensor()
                drawFrame()
            } else {
                handler.removeCallbacks(drawRunner)
                sensorManager.unregisterListener(this)
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunner)
            sensorManager.unregisterListener(this)
            layers.forEach { if (!it.isRecycled) it.recycle() }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunner)
            sensorManager.unregisterListener(this)
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
            xTilt = (-event.values[0] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
            yTilt = (event.values[1] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

        private fun registerSensor() {
            accelerometer?.also {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            val canvas = holder.lockCanvas()
            if (canvas != null) {
                try {
                    render(canvas)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            handler.removeCallbacks(drawRunner)
            if (visible) {
                handler.postDelayed(drawRunner, 16L)
            }
        }

        private fun render(canvas: Canvas) {
            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()
            val intensity = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getFloat(KEY_INTENSITY, DEFAULT_INTENSITY)
                .coerceIn(0f, 1f)

            val alpha = 0.15f
            smoothedX += alpha * (xTilt - smoothedX)
            smoothedY += alpha * (yTilt - smoothedY)

            canvas.drawColor(Color.BLACK)

            layers.forEachIndexed { index, bitmap ->
                val scale = maxOf(width / bitmap.width, height / bitmap.height) * 1.12f
                val scaledWidth = bitmap.width * scale
                val scaledHeight = bitmap.height * scale

                val baseX = (width - scaledWidth) / 2f
                val baseY = (height - scaledHeight) / 2f

                val range = 42f * intensity * layerDepth[index]
                val dx = smoothedX * range
                val dy = smoothedY * range

                val left = baseX + dx
                val top = baseY + dy

                canvas.save()
                canvas.translate(left, top)
                canvas.scale(scale, scale)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                canvas.restore()
            }
        }
    }

    companion object {
        private const val PREFS = "parallax_prefs"
        private const val KEY_INTENSITY = "intensity"
        private const val DEFAULT_INTENSITY = 0.4f
    }
}
