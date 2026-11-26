"""Consulta Firestore con una cuenta de prueba, como lo haría cualquier cliente.

Sirve para registrar evidencias: qué documentos tiene una cuenta y qué responde el servicio
cuando esa cuenta intenta leer o escribir los datos de otra. Usa solo la biblioteca estándar.

Uso:
    python tools/verificar_firestore.py listar   EMAIL CONTRASENIA [--coleccion dev_users]
    python tools/verificar_firestore.py ajeno    EMAIL CONTRASENIA UID_AJENO [--coleccion dev_users]

Lee projectId y apiKey de firebase.properties (no se sube al repositorio).
"""

import argparse
import json
import urllib.error
import urllib.request
from pathlib import Path


def leer_propiedades():
    archivo = Path(__file__).resolve().parent.parent / "firebase.properties"
    datos = {}
    for linea in archivo.read_text(encoding="utf-8").splitlines():
        if "=" in linea and not linea.lstrip().startswith("#"):
            clave, valor = linea.split("=", 1)
            datos[clave.strip()] = valor.strip()
    return datos["firebase.projectId"], datos["firebase.apiKey"]


def pedir(url, metodo="GET", cuerpo=None, token=None):
    datos = None if cuerpo is None else json.dumps(cuerpo).encode("utf-8")
    pedido = urllib.request.Request(url, data=datos, method=metodo)
    pedido.add_header("Content-Type", "application/json")
    if token:
        pedido.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(pedido, timeout=20) as respuesta:
            return respuesta.status, json.loads(respuesta.read() or b"{}")
    except urllib.error.HTTPError as error:
        return error.code, json.loads(error.read() or b"{}")


def iniciar_sesion(api_key, email, contrasenia):
    url = f"https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={api_key}"
    codigo, datos = pedir(url, "POST", {"email": email, "password": contrasenia, "returnSecureToken": True})
    if codigo != 200:
        raise SystemExit(f"No se pudo iniciar sesión: {datos.get('error', {}).get('message')}")
    return datos["localId"], datos["idToken"]


def valor(campo):
    for tipo in ("stringValue", "integerValue", "nullValue"):
        if tipo in campo:
            return None if tipo == "nullValue" else campo[tipo]
    return campo


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("accion", choices=["listar", "ajeno"])
    parser.add_argument("email")
    parser.add_argument("contrasenia")
    parser.add_argument("uid_ajeno", nargs="?")
    parser.add_argument("--coleccion", default="dev_users")
    args = parser.parse_args()

    proyecto, api_key = leer_propiedades()
    uid, token = iniciar_sesion(api_key, args.email, args.contrasenia)
    base = f"https://firestore.googleapis.com/v1/projects/{proyecto}/databases/(default)/documents"
    print(f"Sesión iniciada: {args.email} (uid {uid})")

    if args.accion == "listar":
        codigo, datos = pedir(f"{base}/{args.coleccion}/{uid}/reminders", token=token)
        documentos = datos.get("documents", [])
        print(f"GET {args.coleccion}/{uid}/reminders -> HTTP {codigo}, {len(documentos)} documentos")
        for doc in documentos:
            campos = {k: valor(v) for k, v in doc["fields"].items()}
            print(f"  {doc['name'].rsplit('/', 1)[-1]}  {campos['titulo']}  [{campos['estado']}]")
        return

    ruta = f"{base}/{args.coleccion}/{args.uid_ajeno}/reminders"
    codigo, datos = pedir(ruta, token=token)
    print(f"Leer {args.coleccion}/{args.uid_ajeno}/reminders -> HTTP {codigo} {datos.get('error', {}).get('status', '')}")
    intruso = {"fields": {
        "uid": {"stringValue": args.uid_ajeno}, "uuid": {"stringValue": "intruso"},
        "titulo": {"stringValue": "Escritura ajena"}, "descripcion": {"nullValue": None},
        "categoriaId": {"integerValue": "1"}, "prioridad": {"stringValue": "ALTA"},
        "estado": {"stringValue": "PENDIENTE"},
    }}
    codigo, datos = pedir(f"{ruta}/intruso", "PATCH", intruso, token)
    print(f"Escribir {args.coleccion}/{args.uid_ajeno}/reminders/intruso -> HTTP {codigo} {datos.get('error', {}).get('status', '')}")


if __name__ == "__main__":
    main()
