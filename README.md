# 🧠 NewsSummarizer Agent

## 📌 Objectif du projet
Concevoir un **agent intelligent** qui récupère des **actualités** via des **flux RSS**, filtre selon les **préférences utilisateur** (politique, sport, tech, etc.), puis **résume** les articles pertinents en s’appuyant sur **Ollama** (modèle **Phi4-mini**) pour l’inférence locale.

---

## 🚀 Fonctionnalités
- Récupération multi-flux RSS
- Filtrage par thèmes et préférences
- Résumés concis générés via Mistral (Ollama)
- Interface web
- Sauvegarde locale des préférences
- Journalisation et gestion d’erreurs robuste

---

## 🏗️ Architecture (prévisionnelle)
```text
src/
├─ main/
│  ├─ App.java
│  ├─ agent/
│  │  ├─ NewsAgent.java
│  │  ├─ Preferences.java
│  ├─ api/
│  │  ├─ OllamaClient.java
│  │  └─ RssFetcher.java
│  └─ ui/
│     └─ ConsoleUI.java
└─ test/
   └─ ...
docs/
├─ diagramme_uml.plantuml
└─ README.md
```

---

## 🧰 Technologies utilisées
- **Langage principal :** Java
- **Backend IA :** Ollama (`phi4-mini`)
- **Parsing RSS :** Rome (ou équivalent)
- **JSON :** Gson / Jackson
- **Tests :** JUnit
- **Versionning :** Git + GitHub

---

## ⚙️ Installation et exécution
### 1) Prérequis
- Java 17+
- [Ollama](https://ollama.com) installé et lancé localement

### 2) Cloner le dépôt
```bash
git clone https://github.com/julesRumeau1/IA-RUMEAU-RESTOUEIX-ENJALBERT.git
cd IA-RUMEAU-RESTOUEIX-ENJALBERT
```

### 3) Télécharger le modèle
```bash
ollama pull phi4-mini
```

### 4) Compiler et exécuter (exemple)
```bash
javac -d bin src/main/App.java
java -cp bin main.App
```

---

## 🧠 Utilisation (flux simple)
1. Lancer l’application.
2. Renseigner vos thèmes d’intérêt (ex. `politique`, `sport`, `économie`).
3. L’agent récupère les flux RSS, filtre et résume via Phi4-mini.
4. Ajuster les préférences à tout moment.

---


---

## 💡 Exemple d’appel Ollama
```json
{
  "model": "phi4-mini",
  "prompt": "Résume en 3 phrases les actualités du jour sur le thème 'politique'.",
  "stream": false
}
```

---

## ✅ Bonnes pratiques
- Modulariser le code (séparation agent / API / UI)
- Gestion d’erreurs et validations (programmation défensive)
- Commits réguliers, branches par fonctionnalité, issues GitHub
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