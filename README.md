# 🧠 NewsSorter Agent

## 📌 Objectif du projet
Concevoir un **agent intelligent** qui récupère des **actualités** via des **flux RSS**, filtre selon les **préférences utilisateur** (politique, sport, tech, etc.) sur **Ollama** (modèle **qwen2.5:7b**) pour l’inférence locale.

---

## 🚀 Fonctionnalités
- Récupération multi-flux RSS
- Filtrage par thèmes et préférences
- Interface web
- Journalisation et gestion d’erreurs robuste

---

## 🏗️ Architecture
```text
.
└── src
    ├── docs
    │   ├── UserManual.md
    │   ├── diagramme_de_classe.(plantuml|png)
    │   └── diagramme_de_sequence.(plantuml|png)
    ├── main
    │   ├── java
    │   │   ├── api
    │   │   │   ├── PreferencesApi.java           
    │   │   │   ├── dto/                         
    │   │   │   │   ├── ErrorResponse.java
    │   │   │   │   ├── PreferencesRequest.java
    │   │   │   │   ├── ThemeSelection.java
    │   │   │   │   └── Themes.java
    │   │   │   ├── service/                    
    │   │   │   │   ├── LLMScorer.java
    │   │   │   │   ├── NewsCollectionFactory.java
    │   │   │   │   ├── NewsService.java
    │   │   │   │   └── NewsSorter.java
    │   │   │   └── util/                       
    │   │   │       ├── ApiException.java
    │   │   │       ├── CorsUtil.java
    │   │   │       └── PreferencesUtils.java
    │   │   ├── model/                        
    │   │   │   ├── News.java
    │   │   │   ├── NewsCategoryScore.java
    │   │   │   └── NewsCollection.java
    │   │   ├── rss/                             
    │   │   │   ├── RssFetcher.java
    │   │   │   └── LeMondeRSSFetcher.java
    │   │   └── main/                            
    │   │       └── Main.java
    │   └── resources
    │       ├── logback.xml                     
    │       └── public/                           
    │           ├── index.html
    │           ├── script.js
    │           └── style.css
    └── test
        └── java
            ├── api/dto/*Test.java
            ├── model/*Test.java
            └── rss/*Test.java
```

---

## 🧰 Technologies utilisées
- **Langage principal :** Java
- **Backend IA :** Ollama (`qwen2.5:7b`)
- **JSON :** Gson / Jackson
- **Tests :** JUnit5
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
3. L’agent récupère les flux RSS, filtre et résume via Qwen2.5 7b.
4. Ajuster les préférences à tout moment.

---

## ✅ Bonnes pratiques
- Modulariser le code (séparation agent / API / UI)
- Gestion d’erreurs et validations (programmation défensive)
- Commits réguliers, branches par fonctionnalité, pull request GitHub
- Tests unitaires

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
---

## 🧾 Licence
Projet académique — usage éducatif.