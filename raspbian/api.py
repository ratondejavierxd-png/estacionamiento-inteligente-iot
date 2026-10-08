"""
API del Estacionamiento Inteligente (Flask + MySQL en AWS RDS).
Configuración por variables de entorno: DB_HOST, DB_USER, DB_PASS (y opcional DB_NAME).
"""
import os
import re
import time
import secrets

import bcrypt
import pymysql
from flask import Flask, request, jsonify

app = Flask(__name__)

UMBRAL_M = 5.0          # menos de 5 metros = OCUPADO
TOKEN_TTL = 3600        # el token dura 1 hora
TOKENS = {}             # token -> {"uid": ..., "exp": ...}
CA_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "global-bundle.pem")


def conectar():
    """Conexión a RDS cifrada con TLS (certificado de Amazon)."""
    return pymysql.connect(
        host=os.environ["DB_HOST"],
        user=os.environ["DB_USER"],
        password=os.environ["DB_PASS"],
        database=os.environ.get("DB_NAME", "estacionamiento"),
        ssl={"ca": CA_PATH},
        autocommit=True,
        connect_timeout=10,
        cursorclass=pymysql.cursors.DictCursor,
    )


def consulta(sql, params=(), uno=False):
    """Ejecuta un SELECT parametrizado y devuelve una fila o todas."""
    con = conectar()
    try:
        with con.cursor() as cur:
            cur.execute(sql, params)
            return cur.fetchone() if uno else cur.fetchall()
    finally:
        con.close()


def error(msg, code):
    return jsonify({"error": msg}), code


def autenticado():
    token = request.headers.get("Authorization", "").replace("Bearer ", "").strip()
    datos = TOKENS.get(token)
    if datos and datos["exp"] > time.time():
        return datos
    TOKENS.pop(token, None)
    return None


@app.errorhandler(pymysql.MySQLError)
def error_bd(e):
    print("Error de base de datos:", e)
    return error("Base de datos no disponible", 503)


@app.route("/salud", methods=["GET"])
def salud():
    return jsonify({"ok": True})


@app.route("/registro", methods=["POST"])
def registro():
    j = request.get_json(silent=True) or {}
    usuario = str(j.get("username", "")).strip()
    clave = str(j.get("password", ""))
    if not re.fullmatch(r"[A-Za-z0-9_.-]{3,50}", usuario):
        return error("Usuario inválido (3-50 caracteres: letras, números, . _ -)", 400)
    if len(clave) < 8:
        return error("La contraseña debe tener al menos 8 caracteres", 400)
    hash_ = bcrypt.hashpw(clave.encode(), bcrypt.gensalt()).decode()
    con = conectar()
    try:
        with con.cursor() as cur:
            cur.execute(
                "INSERT INTO usuarios (username, password_hash) VALUES (%s, %s)",
                (usuario, hash_),
            )
    except pymysql.err.IntegrityError:
        return error("El usuario ya existe", 409)
    finally:
        con.close()
    return jsonify({"ok": True}), 201


@app.route("/login", methods=["POST"])
def login():
    j = request.get_json(silent=True) or {}
    usuario = str(j.get("username", ""))
    clave = str(j.get("password", ""))
    fila = consulta(
        "SELECT id, password_hash FROM usuarios WHERE username = %s", (usuario,), uno=True
    )
    if not fila or not bcrypt.checkpw(clave.encode(), fila["password_hash"].encode()):
        return error("Credenciales inválidas", 401)
    token = secrets.token_hex(32)
    TOKENS[token] = {"uid": fila["id"], "exp": time.time() + TOKEN_TTL}
    return jsonify({"token": token})


@app.route("/estado", methods=["GET"])
def estado():
    if not autenticado():
        return error("No autorizado", 401)
    plazas = consulta("SELECT id, nombre, estado, actualizado FROM plazas ORDER BY id")
    for p in plazas:
        p["actualizado"] = str(p["actualizado"])
    return jsonify({"plazas": plazas})


@app.route("/lectura", methods=["POST"])
def lectura():
    if not autenticado():
        return error("No autorizado", 401)
    j = request.get_json(silent=True) or {}
    try:
        plaza = int(j["plaza_id"])
        dist = float(j["distancia"])
    except (KeyError, ValueError, TypeError):
        return error("Datos inválidos", 400)
    if not (0 <= dist <= 100):
        return error("Distancia fuera de rango (0-100 m)", 400)

    nuevo_estado = "OCUPADO" if dist < UMBRAL_M else "DISPONIBLE"
    con = conectar()
    try:
        with con.cursor() as cur:
            cur.execute("SELECT id FROM plazas WHERE id = %s", (plaza,))
            if not cur.fetchone():
                return error("Plaza inexistente", 404)
            cur.execute(
                "INSERT INTO lecturas (plaza_id, distancia_m, estado) VALUES (%s, %s, %s)",
                (plaza, dist, nuevo_estado),
            )
            cur.execute("UPDATE plazas SET estado = %s WHERE id = %s", (nuevo_estado, plaza))
    finally:
        con.close()
    return jsonify({"plaza_id": plaza, "estado": nuevo_estado})


@app.route("/historial", methods=["GET"])
def historial():
    if not autenticado():
        return error("No autorizado", 401)
    filas = consulta(
        "SELECT plaza_id, distancia_m, estado, fecha FROM lecturas ORDER BY id DESC LIMIT 50"
    )
    for f in filas:
        f["distancia_m"] = float(f["distancia_m"])
        f["fecha"] = str(f["fecha"])
    return jsonify({"lecturas": filas})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
