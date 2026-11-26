"""Servicio REST simulado para la app de recordatorios (solo biblioteca estándar de Python 3).

Uso:
    python tools/servidor_simulado.py                 # modo normal, puerto 8090
    python tools/servidor_simulado.py --modo error500 # responde 500 a todo
    python tools/servidor_simulado.py --modo lento    # tarda 8 s (la app corta a los 5 s: timeout)
    python tools/servidor_simulado.py --modo invalido # rechaza altas y cambios con 400

Endpoints:
    GET    /recordatorios
    POST   /recordatorios          (idempotente por uuid: si ya existe, devuelve el mismo)
    PUT    /recordatorios/<id>
    DELETE /recordatorios/<id>

Los datos se guardan en tools/datos_servidor.json para que sobrevivan a un reinicio.
"""

import argparse
import json
import threading
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ARCHIVO = Path(__file__).with_name("datos_servidor.json")
CATEGORIAS = {1, 2, 3, 4}
PRIORIDADES = {"ALTA", "MEDIA", "BAJA"}
ESTADOS = {"PENDIENTE", "COMPLETADO", "CANCELADO"}
candado = threading.Lock()
contador = {"POST": 0, "PUT": 0, "DELETE": 0, "GET": 0}


def ahora_ms():
    return int(time.time() * 1000)


def datos_iniciales():
    base = [
        ("Enviar informe mensual", "Incluir las métricas del último mes", 1, "ALTA", "PENDIENTE", None),
        ("Reunión con el equipo", None, 1, "MEDIA", "PENDIENTE", "Oficina central"),
        ("Entregar Actividad 3", "Room, DataStore y Retrofit", 2, "ALTA", "PENDIENTE", None),
        ("Repasar sealed classes", None, 2, "MEDIA", "COMPLETADO", None),
        ("Turno con el dentista", None, 3, "ALTA", "CANCELADO", "Consultorio"),
        ("Comprar vitaminas", None, 3, "BAJA", "PENDIENTE", None),
        ("Pagar la luz", None, 4, "MEDIA", "COMPLETADO", None),
    ]
    t = ahora_ms()
    lista = []
    for i, (titulo, desc, cat, prio, estado, ubic) in enumerate(base, start=1):
        lista.append({
            "id": i,
            "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, f"recordatorio-{i}")),
            "titulo": titulo,
            "descripcion": desc,
            "categoriaId": cat,
            "prioridad": prio,
            "estado": estado,
            "fechaEstado": t - 86_400_000 if estado == "COMPLETADO" else None,
            "motivoCancelacion": "Se reprogramó" if estado == "CANCELADO" else None,
            "fechaAviso": None,
            "ubicacion": ubic,
            "actualizadoEn": t - i * 60_000,
        })
    return {"siguienteId": len(lista) + 1, "recordatorios": lista}


def cargar():
    if ARCHIVO.exists():
        return json.loads(ARCHIVO.read_text(encoding="utf-8"))
    datos = datos_iniciales()
    guardar(datos)
    return datos


def guardar(datos):
    ARCHIVO.write_text(json.dumps(datos, ensure_ascii=False, indent=2), encoding="utf-8")


def validar(cuerpo):
    titulo = (cuerpo.get("titulo") or "").strip()
    if len(titulo) < 4 or len(titulo) > 60:
        return "El título debe tener entre 4 y 60 caracteres"
    if len(cuerpo.get("descripcion") or "") > 200:
        return "La descripción supera los 200 caracteres"
    if cuerpo.get("categoriaId") not in CATEGORIAS:
        return "Categoría desconocida"
    if cuerpo.get("prioridad") not in PRIORIDADES:
        return "Prioridad desconocida"
    if cuerpo.get("estado") not in ESTADOS:
        return "Estado desconocido"
    if not cuerpo.get("uuid"):
        return "Falta el uuid"
    return None


CAMPOS = ["uuid", "titulo", "descripcion", "categoriaId", "prioridad", "estado",
          "fechaEstado", "motivoCancelacion", "fechaAviso", "ubicacion", "actualizadoEn"]


