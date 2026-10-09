# SmartBank-App

Application bancaire desktop de démonstration réalisée avec Java 17, JavaFX et Maven.

## Structure

```text
bank_app/
├── docs/                 Documentation d'intégration Java/JavaFX
├── src/main/java/        Code source Java
├── src/main/resources/   FXML, configuration et scripts SQL
├── .gitignore
└── pom.xml
```

## Prérequis

- JDK 17 ou supérieur
- Maven
- MySQL ou MariaDB pour la base de données

## Compilation et lancement

Depuis ce répertoire :

```bash
mvn clean package
mvn javafx:run
```

Les paramètres de connexion par défaut se trouvent dans `src/main/resources/app.properties`.
Adaptez-les à votre installation locale avant de lancer l'application.

## Documentation

- [Cours pratique Java & JavaFX pour l'intégrateur](docs/Java%20%26%20JavaFX%20pour%20l'int%C3%A9gration%20d'une%20application%20bancaire%20desktop.md)
