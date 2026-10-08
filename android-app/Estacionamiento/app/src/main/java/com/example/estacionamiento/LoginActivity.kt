package com.example.estacionamiento

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.estacionamiento.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnLogin.setOnClickListener { enviar(registrar = false) }
        b.btnRegistro.setOnClickListener { enviar(registrar = true) }
    }

    private fun enviar(registrar: Boolean) {
        val u = b.etUser.text.toString().trim()
        val p = b.etPass.text.toString()

        // Validación de entradas en el cliente (el servidor valida de nuevo)
        if (!Regex("[A-Za-z0-9_.-]{3,50}").matches(u)) {
            toast("Usuario inválido (3-50 caracteres: letras, números, . _ -)")
            return
        }
        if (p.length < 8) {
            toast("La contraseña debe tener al menos 8 caracteres")
            return
        }

        activarBotones(false)
        lifecycleScope.launch {
            try {
                val datos = JSONObject().put("username", u).put("password", p)
                if (registrar) Api.request("/registro", datos)
                val r = Api.request("/login", datos)
                Api.token = r.getString("token")
                startActivity(Intent(this@LoginActivity, MenuActivity::class.java))
                finish()
            } catch (e: Exception) {
                toast(Api.mensaje(e))
            } finally {
                activarBotones(true)
            }
        }
    }

    private fun activarBotones(si: Boolean) {
        b.btnLogin.isEnabled = si
        b.btnRegistro.isEnabled = si
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
