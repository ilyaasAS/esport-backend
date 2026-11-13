#!/usr/bin/env python3
"""
Script de validation RBAC et Robustesse du Parser Naturel Oracle.
"""

import urllib.request
import urllib.error
import json
import sys
import time

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

def send_oracle_chat(token, message):
    data = json.dumps({"message": message}).encode("utf-8")
    req = urllib.request.Request(
        f"{BASE_URL}/oracle/chat",
        data=data,
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}",
            "Accept": "text/event-stream"
        }
    )
    try:
        with urllib.request.urlopen(req) as resp:
            status_code = resp.status
            tokens = []
            for line in resp:
                decoded = line.decode("utf-8")
                if decoded.startswith("data:"):
                    content = decoded[5:].rstrip("\r\n")
                    tokens.append(content)
            return status_code, "".join(tokens)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8")

def main():
    print("=" * 80)
    print("  TESTS DE VALIDATION CIBLÉS : RBAC & PARSER NATUREL ORACLE")
    print("=" * 80)

    admin_token = get_token("admin", "admin123")
    user_token = register_user("normal_user", "UserPass123!")
    print(f"[OK] Token ADMIN et USER récupérés avec succès.")

    suffix = str(int(time.time() * 1000) % 100000)
    nick1 = f"Imane_{suffix}"
    nick2 = f"Zephyr_{suffix}"

    tests = [
        # 1. Création de joueur par USER (Désormais AUTORISÉE)
        {
            "name": "1. USER crée un joueur avec faute 'jouer', 'qui s'appel' et 'niv 50'",
            "token": user_token,
            "role": "USER",
            "message": f"Créer un jouer qui s'appel {nick1} niv 50",
            "check": lambda res: nick1 in res and "50" in res and "intégré" in res
        },
        {
            "name": "2. USER crée un joueur avec 'créer [Pseudo] niv 50'",
            "token": user_token,
            "role": "USER",
            "message": f"créer {nick1}Bis niv 50",
            "check": lambda res: f"{nick1}Bis" in res and "intégré" in res
        },
        {
            "name": "3. USER crée un joueur sans niveau spécifié (défaut = 1)",
            "token": user_token,
            "role": "USER",
            "message": f"Inscris le joueur {nick2}",
            "check": lambda res: nick2 in res and "1" in res and "intégré" in res
        },

        # 2. Création de match par USER (STRICTEMENT INTERDITE)
        {
            "name": "4. USER tente d'enregistrer un match standard -> REFUS",
            "token": user_token,
            "role": "USER",
            "message": "Enregistre un matche entre ShadowStrike et Viper 10 à 5",
            "check": lambda res: "autorité nécessaire" in res
        },
        {
            "name": "5. USER tente d'enregistrer un match formulation victoire -> REFUS",
            "token": user_token,
            "role": "USER",
            "message": "Tu peux créer un match avec ShadowStrike et Viper. ShadowStrike gagne 10 à 5",
            "check": lambda res: "autorité nécessaire" in res
        },
        {
            "name": "6. USER tente formulation directe 'gagne contre' -> REFUS",
            "token": user_token,
            "role": "USER",
            "message": "ShadowStrike gagne 10 à 5 contre Viper",
            "check": lambda res: "autorité nécessaire" in res
        },

        # 3. Création de match par ADMIN (AUTORISÉE avec calcul vainqueur / score max)
        {
            "name": "7. ADMIN crée match format victoire 'Tu peux créer un match avec J1 et J2. J1 gagne X à Y'",
            "token": admin_token,
            "role": "ADMIN",
            "message": "Tu peux créer un match avec ShadowStrike et Viper. ShadowStrike gagne 10 à 5",
            "check": lambda res: "enregistré avec un score de [10-5]" in res and "ShadowStrike" in res and "Viper" in res
        },
        {
            "name": "8. ADMIN crée match format victoire avec score inversé 'J2 gagne 4 à 9'",
            "token": admin_token,
            "role": "ADMIN",
            "message": "Tu peux créer un matche avec ShadowStrike et Viper. Viper gagne 4 à 9",
            "check": lambda res: "enregistré avec un score de [9-4]" in res and "Viper" in res and "ShadowStrike" in res
        },
        {
            "name": "9. ADMIN crée match direct 'J1 gagne Score1 à Score2 contre J2'",
            "token": admin_token,
            "role": "ADMIN",
            "message": "ShadowStrike gagne 15 à 7 contre Viper",
            "check": lambda res: "enregistré avec un score de [15-7]" in res and "ShadowStrike" in res and "Viper" in res
        },

        # 4. Tolérance orthographique 'matche'
        {
            "name": "10. ADMIN crée un matche avec 'matche' standard",
            "token": admin_token,
            "role": "ADMIN",
            "message": "Enregistre un matche entre ShadowStrike et Viper 8 à 6",
            "check": lambda res: "enregistré avec un score de [8-6]" in res
        },

        # 5. Pas de faux positif de confidentialité sur chiffres + 'à'
        {
            "name": "11. Absence de message de confidentialité sur scores avec préposition 'à'",
            "token": admin_token,
            "role": "ADMIN",
            "message": "Match ShadowStrike contre Viper score 12 à 10",
            "check": lambda res: "confidentiels" not in res and "enregistré" in res
        },

        # 6. Lectures & Prédictions ouvertes à USER
        {
            "name": "12. USER consulte le classement des joueurs",
            "token": user_token,
            "role": "USER",
            "message": "Affiche le classement des joueurs",
            "check": lambda res: len(res) > 20 and "autorité" not in res
        },
        {
            "name": "13. USER demande un pronostic entre deux joueurs",
            "token": user_token,
            "role": "USER",
            "message": "Prédis le résultat d'un match entre ShadowStrike et Viper",
            "check": lambda res: "[PRONOSTIC OFFICIEL]" in res and "favori" in res
        },
        {
            "name": "14. Pronostic auto-affrontement refusé",
            "token": user_token,
            "role": "USER",
            "message": "Prédis le résultat d'un match entre ShadowStrike et ShadowStrike",
            "check": lambda res: "ne peut pas s'affronter lui-même" in res
        }
    ]

    all_passed = True
    for t in tests:
        status, response = send_oracle_chat(t["token"], t["message"])
        ok = (status == 200) and t["check"](response)
        if ok:
            print(f"[PASS] {t['name']}")
        else:
            print(f"[FAIL] {t['name']}")
            print(f"       Status: {status}")
            print(f"       Réponse: {response}")
            all_passed = False

    print("=" * 80)
    if all_passed:
        print("  TOUS LES TESTS ONT RÉUSSI (100% SUCCÈS) !")
    else:
        print("  CERTAINS TESTS ONT ÉCHOUÉ.")
    print("=" * 80)

    if not all_passed:
        sys.exit(1)

if __name__ == "__main__":
    main()
