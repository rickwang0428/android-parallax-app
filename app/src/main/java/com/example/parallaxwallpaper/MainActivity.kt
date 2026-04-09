package com.example.parallaxwallpaper

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var intensityValue: TextView
    private lateinit var intensitySeek: SeekBar
    private lateinit var parallaxController: ParallaxController

    private var intensity = DEFAULT_INTENSITY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val backLayer = findViewById<ImageView>(R.id.layerBack)
        val midLayer = findViewById<ImageView>(R.id.layerMid)
        val frontLayer = findViewById<ImageView>(R.id.layerFront)

        intensityValue = findViewById(R.id.intensityValue)
        intensitySeek = findViewById(R.id.intensitySeek)

        intensity = readIntensity()
        intensitySeek.progress = (intensity * 100f).roundToInt()

        parallaxController = ParallaxController(
            layers = listOf(
                LayerConfig(backLayer, depthFactor = 0.35f),
                LayerConfig(midLayer, depthFactor = 0.65f),
                LayerConfig(frontLayer, depthFactor = 1.0f)
            ),
            movementRangePx = 36f
        )

        updateIntensityUi(intensity)
        parallaxController.updateIntensity(intensity)

        intensitySeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val normalized = progress / 100f
                updateIntensityUi(normalized)
                parallaxController.updateIntensity(normalized)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val normalized = (seekBar?.progress ?: 0) / 100f
                intensity = normalized
                saveIntensity(normalized)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = (-event.values[0] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
        val y = (event.values[1] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
        parallaxController.onTiltChanged(x, y)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun updateIntensityUi(value: Float) {
        intensityValue.text = getString(R.string.intensity_value_format, (value * 100).roundToInt())
    }

    private fun readIntensity(): Float {
        val pref = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return pref.getFloat(KEY_INTENSITY, DEFAULT_INTENSITY).coerceIn(0f, 1f)
    }

    private fun saveIntensity(value: Float) {
        val pref = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        pref.edit().putFloat(KEY_INTENSITY, value).apply()
    }

    companion object {
        private const val PREFS = "parallax_prefs"
        private const val KEY_INTENSITY = "intensity"
        private const val DEFAULT_INTENSITY = 0.4f
    }
}
