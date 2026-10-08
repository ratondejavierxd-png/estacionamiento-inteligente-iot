package com.example.estacionamiento

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.estacionamiento.databinding.ActivityHistorialBinding
import kotlinx.coroutines.launch

class HistorialActivity : AppCompatActivity() {
    private lateinit var b: ActivityHistorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHistorialBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnActualizar.setOnClickListener { cargar() }
        cargar()
    }

    private fun cargar() {
        lifecycleScope.launch {
            try {
                val arr = Api.request("/historial").getJSONArray("lecturas")
                val filas = (0 until arr.length()).map {
                    val l = arr.getJSONObject(it)
                    "Plaza ${l.getInt("plaza_id")} · ${l.getDouble("distancia_m")} m · " +
                        "${l.getString("estado")}\n${l.getString("fecha")}"
                }
                b.lista.adapter = ArrayAdapter(
                    this@HistorialActivity, android.R.layout.simple_list_item_1, filas
                )
            } catch (e: SesionExpirada) {
                Api.volverAlLogin(this@HistorialActivity)
            } catch (e: Exception) {
                Toast.makeText(this@HistorialActivity, Api.mensaje(e), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