class Manejador(BaseHTTPRequestHandler):
    modo = "normal"

    def log_message(self, formato, *args):
        print(f"[{time.strftime('%H:%M:%S')}] {self.command} {self.path} -> {args[1] if len(args) > 1 else ''}")

    def responder(self, codigo, cuerpo=None):
        datos = b"" if cuerpo is None else json.dumps(cuerpo, ensure_ascii=False).encode("utf-8")
        self.send_response(codigo)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(datos)))
        self.end_headers()
        if datos:
            self.wfile.write(datos)

    def leer_cuerpo(self):
        largo = int(self.headers.get("Content-Length") or 0)
        try:
            return json.loads(self.rfile.read(largo) or b"{}")
        except json.JSONDecodeError:
            return None

    def simular_modo(self, es_escritura):
        if self.modo == "lento":
            time.sleep(8)
        if self.modo == "error500":
            self.responder(500, {"error": "Error interno simulado"})
            return True
        if self.modo == "invalido" and es_escritura:
            self.responder(400, {"error": "Solicitud rechazada (modo invalido)"})
            return True
        return False

    def id_de_ruta(self):
        partes = self.path.rstrip("/").split("/")
        if len(partes) == 3 and partes[1] == "recordatorios" and partes[2].isdigit():
            return int(partes[2])
        return None

    def do_GET(self):
        contador["GET"] += 1
        if self.simular_modo(False):
            return
        if self.path.rstrip("/") != "/recordatorios":
            return self.responder(404, {"error": "Ruta inexistente"})
        with candado:
            self.responder(200, cargar()["recordatorios"])

    def do_POST(self):
        contador["POST"] += 1
        if self.simular_modo(True):
            return
        cuerpo = self.leer_cuerpo()
        if cuerpo is None or self.path.rstrip("/") != "/recordatorios":
            return self.responder(400, {"error": "JSON inválido"})
        error = validar(cuerpo)
        if error:
            return self.responder(400, {"error": error})
        with candado:
            datos = cargar()
            existente = next((r for r in datos["recordatorios"] if r["uuid"] == cuerpo["uuid"]), None)
            if existente:
                # Idempotencia: el mismo uuid nunca crea un segundo registro
                print(f"    uuid repetido, se devuelve el id {existente['id']} sin duplicar")
                return self.responder(200, existente)
            nuevo = {campo: cuerpo.get(campo) for campo in CAMPOS}
            nuevo["id"] = datos["siguienteId"]
            datos["siguienteId"] += 1
            datos["recordatorios"].append(nuevo)
            guardar(datos)
            print(f"    creado id {nuevo['id']}: {nuevo['titulo']} (total {len(datos['recordatorios'])})")
            self.responder(201, nuevo)

    def do_PUT(self):
        contador["PUT"] += 1
        if self.simular_modo(True):
            return
        rid = self.id_de_ruta()
        cuerpo = self.leer_cuerpo()
        if rid is None or cuerpo is None:
            return self.responder(400, {"error": "Solicitud inválida"})
        error = validar(cuerpo)
        if error:
            return self.responder(400, {"error": error})
        with candado:
            datos = cargar()
            actual = next((r for r in datos["recordatorios"] if r["id"] == rid), None)
            if actual is None:
                return self.responder(404, {"error": "No existe"})
            for campo in CAMPOS:
                actual[campo] = cuerpo.get(campo)
            guardar(datos)
            self.responder(200, actual)

    def do_DELETE(self):
        contador["DELETE"] += 1
        if self.simular_modo(True):
            return
        rid = self.id_de_ruta()
        with candado:
            datos = cargar()
            antes = len(datos["recordatorios"])
            datos["recordatorios"] = [r for r in datos["recordatorios"] if r["id"] != rid]
            if len(datos["recordatorios"]) == antes:
                return self.responder(404, {"error": "No existe"})
            guardar(datos)
            self.responder(204)


def main():
    parser = argparse.ArgumentParser(description="Servicio simulado de recordatorios")
    parser.add_argument("--puerto", type=int, default=8090)
    parser.add_argument("--modo", choices=["normal", "error500", "lento", "invalido"], default="normal")
    args = parser.parse_args()
    Manejador.modo = args.modo
    cargar()
    servidor = ThreadingHTTPServer(("0.0.0.0", args.puerto), Manejador)
    print(f"Servicio simulado en http://localhost:{args.puerto}/recordatorios (modo {args.modo})")
    try:
        servidor.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
