package com.example.estacionamiento

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.estacionamiento.databinding.ActivityMenuBinding

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnMonitor.setOnClickListener { startActivity(Intent(this, MonitorActivity::class.java)) }
        b.btnSensor.setOnClickListener { startActivity(Intent(this, SensorActivity::class.java)) }
        b.btnHistorial.setOnClickListener { startActivity(Intent(this, HistorialActivity::class.java)) }
        b.btnSalir.setOnClickListener { Api.volverAlLogin(this) }
    }
}
