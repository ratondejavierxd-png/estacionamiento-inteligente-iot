package com.example.estacionamiento

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.estacionamiento.databinding.ActivityMonitorBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MonitorActivity : AppCompatActivity() {
    private lateinit var b: ActivityMonitorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMonitorBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Consulta cada 2 segundos solo mientras la pantalla está visible
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    try {
                        val plazas = Api.request("/estado").getJSONArray("plazas")
                        b.tvConexion.text = "🟢 Conectado"
                        pintar(plazas)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: SesionExpirada) {
                        Toast.makeText(this@MonitorActivity, "Sesión expirada", Toast.LENGTH_SHORT).show()
                        Api.volverAlLogin(this@MonitorActivity)
                        return@repeatOnLifecycle
                    } catch (e: Exception) {
                        b.tvConexion.text = "🔴 Desconectado"
                    }
                    delay(2000)
                }
            }
        }
    }

    private fun pintar(plazas: org.json.JSONArray) {
        b.contenedor.removeAllViews()
        val alto = (90 * resources.displayMetrics.density).toInt()
        val margen = (12 * resources.displayMetrics.density).toInt()
        for (i in 0 until plazas.length()) {
            val p = plazas.getJSONObject(i)
            val ocupado = p.getString("estado") == "OCUPADO"
            val tv = TextView(this).apply {
                text = "${p.getString("nombre")}: ${p.getString("estado")}"
                textSize = 20f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor(if (ocupado) "#D32F2F" else "#388E3C"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, alto
                ).apply { setMargins(0, 0, 0, margen) }
            }
            b.contenedor.addView(tv)
        }
    }
}
