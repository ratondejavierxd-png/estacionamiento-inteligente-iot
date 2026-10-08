package com.example.estacionamiento

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.estacionamiento.databinding.ActivitySensorBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.random.Random

class SensorActivity : AppCompatActivity() {
    private lateinit var b: ActivitySensorBinding
    private var autoJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySensorBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.spPlaza.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, listOf("1", "2", "3")
        )

        b.seekDistancia.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                b.tvDistancia.text = "Distancia: ${p / 10.0} m"
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        // Envío manual de una lectura
        b.btnEnviar.setOnClickListener {
            val plaza = b.spPlaza.selectedItem.toString().toInt()
            val metros = b.seekDistancia.progress / 10.0
            lifecycleScope.launch { enviar(plaza, metros) }
        }

        // Modo automático: lecturas aleatorias cada 3 segundos
        b.swAuto.setOnCheckedChangeListener { _, activo ->
            autoJob?.cancel()
            if (activo) {
                autoJob = lifecycleScope.launch {
                    while (isActive) {
                        val plaza = b.spPlaza.selectedItem.toString().toInt()
                        val metros = Random.nextInt(5, 100) / 10.0   // 0.5 a 9.9 m
                        enviar(plaza, metros)
                        delay(3000)
                    }
                }
            }
        }
    }

    private suspend fun enviar(plaza: Int, metros: Double) {
        try {
            val r = Api.request(
                "/lectura",
                JSONObject().put("plaza_id", plaza).put("distancia", metros)
            )
            b.tvResultado.text = "Plaza $plaza · $metros m → ${r.getString("estado")}"
        } catch (e: CancellationException) {
            throw e
        } catch (e: SesionExpirada) {
            autoJob?.cancel()
            Api.volverAlLogin(this)
        } catch (e: Exception) {
            b.tvResultado.text = "⚠️ ${Api.mensaje(e)}"
        }
    }
}
