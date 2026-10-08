# Estacionamiento Inteligente IoT

Proyecto de la asignatura **TI3042 · Unidad 2**. Simulación de un estacionamiento inteligente: un sensor de distancia detecta si hay un vehículo a menos de 5 metros y la plaza pasa a **OCUPADO**; en caso contrario queda **DISPONIBLE**. El estado se consulta en tiempo real desde una app Android.

**Autor: Javier Rojas**

## Arquitectura

```
 Dispositivo Android 1            Dispositivo Android 2
 (Monitor de plazas)              (Modo sensor)
          \                          /
           \──────── HTTP ──────────/        (WiFi, red local)
                       │
          ┌────────────▼─────────────┐
          │  Raspbian (VirtualBox)   │      Archivos subidos con FileZilla (SFTP)
          │  API REST (Flask)        │
          └────────────┬─────────────┘
                       │  TLS (certificado de Amazon)
          ┌────────────▼─────────────┐
          │  AWS RDS · MySQL 8.4     │ ◄──── MySQL Workbench (administración)
          └──────────────────────────┘
```

## Tecnologías y justificación

| Herramienta | Uso | Por qué |
|---|---|---|
| Android Studio + Kotlin | App móvil | Lenguaje y entorno oficiales de Android |
| OkHttp + corrutinas | Cliente HTTP de la app | Peticiones asíncronas sin bloquear la interfaz |
| Raspbian en VirtualBox | Servidor de la API | Simula el nodo IoT/gateway sin hardware físico |
| Python + Flask | API REST | Ligera y adecuada para un nodo de bajo consumo |
| AWS RDS (MySQL 8.4) | Almacenamiento | Base de datos administrada, con respaldo y cifrado |
| MySQL Workbench | Administración y consultas | Verificación visual de los datos |
| FileZilla (SFTP) | Transferencia de archivos | Subida cifrada de la API a la Raspbian |

## Estructura del repositorio

```
├── android-app/     Proyecto de Android Studio (Kotlin)
├── raspbian/
│   ├── api.py       API REST (Flask)
│   ├── sensor.py    Sensor de distancia simulado
│   ├── schema.sql   Esquema de la base de datos
│   └── env.example  Plantilla de variables de entorno
└── README.md
```

## Instalación

### 1. Base de datos (AWS RDS)
1. Crear una instancia **MySQL** en RDS (capa gratuita) con la base inicial `estacionamiento`.
2. En el grupo de seguridad, permitir el puerto **3306** únicamente desde tu IP pública (`/32`).
3. Ejecutar `raspbian/schema.sql` desde MySQL Workbench, cambiando antes `CAMBIAR_CLAVE`.

### 2. Raspbian (API)
```bash
sudo apt install -y python3-flask python3-pymysql python3-bcrypt python3-requests
mkdir ~/estacionamiento && cd ~/estacionamiento
# Subir api.py y sensor.py con FileZilla (SFTP, puerto 22)
curl -o global-bundle.pem https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
cp env.example env.sh        # editar con los valores reales (chmod 600 env.sh)
source env.sh
nohup python3 api.py > api.log 2>&1 &
curl http://localhost:5000/salud
```

### 3. App Android
1. Abrir `android-app/` con Android Studio.
2. Poner la IP de la Raspbian en `Api.kt` (`BASE`) y en `res/xml/network_security_config.xml`.
3. Ejecutar en dos dispositivos conectados a la misma red que la Raspbian.

## Uso (demo con dos dispositivos)
- **Dispositivo 1:** registrarse e iniciar sesión → *Monitor de plazas*.
- **Dispositivo 2:** iniciar sesión → *Modo sensor* → elegir plaza y distancia (menor a 5 m = ocupada) → *Enviar lectura*, o activar el modo automático.
- El monitor actualiza el color de la plaza (rojo/verde) cada 2 segundos.
- *Historial* muestra las últimas lecturas guardadas en MySQL.

## API REST

| Método | Ruta | Descripción | Autenticación |
|---|---|---|---|
| GET | `/salud` | Comprobación de estado | No |
| POST | `/registro` | Crea un usuario | No |
| POST | `/login` | Devuelve un token (1 hora) | No |
| GET | `/estado` | Estado de las plazas | Token |
| POST | `/lectura` | Registra una lectura y actualiza la plaza | Token |
| GET | `/historial` | Últimas 50 lecturas | Token |

## Seguridad (basada en las guías ISO/IEC 27400 para IoT)

| Medida | Cómo se aplica |
|---|---|
| Contraseñas protegidas | Hash con bcrypt; nunca se guardan en texto plano |
| Autenticación | Token aleatorio con expiración de 1 hora; los endpoints de datos lo exigen |
| Mínimo privilegio | La API usa `apiuser`, solo con SELECT, INSERT y UPDATE |
| Cifrado API → base de datos | TLS con el certificado de Amazon; `apiuser` exige SSL |
| Acceso restringido a la nube | Security Group de AWS abierto solo a una IP (`/32`) |
| Inyección SQL | Consultas parametrizadas |
| Validación de datos | Usuario, contraseña, plaza y rango de distancia, en la app y en el servidor |
| Secretos fuera del código | Variables de entorno (`env.sh` con permisos 600, excluido del repositorio) |
| App Android | `allowBackup="false"`, token solo en memoria y HTTP permitido únicamente hacia la IP del servidor |

## Limitaciones y mejoras para producción
- La comunicación **app → API** usa HTTP dentro de la red local. En producción se usaría **HTTPS** (por ejemplo, Nginx con certificado).
- La base de datos tiene acceso público protegido por el firewall. En producción iría en una **subred privada**, con la API dentro de la misma VPC.
- Las credenciales de la base se gestionarían con **AWS Secrets Manager**.
- Los tokens se guardan en memoria del servidor: se pierden al reiniciar la API.
