#!/usr/bin/env python3
"""
Test de validation de l'endpoint REST GET /api/matches/predict
et de l'intégrité métier.
"""

import urllib.request
import urllib.error
import json
import sys

BASE_URL = "http://localhost:80/api"

def get_token(username, password):
    login_data = json.dumps({"username": username, "password": password}).encode("utf-8")
    req = urllib.request.Request(
        f"{BASE_URL}/auth/login",
        data=login_data,
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode("utf-8"))["token"]

def register_user(username, password):
    reg_data = json.dumps({"username": username, "password": password}).encode("utf-8")
    req = urllib.request.Request(
        f"{BASE_URL}/auth/register",
        data=reg_data,
        headers={"Content-Type": "application/json"}
    )
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode("utf-8"))["token"]
    except urllib.error.HTTPError as e:
        if e.code == 400 or e.code == 409:
            return get_token(username, password)
        raise

def get_players(token):
    req = urllib.request.Request(
        f"{BASE_URL}/players",
        headers={"Authorization": f"Bearer {token}"}
    )
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode("utf-8"))

def call_predict(token, p1_id, p2_id):
    url = f"{BASE_URL}/matches/predict?player1Id={p1_id}&player2Id={p2_id}"
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace")
        try:
            return e.code, json.loads(body)
        except Exception:
            return e.code, body

def main():
    print("=" * 80)
    print("  TEST DE VALIDATION DE L'ENDPOINT GET /api/matches/predict")
    print("=" * 80)

    admin_token = get_token("admin", "admin123")
    user_token = register_user("crash_user", "UserPass123!")
    print("[OK] Tokens ADMIN et USER récupérés.")

    players = get_players(admin_token)
    print(f"[OK] {len(players)} joueurs disponibles en base.")
    if len(players) < 2:
        print("Erreur: moins de 2 joueurs en base.")
        sys.exit(1)

    p1 = players[0]
    p2 = players[1]
    print(f" -> Joueur 1 : ID={p1['id']}, Pseudo={p1['nickname']}, Lvl={p1['level']}, Score={p1['score']}")
    print(f" -> Joueur 2 : ID={p2['id']}, Pseudo={p2['nickname']}, Lvl={p2['level']}, Score={p2['score']}")

    all_passed = True

    # 1. Non authentifié -> 401
    status, body = call_predict(None, p1["id"], p2["id"])
    if status == 401:
        print("[PASS] 1. Requête non authentifiée rejetée avec HTTP 401")
    else:
        print(f"[FAIL] 1. Requête non authentifiée attendait 401, reçu {status}")
        all_passed = False

    # 2. Rôle USER authentifié -> 200 avec structure DTO exacte
    status, body = call_predict(user_token, p1["id"], p2["id"])
    required_keys = {
        "player1Id", "player1Nickname", "player2Id", "player2Nickname",
        "player1WinProbability", "player2WinProbability", "favoriteNickname"
    }
    if status == 200 and isinstance(body, dict) and required_keys.issubset(body.keys()):
        total_prob = body["player1WinProbability"] + body["player2WinProbability"]
        print(f"[PASS] 2. Rôle USER autorisé (HTTP 200)")
        print(f"       DTO reçu : {json.dumps(body, indent=2)}")
        if total_prob == 100:
            print(f"[PASS]    Somme des probabilités = 100% ({body['player1WinProbability']}% + {body['player2WinProbability']}%)")
        else:
            print(f"[FAIL]    Somme des probabilités != 100% : {total_prob}")
            all_passed = False
    else:
        print(f"[FAIL] 2. Échec prédiction USER : status={status}, body={body}")
        all_passed = False

    # 3. Rôle ADMIN authentifié -> 200
    status, body = call_predict(admin_token, p1["id"], p2["id"])
    if status == 200 and isinstance(body, dict) and required_keys.issubset(body.keys()):
        print(f"[PASS] 3. Rôle ADMIN autorisé (HTTP 200)")
    else:
        print(f"[FAIL] 3. Échec prédiction ADMIN : status={status}, body={body}")
        all_passed = False

    # 4. Même joueur (player1Id == player2Id) -> HTTP 400 (InvalidMatchException)
    status, body = call_predict(user_token, p1["id"], p1["id"])
    if status == 400:
        error_code = body.get("errorCode") if isinstance(body, dict) else ""
        print(f"[PASS] 4. Auto-affrontement (player1Id == player2Id) rejeté avec HTTP 400")
        print(f"       Code d'erreur reçu : {error_code}, Message : {body.get('message')}")
    else:
        print(f"[FAIL] 4. Auto-affrontement attendait 400, reçu {status} : {body}")
        all_passed = False

    # 5. Joueur inexistant -> HTTP 404 (PlayerNotFoundException)
    status, body = call_predict(user_token, 999999, p2["id"])
    if status == 404:
        print(f"[PASS] 5. Joueur inexistant rejeté avec HTTP 404")
    else:
        print(f"[FAIL] 5. Joueur inexistant attendait 404, reçu {status} : {body}")
        all_passed = False

    print("=" * 80)
    if all_passed:
        print("  TOUS LES TESTS DE L'ENDPOINT ONT RÉUSSI (100% SUCCÈS) !")
    else:
        print("  CERTAINS TESTS ONT ÉCHOUÉ.")
    print("=" * 80)

    if not all_passed:
        sys.exit(1)

if __name__ == "__main__":
    main()
