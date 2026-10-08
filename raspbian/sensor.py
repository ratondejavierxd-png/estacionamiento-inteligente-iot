"""
Sensor de distancia simulado para el Estacionamiento Inteligente.
Envía una lectura aleatoria por plaza cada pocos segundos a la API.
Variables de entorno: SENSOR_PASS (obligatoria), SENSOR_USER y API_URL (opcionales).
"""
import os
import random
import time

import requests

API = os.environ.get("API_URL", "http://localhost:5000")
USUARIO = os.environ.get("SENSOR_USER", "sensor")
CLAVE = os.environ["SENSOR_PASS"]
PLAZAS = (1, 2, 3)
INTERVALO_S = 3


def iniciar_sesion():
    r = requests.post(
        API + "/login", json={"username": USUARIO, "password": CLAVE}, timeout=10
    )
    r.raise_for_status()
    return {"Authorization": "Bearer " + r.json()["token"]}


def main():
    headers = iniciar_sesion()
    print("Sesión iniciada. Enviando lecturas (Ctrl+C para detener)...")
    while True:
        for plaza in PLAZAS:
            distancia = round(random.uniform(0.5, 10), 2)
            try:
                r = requests.post(
                    API + "/lectura",
                    json={"plaza_id": plaza, "distancia": distancia},
                    headers=headers,
                    timeout=10,
                )
                if r.status_code == 401:       # el token expiró: volver a entrar
                    headers = iniciar_sesion()
                    continue
                print("Plaza", plaza, "|", distancia, "m |", r.json())
            except requests.RequestException as e:
                print("Error de red:", e)
        time.sleep(INTERVALO_S)


if __name__ == "__main__":
    main()
