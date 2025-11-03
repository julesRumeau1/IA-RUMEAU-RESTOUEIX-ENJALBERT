# 🧠 NewsSummarizer Agent

## 📌 Objectif du projet
Concevoir un **agent intelligent** qui récupère des **actualités** via des **flux RSS**, filtre selon les **préférences utilisateur** (politique, sport, tech, etc.), puis **résume** les articles pertinents en s’appuyant sur **Ollama** (modèle **qwen2.5:7b**) pour l’inférence locale.

---

## 🚀 Fonctionnalités
- Récupération multi-flux RSS
- Filtrage par thèmes et préférences
- Résumés concis générés via Qwen2.5 3b (Ollama)
- Interface web
- Sauvegarde locale des préférences
- Journalisation et gestion d’erreurs robuste

---

## 🏗️ Architecture
```text
src/
├─ main/
│  ├─ java/
│  │  ├─ api/
│  │  │  ├─ dto/
│  │  │  │  ├─ ErrorResponse.java
│  │  │  │  ├─ PreferencesRequest.java
│  │  │  │  ├─ Themes.java
│  │  │  │  ├─ ThemeSelection.java
│  │  │  │  └─ package-info.java
│  │  │  ├─ util/
│  │  │  │  ├─ CorsUtil.java
│  │  │  │  ├─ PreferencesUtils.java
│  │  │  │  └─ package-info.java
│  │  │  ├─ PreferencesApi.java
│  │  │  └─ package-info.java
│  │  ├─ main/
│  │  │  ├─ Main.java
│  │  │  └─ package-info.java
│  │  ├─ model/
│  │  │  ├─ News.java
│  │  │  ├─ NewsCategoryScore.java
│  │  │  ├─ NewsCollection.java
│  │  │  └─ package-info.java
│  │  └─ rss/
│  │     ├─ LeMondeRSSFetcher.java
│  │     └─ package-info.java
│  ├─ resources/
│  │  └─ public/
│  │     ├─ index.html
│  │     ├─ script.js
│  │     └─ style.css
│  └─ test/
└─ docs/

└─ README.md
```

---

## 🧰 Technologies utilisées
- **Langage principal :** Java
- **Backend IA :** Ollama (`qwen2.5:7b`)
- **Parsing RSS :** Rome (ou équivalent)
- **JSON :** Gson / Jackson
- **Tests :** JUnit
- **Versionning :** Git + GitHub

---

## ⚙️ Installation et exécution
### 1) Prérequis
- Java 17+
- Docker avec docker compose

### 2) Cloner le dépôt
```bash
git clone https://github.com/julesRumeau1/IA-RUMEAU-RESTOUEIX-ENJALBERT.git
cd IA-RUMEAU-RESTOUEIX-ENJALBERT
```

### 3) Télécharger le modèle, lancer ollama et lancer l'application
```bash

1ère utilisation :
sudo docker compose up --build

Après :
sudo docker compose up -d
```


---

## 🧠 Utilisation (flux simple)
1. Lancer l’application.
2. Renseigner vos thèmes d’intérêt sur l'interface web (ex. `politique`, `sport`, `économie`).
3. L’agent récupère les flux RSS, filtre et résume via Qwen2.5 3b.
4. Ajuster les préférences à tout moment.

---


---

## 💡 Exemple d’appel Ollama
```json
{
  "model": "qwen2.5:7b",
  "prompt": "Résume en 3 phrases les actualités du jour sur le thème 'politique'.",
  "stream": false
}
```

---

## ✅ Bonnes pratiques
- Modulariser le code (séparation agent / API / UI)
- Gestion d’erreurs et validations (programmation défensive)
- Commits réguliers, branches par fonctionnalité, pull request GitHub
- Tests unitaires sur les parties critiques (parsing, filtrage)

---

## 👥 Équipe
| Nom               | Rôle                  |
|-------------------|-----------------------|
| Jules RUMEAU      | Dév. & Chef de projet |
| Émilien RESTOUEIX | Dév.                  |
| Anthony ENJALBERT | Dév.                  |

---

## 📅 Jalons (indicatifs)
| Date | Objectif | Délivrable |
|------|----------|------------|
| 2025-10-07 | Idée + planification | Concept, schéma d’archi, dépôt GitHub |
| 2025-10-21 | Prototype minimal | Agent répond via Ollama, README de lancement |
| 2025-11-04 | Soutenance finale | Application stable, démo, doc complète |

---

## 📂 Livrables
- Code source et historique Git propre
- README et guide utilisateur
- Diagrammes UML (`docs/`)
- Présentation de soutenance

---

## 🧾 Licence
Projet académique — usage éducatif.