# Guide Utilisateur : Agent de Tri d'Actualités

## 1. Objectif de l'application

Bienvenue ! Cette application est un **agent intelligent** conçu pour vous aider à filtrer et trier les dernières actualités.

Elle récupère les articles depuis les flux RSS du journal *Le Monde* et utilise un modèle d'intelligence artificielle (Ollama `qwen2.5:3b`) pour analyser et catégoriser chaque article.

En fonction des **préférences** que vous définissez (votre intérêt pour la politique, le sport, la tech, etc.), l'agent vous présentera une liste d'articles triés, correspondant parfaitement à ce que vous souhaitez lire.

## 2. Lancement de l'application (Prérequis)

La première chose a avoir un JDK installé dans une version supérieur a 17 et un version de docker comprenant docker compose.

1.  Ouvrez un terminal sur votre machine.
2.  Naviguez jusqu'au dossier racine du projet (là où se trouve le fichier `docker-compose.yml`).
3.  Exécutez la commande de lancement appropriée :
    * **Pour le tout premier lancement** (afin de construire l'image de l'application) :
        ```bash
        sudo docker compose up --build
        ```
    * **Pour les lancements suivants** (l'image étant déjà construite) :
        ```bash
        sudo docker compose up -d
        ```
4.  **Note importante :** Lors du premier lancement, le service `ollama-init` téléchargera le modèle `qwen2.5:3b`. Cette opération peut prendre plusieurs minutes en fonction de votre connexion.
5.  Une fois les conteneurs démarrés, l'application est accessible dans votre navigateur à l'adresse : **[http://localhost:8080](http://localhost:8080)**

## 3. Utiliser l'interface de préférences

En ouvrant [http://localhost:8080](http://localhost:8080), vous accédez à l'interface principale.

#### Étape 1 : Définir vos intérêts

L'écran principal affiche 12 thèmes d'actualité. Pour chacun, vous disposez d'un curseur allant de 1 à 5.

Utilisez ces curseurs pour indiquer votre niveau d'intérêt. Voici ce que signifient les niveaux :

* **Niveau 5 (Très important) :** Donne un poids positif **très élevé** (+5) aux articles de ce thème.
* **Niveau 4 (Important) :** Donne un poids positif **élevé** (+3).
* **Niveau 3 (Neutre) :** Donne un petit poids positif (+1).
* **Niveau 2 (Peu d'intérêt) :** Donne un poids **négatif** (-1).
* **Niveau 1 (Pas d'intérêt) :** Donne un poids **très négatif** (-5).

> **💡 Astuce :** Mettre un thème à 1 ou 2 va activement **filtrer et cacher** les articles de ce thème. Mettre un thème à 4 ou 5 les fera **remonter en priorité**.

#### Étape 2 : Lancer l'analyse

Une fois vos préférences réglées, cliquez sur le bouton principal :
**⚡ Récupérer les actualités**

Un indicateur de chargement apparaîtra. Pendant ce temps, l'agent effectue les actions suivantes en arrière-plan :

1.  Il récupère le dernier flux RSS "À la une" du Monde (`LeMondeRSSFetcher.java`).
2.  Il envoie les articles au modèle IA `qwen2.5:3b` pour qu'ils soient évalués et catégorisés selon les 12 thèmes (`PreferencesApi.java`).
3.  Il calcule un "score de pertinence" pour chaque article en comparant les catégories trouvées par l'IA avec les poids que vous avez définis.
4.  Il trie la liste finale et ne conserve que les articles ayant un score positif.

#### Étape 3 : Consulter les résultats

Une fois l'analyse terminée, une fenêtre **"Résultats — Articles recommandés"** s'ouvrira.

Vous y verrez la liste des articles triés pour vous. Pour chaque article, l'interface affiche :

* **Le titre :** Un lien cliquable qui ouvre l'article original sur le site *Le Monde*.
* **Le résumé :** La description issue du flux RSS.
* **Les badges :**
    * **Thème :** Le thème principal détecté par l'IA (ex: `Politique`, `Sport`).
    * **Tonalité :** La tonalité de l'article (ex: `POSITIVE`, `NEGATIVE`, `NEUTRAL`) également détectée par l'IA (`script.js`).

## 4. Outils additionnels

L'interface propose quelques outils pour vous faciliter la vie :

* **Réinitialiser :** Remet tous les curseurs à la valeur neutre (3).
* **Tout mettre au minimum :** Met tous les curseurs à 1.
* **Aperçu de la pondération :** Ouvre une fenêtre affichant un résumé de vos choix actuels (ex: "Économie: 4").
* **Copier le JSON :** Copie la structure de données JSON brute de vos préférences dans le presse-papiers.

## 5. Arrêter l'application

Lorsque vous avez terminé, vous pouvez arrêter tous les services (l'application Java et Ollama) en retournant dans votre terminal et en exécutant :

```bash
sudo docker compose down