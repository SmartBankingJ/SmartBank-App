# SmartBank-App

Point d’intégration de SmartBanking : compilation des deux modules, lancement du desktop et documentation commune. Le code JavaFX appartient désormais à `SmartBank-frontend` ; les modèles, JDBC et SQL à `SmartBank-backend`.

## Structure

```text
SmartBankingJ/
├── SmartBank-App/        Agrégateur Maven, scripts et documentation
├── SmartBank-frontend/   Application desktop JavaFX autonome
└── SmartBank-backend/    Modèles métier, configuration et accès MySQL
```

## Prérequis

- JDK 17 ou supérieur
- Maven
- MySQL ou MariaDB pour les futurs parcours persistants ; inutile pour afficher la connexion

## Compilation et lancement

Depuis ce répertoire :

```bash
mvn clean verify
mvn -f ../SmartBank-frontend/pom.xml javafx:run
```

Les trois dépôts doivent être clonés côte à côte, avec les noms ci-dessus. Le POM est un agrégateur, pas un parent obligatoire : chaque module possède son propre POM et peut être compilé indépendamment.

Sous PowerShell, un lanceur est également disponible :

```powershell
.\scripts\run-desktop.cmd
# Avec un SDK JavaFX déjà installé, pour un lancement sans téléchargement :
.\scripts\run-desktop.cmd -JavaHome 'C:\chemin\jdk-20' -JavaFxLib 'C:\chemin\javafx-sdk\lib'
```

Le script utilise aussi le runtime JavaFX local préparé dans `%LOCALAPPDATA%\SmartBankingJ\tools\javafx-runtime` s’il existe. Il compile uniquement les sources du frontend. Sans runtime local, Maven et un JDK 17+ sont nécessaires.

La configuration JDBC est dans `../SmartBank-backend/src/main/resources/app.properties`. Aucun secret SQL et aucune dépendance MySQL ne sont embarqués dans le frontend. Les paramètres actuels du backend restent des valeurs de démonstration.

## État et séparation des responsabilités

- Le frontend vérifie maintenant les identifiants auprès de l’API du backend ; le dashboard est réservé à une session authentifiée. Ses données bancaires restent des exemples.
- Le backend expose une API locale sur `127.0.0.1:8081` pour connexion, sessions, déconnexion, changement de mot de passe et récupération. Voir son README pour la configuration MySQL, la migration et les tests.
- Démarrer le backend avec `../SmartBank-backend/scripts/run-backend.cmd`, puis le frontend avec `scripts/run-desktop.cmd`. Le lanceur desktop n’importe jamais la base et ne démarre pas implicitement un serveur.
- Le frontend ne dépend pas du JAR backend : la communication est HTTP/JSON. Gson est déclaré dans les POM ; le lanceur local accepte aussi `-GsonJar`.
- La documentation technique d’origine reste dans `docs/`. Les livrables personnels générés (cahier des charges, PDF, maquettes et fichiers temporaires associés) sont conservés dans `../Livrables-locaux/`, hors des trois dépôts. Les dossiers `output/`, `target/` et `tmp/` sont ignorés par Git.

## Documentation

- [Cours pratique Java & JavaFX pour l'intégrateur](docs/Java%20%26%20JavaFX%20pour%20l'int%C3%A9gration%20d'une%20application%20bancaire%20desktop.md)
