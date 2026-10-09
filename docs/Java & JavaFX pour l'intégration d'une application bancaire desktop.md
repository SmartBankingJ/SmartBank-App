# Java & JavaFX pour l'intégration d'une application bancaire desktop

*Cours pratique pour l'intégrateur — simulation académique de BMCE Direct (BANK OF AFRICA)*

## 0. Comment lire ce cours

Ce cours ne réexplique pas la syntaxe de base. Il t'apprend à **raisonner sur le projet** : qui appelle qui, quelle donnée circule, où un module doit se brancher.

Pour chaque concept important, le plan est : **pourquoi → où dans le projet → qui l'appelle → qui il appelle → circulation des données → syntaxe → exemple bancaire → erreurs à éviter.**

Hypothèses techniques (à valider avec l'équipe, c'est ton premier travail d'intégrateur) :

- Java 17 ou 21, JavaFX 21, Maven.
- Base MySQL/MariaDB (XAMPP) ou PostgreSQL, accès par **JDBC** (voir section 11 pour JDBC vs JPA).
- Package racine : `ma.bankdirect` (remplace par celui que l'équipe fixera).
- Pour BMCE Direct : seules les fonctionnalités publiques sont prises comme référence. Chaque fois qu'un détail d'interface n'est pas vérifiable, je l'indique : **« À adapter selon l'observation de l'application réelle. »**

---

## 1. Architecture en couches : le raisonnement de base

### 1.1 Le chemin aller et le chemin retour

```
ALLER (une action utilisateur)
View (FXML)  →  Controller  →  Service  →  DAO  →  JDBC  →  Database

RETOUR (le résultat)
Database  →  ResultSet  →  DAO (mapping en Model)  →  Service (règles métier)
          →  Controller  →  View (TableView, Label, Dialog...)
```

Les objets qui circulent sont des **Models** (Account, Transaction...) ou des petits objets de requête/réponse (DTO, `record`). Jamais de `ResultSet` en dehors du DAO, jamais de `Node` JavaFX en dehors du Controller.

### 1.2 Responsabilité de chaque couche

| Couche | Responsabilité unique | Exemple |
| --- | --- | --- |
| **View (FXML + CSS)** | Décrire l'apparence et déclarer les composants | `transfer.fxml` |
| **Controller** | Lire les champs, appeler un service, afficher le résultat ou l'erreur, demander une navigation | `TransferController` |
| **Service** | Règles métier, validation, orchestration, frontière de transaction | `TransferService` |
| **DAO** | Accès aux données : SQL, mapping ligne → objet | `AccountDAO`, `TransactionDAO` |
| **Model** | Représenter les données métier, sans logique d'accès | `Account`, `Transaction` |
| **Session** | Mémoriser l'utilisateur connecté | `SessionManager` |
| **Navigation** | Changer d'écran | `Navigator` |
| **Config / DB** | Paramètres, fournisseur de connexions | `AppConfig`, `Database` |

### 1.3 Pourquoi une couche ne doit pas faire le travail d'une autre

- **Controller qui fait du SQL** : impossible à tester, SQL dupliqué dans 5 écrans, et un changement de colonne casse toute l'application.
- **Controller qui calcule le solde** : la règle « solde suffisant » existera dans deux écrans, avec deux comportements différents.
- **DAO qui connaît JavaFX** : le DAO devient inutilisable hors de l'interface (tests, import de données).
- **Service qui ouvre une fenêtre** : le service ne peut plus être appelé en tâche de fond ni testé.
- **Deux DAO qui font chacun leur `commit`** : un virement peut débiter A sans créditer B (section 12).

### 1.4 Erreurs d'architecture typiques d'un étudiant

1. `DriverManager.getConnection(...)` écrit dans chaque Controller.
2. Un « God Controller » de 1 500 lignes qui gère login, dashboard et virement.
3. Un Controller qui appelle directement un autre Controller (`DashboardController.getInstance().refresh()`).
4. `double` utilisé pour l'argent (erreurs d'arrondi) au lieu de `BigDecimal`.
5. Logique métier dans le `initialize()` du Controller.
6. Chaque membre qui crée sa propre classe `User` ou sa propre variable statique de session.
7. Chaque module qui crée son propre `Stage` pour naviguer.
8. Chemins FXML écrits en dur et différents d'un module à l'autre.
9. Exceptions avalées (`catch (Exception e) {}`) : l'écran affiche un succès alors que la DB a échoué.
10. Mots de passe stockés en clair.

**Règle d'or de l'intégrateur** : toute dépendance doit aller *vers le bas* (View → Controller → Service → DAO → DB). Si tu vois une flèche qui remonte ou qui va de côté entre deux Controllers, c'est un défaut d'architecture à corriger.

---

## 2. Structure du projet et « qui appelle qui »

### 2.1 Arborescence recommandée

```
bankdirect/
├── pom.xml
└── src/main/
    ├── java/
    │   ├── module-info.java            (optionnel, voir section 16)
    │   └── ma/bankdirect/
    │       ├── MainApp.java            point d'entrée JavaFX (Application)
    │       ├── Launcher.java           main() qui lance MainApp (utile pour le jar)
    │       ├── config/                 AppConfig, lecture de app.properties
    │       ├── db/                     Database, TransactionManager
    │       ├── model/                  User, Customer, Account, Transaction...
    │       │   └── enums/              AccountType, TransactionType, TransactionStatus...
    │       ├── dao/                    interfaces : AccountDAO, UserDAO...
    │       │   └── jdbc/               JdbcAccountDAO, JdbcUserDAO...
    │       ├── service/                AuthService, TransferService...
    │       ├── session/                SessionManager
    │       ├── navigation/             Navigator, Route, NavigationParams
    │       ├── controller/
    │       │   ├── auth/               LoginController
    │       │   ├── dashboard/          DashboardController
    │       │   ├── account/            AccountListController, AccountDetailController
    │       │   ├── transfer/           TransferController, BeneficiaryController
    │       │   ├── card/ bill/ document/ profile/ support/
    │       │   └── shell/              MainShellController (cadre de l'application)
    │       ├── util/                   Money, Formatters, Validators, Dialogs
    │       └── exception/              BusinessException, InsufficientFundsException...
    └── resources/
        ├── app.properties              url DB, user (jamais de mot de passe réel dans Git)
        ├── fxml/                       login.fxml, shell.fxml, dashboard.fxml...
        ├── css/                        theme.css
        ├── images/
        └── sql/                        schema.sql, seed.sql
```

**Convention de ressources** : tous les chemins FXML sont absolus depuis la racine du classpath, par exemple `/fxml/transfer/transfer.fxml`. Un seul style, décidé par l'intégrateur.

### 2.2 Tableau « qui appelle qui »

| Couche | Appelée par | Reçoit | Retourne | Peut utiliser | Ne doit PAS utiliser |
| --- | --- | --- | --- | --- | --- |
| **Controller** | FXMLLoader, Navigator | Événements UI, paramètres de navigation | `void` (met à jour la vue) | Services, SessionManager, Navigator, util, Models | DAO, JDBC, `Connection`, autres Controllers |
| **Service** | Controllers, autres Services | Models, identifiants, montants (`BigDecimal`) | Models, listes, `record` de résultat | DAO, TransactionManager, autres Services, exceptions | Classes `javafx.*` (sauf `javafx.concurrent` si assumé), Controllers |
| **DAO** | Services | Models, identifiants, `Connection` (cas transaction) | Models, `Optional<Model>`, `List<Model>` | JDBC, Database, Models | Services, Controllers, JavaFX |
| **Model** | Tout le monde | — | — | Enums, types Java | DAO, Services, JavaFX |
| **SessionManager** | Controllers, Services (lecture) | Utilisateur après login | Utilisateur courant | Models | DAO |
| **Navigator** | Controllers | Une `Route` + paramètres | `void` | FXMLLoader, Stage principal | Services métier |
| **Database** | DAO, TransactionManager | — | `Connection` | AppConfig | Tout le reste |

Si un membre de l'équipe t'envoie du code, ce tableau est ta grille de relecture : une colonne « Ne doit PAS utiliser » violée = demande de correction avant merge.

---

## 3. Modélisation métier bancaire

### 3.1 Méthode : besoin → entités → relations → services → DAO → écrans

Exemple avec le besoin « Je veux voir l'historique de mon compte » :

1. **Besoin** : lister les opérations d'un compte, filtrer par période.
2. **Entités** : `Account`, `Transaction`.
3. **Relations** : un Account a plusieurs Transactions (1—N).
4. **Service** : `AccountService.getTransactions(accountId, from, to)`, qui vérifie que le compte appartient bien au client connecté.
5. **DAO** : `TransactionDAO.findByAccountAndPeriod(accountId, from, to)`.
6. **Écran** : `transaction-history.fxml` + `TransactionHistoryController` (TableView).

Refais cette chaîne pour **chaque** fonctionnalité du cahier des charges : tu obtiens la liste complète des classes à exiger de chaque développeur.

### 3.2 Relations entre entités

```
User (1) ────── (1) Customer (1) ────── (N) Account (1) ────── (N) Transaction
                      │                      │
                      │                      └── (N) Card
                      ├── (N) Beneficiary
                      ├── (N) Notification
                      ├── (N) Document
                      ├── (N) BillPayment ── (1) Bill
                      └── (0..1) Advisor
```

| Entité | Rôle | Champs principaux |
| --- | --- | --- |
| `User` | Identifiants de connexion | id, username, passwordHash, role, status, failedAttempts, customerId |
| `Customer` | Personne physique cliente | id, firstName, lastName, cin, email, phone, address, advisorId |
| `Account` | Compte bancaire | id, customerId, rib, type, currency, balance, status, openedAt |
| `Transaction` | Une écriture sur un compte | id, accountId, type, direction, amount, label, createdAt, status, reference, counterpartRib |
| `Beneficiary` | Destinataire enregistré | id, customerId, fullName, rib, bankName, createdAt |
| `Card` | Carte liée à un compte | id, accountId, maskedNumber, type, status, expiry, dailyLimit, onlinePaymentEnabled |
| `Bill` / `BillPayment` | Facture à payer / paiement effectué | creditor, reference, amount, dueDate / billId, accountId, paidAt, status |
| `Notification` | Information destinée au client | id, customerId, title, body, createdAt, read |
| `Document` | Relevé ou document consultable | id, customerId, accountId, type, period, filePath |
| `Advisor` | Conseiller | id, fullName, phone, email, agency |
| `Session` | **Pas en base** : objet en mémoire (section 10) | user, customer, loginTime |

### 3.3 Données stockées ou calculées

| Donnée | Choix | Justification |
| --- | --- | --- |
| Solde du compte (`balance`) | **Stocké** et mis à jour dans la même transaction DB que l'opération | Lecture rapide ; la table `transactions` sert de preuve |
| Solde après opération (`balanceAfter`) | Stocké dans la Transaction | Historique cohérent même plus tard |
| Total de tous les comptes (dashboard) | **Calculé** (somme côté Service) | Dérivé, ne jamais le stocker |
| Numéro de carte complet | Ne pas l'afficher : stocker/afficher `maskedNumber` | Simulation, mais bonne pratique |
| Nom complet | **Calculé** (`firstName + lastName`) | Évite l'incohérence |
| Facture « en retard » | **Calculé** (`dueDate` < aujourd'hui et statut impayé) | Dépend de la date |
| Nombre de notifications non lues | **Calculé** (`COUNT`) | Dérivé |

Règle : si une valeur peut être recalculée à partir d'autres colonnes sans risque, ne la stocke pas. Le solde est l'exception justifiée, à condition de ne le modifier **que** via le service de transaction.

### 3.4 Enums du domaine

```java
public enum AccountType { CURRENT, SAVINGS, PREPAID }
public enum TransactionType { INTERNAL_TRANSFER, BENEFICIARY_TRANSFER, BILL_PAYMENT, CARD_TOPUP, DEPOSIT, WITHDRAWAL }
public enum Direction { DEBIT, CREDIT }
public enum TransactionStatus { PENDING, COMPLETED, FAILED, CANCELLED }
public enum CardStatus { ACTIVE, BLOCKED, EXPIRED }
```

Ces enums sont **dans le package `model.enums`, créés une seule fois par l'intégrateur**. En base, on les stocke en `VARCHAR` (nom de l'enum), pas en numéro d'ordre : changer l'ordre dans le code ne corrompt alors aucune donnée.

### 3.5 Schéma SQL minimal (contrat de base de données)

```sql
CREATE TABLE customers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  first_name VARCHAR(80) NOT NULL, last_name VARCHAR(80) NOT NULL,
  cin VARCHAR(20) UNIQUE, email VARCHAR(120), phone VARCHAR(30), address VARCHAR(255),
  advisor_id BIGINT NULL
);
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(60) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  failed_attempts INT NOT NULL DEFAULT 0,
  customer_id BIGINT NOT NULL,
  FOREIGN KEY (customer_id) REFERENCES customers(id)
);
CREATE TABLE accounts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  rib VARCHAR(30) NOT NULL UNIQUE,
  type VARCHAR(20) NOT NULL, currency CHAR(3) NOT NULL DEFAULT 'MAD',
  balance DECIMAL(15,2) NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  opened_at DATETIME NOT NULL,
  FOREIGN KEY (customer_id) REFERENCES customers(id)
);
CREATE TABLE transactions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL, direction VARCHAR(10) NOT NULL,
  amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
  balance_after DECIMAL(15,2) NOT NULL,
  label VARCHAR(255), reference VARCHAR(40) NOT NULL,
  counterpart_rib VARCHAR(30), status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL,
  FOREIGN KEY (account_id) REFERENCES accounts(id)
);
CREATE TABLE beneficiaries (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT NOT NULL, full_name VARCHAR(120) NOT NULL,
  rib VARCHAR(30) NOT NULL, bank_name VARCHAR(80),
  created_at DATETIME NOT NULL,
  UNIQUE (customer_id, rib),
  FOREIGN KEY (customer_id) REFERENCES customers(id)
);
```

Les tables `cards`, `bills`, `bill_payments`, `notifications`, `documents`, `advisors` suivent le même modèle (clé étrangère vers `customers` ou `accounts`). **Un seul `schema.sql` dans `resources/sql/`, propriété de l'intégrateur.** Toute modification passe par une demande de merge sur ce fichier.

Un virement produit **deux lignes** dans `transactions` : un `DEBIT` sur le compte source et un `CREDIT` sur le compte destination (si le destinataire est interne), avec la même `reference`. Pour un bénéficiaire externe fictif, seule la ligne `DEBIT` existe.

---

## 4. Flux complets

### 4.1 Le virement vers un bénéficiaire, étape par étape

| # | Étape | Couche | Appel technique | Donnée qui circule |
| --- | --- | --- | --- | --- |
| 1 | Clic sur « Virement » dans le menu | View → Controller | `@FXML onTransferMenu()` du `MainShellController` | événement |
| 2 | Demande d'ouverture de l'écran | Controller → Navigator | `Navigator.go(Route.TRANSFER)` | `Route` |
| 3 | Chargement du FXML | Navigator | `FXMLLoader.load()` puis `shell.setCenter(root)` | Parent |
| 4 | `initialize()` du `TransferController` | Controller | lance un `Task` (section 14) | — |
| 5 | Récupération des comptes | Controller → Service → DAO | `accountService.getAccountsOf(session.customerId())` → `accountDAO.findByCustomerId(id)` | `List<Account>` |
| 6 | Récupération des bénéficiaires | Controller → Service → DAO | `beneficiaryService.getFor(customerId)` → `beneficiaryDAO.findByCustomerId(id)` | `List<Beneficiary>` |
| 7 | Remplissage des ComboBox | Controller | `accountCombo.setItems(FXCollections.observableList(accounts))` | ObservableList |
| 8 | Sélection compte, bénéficiaire, saisie du montant | View | listeners, `TextField` | saisie texte |
| 9 | Clic « Valider » : validation de forme | Controller | montant non vide, parsable, > 0 | `BigDecimal` |
| 10 | Écran de confirmation | Controller → Navigator | `Navigator.go(Route.TRANSFER_CONFIRM, params)` | `TransferRequest` |
| 11 | Confirmation (code simulé) | Controller | saisie du code, appel `securityService.verifyOtp(...)` | code |
| 12 | Appel du service métier | Controller → Service | `transferService.transferToBeneficiary(request)` | `TransferRequest` (record) |
| 13 | Règles métier | Service | compte actif ? propriétaire ? solde suffisant ? plafond ? bénéficiaire du client ? | Models |
| 14 | Ouverture de la transaction DB | Service → TransactionManager | `tx.execute(conn -> {...})` | `Connection` |
| 15 | Débit, insertion de l'écriture | Service → DAO | `accountDAO.debit(conn, id, amount)` ; `transactionDAO.insert(conn, tx)` | `Connection`, Models |
| 16 | `COMMIT` ou `ROLLBACK` | TransactionManager | `conn.commit()` / `conn.rollback()` | — |
| 17 | Création d'une notification | Service | `notificationService.push(...)` | `Notification` |
| 18 | Retour du résultat | Service → Controller | retourne `TransferReceipt` ou lève `BusinessException` | record |
| 19 | Écran de succès | Controller → Navigator | `Navigator.go(Route.TRANSFER_SUCCESS, receipt)` | `TransferReceipt` |
| 20 | Retour dashboard / historique | Navigator | `Route.DASHBOARD` : son `initialize()` **recharge** les données | — |

**Point d'intégration crucial (étapes 18-20)** : le Dashboard et l'Historique ne sont pas « mis à jour par le TransferController ». Ils se **rechargent eux-mêmes** à chaque affichage (la navigation recrée le Controller et appelle `initialize()`). Aucun Controller n'appelle un autre Controller.

### 4.2 Le login complet

```
login.fxml (PasswordField + Button onAction="#onLogin")
  → LoginController.onLogin()
     lit username/password, désactive le bouton, affiche ProgressIndicator
  → AuthService.login(username, password)            [dans un Task]
     → UserDAO.findByUsername(username)  → Optional<User>
     → vérifie statut, tentatives, hash (BCrypt ou SHA-256 + sel pour le projet)
     → CustomerDAO.findById(user.customerId())
     → retourne AuthResult(user, customer)
  ← LoginController (succès) : SessionManager.start(user, customer)
                               Navigator.showShell(); Navigator.go(Route.DASHBOARD)
  ← LoginController (échec) : affiche le message d'une BusinessException
```

Le `LoginController` ne connaît ni la table `users` ni l'algorithme de hash. Le `DashboardController` ne reçoit **pas** l'utilisateur en paramètre : il le lit dans `SessionManager` (section 10).

### 4.3 Vue synthétique des 15 flux

| # | Flux | Controller (déclencheur) | Service | DAO | Tables | Écran suivant |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Login | LoginController | AuthService | UserDAO, CustomerDAO | users, customers | Dashboard |
| 2 | Logout | MainShellController | AuthService (journal) | — | — | Login (+ `SessionManager.clear()`) |
| 3 | Consultation compte | AccountListController / AccountDetailController | AccountService | AccountDAO | accounts | Détail compte |
| 4 | Historique | TransactionHistoryController | AccountService / TransactionService | TransactionDAO | transactions | Détail opération |
| 5 | Virement interne | TransferController | TransferService | AccountDAO, TransactionDAO | accounts, transactions | Confirmation → Succès |
| 6 | Virement bénéficiaire | TransferController | TransferService | AccountDAO, BeneficiaryDAO, TransactionDAO | accounts, beneficiaries, transactions | Confirmation → Succès |
| 7 | Ajout bénéficiaire | BeneficiaryFormController | BeneficiaryService | BeneficiaryDAO | beneficiaries | Liste / Virement |
| 8 | Paiement facture | BillPaymentController | BillService | BillDAO, AccountDAO, TransactionDAO | bills, bill\_payments, accounts, transactions | Reçu |
| 9 | Recharge carte prépayée | CardTopupController | CardService | CardDAO, AccountDAO, TransactionDAO | cards, accounts, transactions | Reçu |
| 10 | Gestion carte (bloquer, plafonds, paiement en ligne) | CardManageController | CardService | CardDAO | cards | Détail carte |
| 11 | Consultation document | DocumentListController | DocumentService | DocumentDAO | documents | Visionneuse / export |
| 12 | Modification profil | ProfileController | ProfileService | CustomerDAO, UserDAO | customers, users | Profil |
| 13 | Confirmation d'opération | ConfirmController (réutilisable) | SecurityService | — (ou OtpDAO) | — | Retour à l'opération appelante |
| 14 | Notification | NotificationController / bandeau du shell | NotificationService | NotificationDAO | notifications | Détail ou écran concerné |
| 15 | Retour dashboard | n'importe quel Controller | — | — | — | `Navigator.go(Route.DASHBOARD)` |

Ce tableau est ton **plan d'intégration** : une ligne = un parcours à tester de bout en bout, et un contrat de classes à exiger du développeur responsable.

### 4.4 Le flux « confirmation d'une opération sensible »

Plutôt que 5 écrans de confirmation différents, définis **une seule** boîte de confirmation réutilisable (une `Dialog` modale ou une route `CONFIRM`) qui reçoit un `PendingOperation` (libellé, montant, `Supplier` ou `Runnable` à exécuter après validation) et le renvoie confirmé ou annulé. Le code OTP est simulé dans `SecurityService` (par exemple code fixe en développement, généré en production simulée). **À adapter selon l'observation de l'application réelle** pour le mécanisme exact de confirmation de BMCE Direct.

---

## 5. Java : ce qui est vraiment nécessaire dans ce projet

| Notion | Pourquoi ici | Où elle intervient | Fichiers qui la partagent |
| --- | --- | --- | --- |
| `BigDecimal` | L'argent ne se calcule jamais en `double` | `Account.balance`, `Transaction.amount`, `TransferService` | Models, Services, DAO, Controllers (affichage) |
| `enum` | Valeurs fermées (type, statut) | `TransactionType`, `CardStatus` | Models, DAO (mapping), Services |
| `record` | DTO immuables entre couches | `TransferRequest`, `TransferReceipt`, `AuthResult` | Controller → Service → Controller |
| `interface` | Contrat DAO/Service, subère les tests | `AccountDAO` / `JdbcAccountDAO` | Services (dépendent de l'interface) |
| Exceptions personnalisées | Transmettre une erreur métier à l'écran | `InsufficientFundsException` | Service (lance), Controller (affiche) |
| `Optional` | « Peut ne rien trouver » dans un DAO | `findByUsername` | DAO → Service |
| Collections / generics | Listes de comptes, de transactions | `List<Account>`, `Map<Long, Account>` | Partout |
| Streams / lambdas | Totaux, filtres, listeners JavaFX | Total des soldes, `setOnAction(e -> ...)` | Services, Controllers |
| `java.time` | Dates d'opération, périodes de relevé | `LocalDateTime createdAt`, `DatePicker` | Models, DAO (conversion SQL), vue |
| UUID / références | Référence unique d'un virement | `Transaction.reference` | TransferService |
| try-with-resources | Fermer Connection/Statement/ResultSet | tous les DAO | DAO, TransactionManager |
| Logging | Diagnostiquer sans `System.out` | Services, DAO | Tout |
| Fichier de configuration | Ne pas coder l'URL DB en dur | `app.properties` | `AppConfig` → `Database` |
| Threads (`Task`) | Ne pas geler l'interface | Controllers | Controller → Service (section 14) |

### 5.1 BigDecimal et la classe `Money`

```java
public final class Money {
    private Money() {}
    public static final int SCALE = 2;

    public static BigDecimal of(String text) {
        // accepte "1 250,50" et "1250.50"
        String clean = text.trim().replace(" ", "").replace(',', '.');
        return new BigDecimal(clean).setScale(SCALE, RoundingMode.HALF_UP);
    }
    public static boolean isPositive(BigDecimal a) { return a.signum() > 0; }
    public static String format(BigDecimal a) {
        return NumberFormat.getNumberInstance(Locale.FRANCE).format(a) + " MAD";
    }
}
```

Erreurs à éviter : `new BigDecimal(12.1)` (utilise toujours le constructeur `String`), `a == b` ou `a.equals(b)` pour comparer des montants (utilise `compareTo`), `double` dans un Model.

### 5.2 Enums, records, exceptions

```typescript
public record TransferRequest(long fromAccountId, Long toAccountId, Long beneficiaryId,
                              BigDecimal amount, String label) {}

public record TransferReceipt(String reference, BigDecimal amount, BigDecimal newBalance,
                              LocalDateTime executedAt) {}

public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
public class InsufficientFundsException extends BusinessException {
    public InsufficientFundsException() { super("Solde insuffisant"); }
}
// Erreur technique (DB, I/O) : encapsulée, jamais affichée brute à l'utilisateur
public class DataAccessException extends RuntimeException {
    public DataAccessException(String msg, Throwable cause) { super(msg, cause); }
}
```

**Convention à imposer** : exceptions *non vérifiées* (`RuntimeException`). Un `BusinessException` = message affichable tel quel ; un `DataAccessException` = on affiche « Erreur technique » et on logue la cause.

### 5.3 Interfaces, Optional, streams

```java
public interface AccountDAO {
    Optional<Account> findById(long id);
    List<Account> findByCustomerId(long customerId);
    void debit(Connection conn, long accountId, BigDecimal amount);   // même Connection que la transaction
    void credit(Connection conn, long accountId, BigDecimal amount);
}

public class AccountService {
    private final AccountDAO accountDAO;
    public AccountService(AccountDAO accountDAO) { this.accountDAO = accountDAO; }

    public BigDecimal totalBalance(long customerId) {
        return accountDAO.findByCustomerId(customerId).stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

Le Service reçoit son DAO **par le constructeur** (injection manuelle) : c'est ce qui te permet, en tant qu'intégrateur, de câbler tout à un seul endroit (section 15).

### 5.4 Fermeture des ressources, logging, configuration

```java
try (Connection c = Database.getConnection();
     PreparedStatement ps = c.prepareStatement(SQL);
     ResultSet rs = ps.executeQuery()) {
    ...
} catch (SQLException e) {
    log.error("findById failed", e);
    throw new DataAccessException("Lecture du compte impossible", e);
}
```

```properties
# resources/app.properties
db.url=jdbc:mysql://localhost:3306/bankdirect?useUnicode=true&characterEncoding=utf8
db.user=root
db.password=
app.currency=MAD
```

```java
public final class AppConfig {
    private static final Properties P = new Properties();
    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/app.properties")) {
            P.load(in);
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    public static String get(String key) { return P.getProperty(key); }
}
```

---

## 6. JavaFX : les composants et leur composition

### 6.1 Hiérarchie à comprendre

```
Stage  (la fenêtre ; UN seul, possédé par MainApp / Navigator)
 └── Scene  (le contenu de la fenêtre ; racine = un Parent)
      └── Parent racine (BorderPane du « shell »)
           └── Node ... (Pane, VBox, HBox, Button, TableView ...)
```

`Node` est tout ce qui s'affiche ; `Parent` est un `Node` qui contient d'autres Nodes ; `Pane` est un `Parent` qui laisse les enfants se positionner.

### 6.2 Quel conteneur pour quel écran bancaire

| Conteneur | Usage dans l'application |
| --- | --- |
| `BorderPane` | **Cadre général** : `top` = en-tête (nom du client, cloche de notifications), `left` = menu, `center` = l'écran courant |
| `VBox` | Empiler : carte de compte, puis liste, puis bouton |
| `HBox` | Aligner : libellé + montant, ou boutons Valider/Annuler |
| `GridPane` | Formulaires (libellé / champ) : virement, profil, ajout bénéficiaire |
| `StackPane` | Superposer : voile gris + `ProgressIndicator` pendant un chargement |
| `ScrollPane` | Dashboard ou liste de produits plus haut que la fenêtre |
| `TabPane` | Sous-sections : Cartes (Débit / Prépayée), Documents (Relevés / Attestations) |
| `AnchorPane` | À éviter pour les mises en page principales (positions figées) |

### 6.3 Composants de saisie et d'affichage

| Composant | Exemple bancaire |
| --- | --- |
| `Label` | Solde, RIB, message d'erreur |
| `TextField` | Montant, motif du virement, RIB |
| `PasswordField` | Mot de passe, code de confirmation |
| `ComboBox<Account>` | Compte à débiter (utilise `setConverter` pour afficher RIB + solde) |
| `ListView<Notification>` | Liste des notifications |
| `TableView<Transaction>` | Historique (section 8) |
| `DatePicker` | Filtre de période de l'historique, de relevés |
| `CheckBox` | « Ajouter aux bénéficiaires », « Paiement en ligne » sur une carte |
| `ProgressIndicator` | Chargement en tâche de fond |
| `ImageView` | Logo, icône de carte |
| `Alert` / `Dialog` | Confirmation, erreur, saisie du code |

```java
// ComboBox<Account> lisible : "0123 ... 4567 - 12 500,00 MAD"
accountCombo.setConverter(new StringConverter<>() {
    @Override public String toString(Account a) {
        return a == null ? "" : a.getMaskedRib() + " - " + Money.format(a.getBalance());
    }
    @Override public Account fromString(String s) { return null; }
});
```

```java
// Alert : préférer une classe utilitaire unique (util/Dialogs)
public static void error(String message) {
    Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
    a.setHeaderText(null);
    a.showAndWait();
}
public static boolean confirm(String message) {
    Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
    return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
}
```

---

## 7. FXML et Controllers

### 7.1 Pourquoi FXML

L'apparence (FXML + CSS) est séparée du comportement (Controller). Chaque développeur peut modifier un écran sans toucher au code des autres, et les conflits Git sont plus faciles à résoudre qu'avec une interface construite par du code.

### 7.2 Structure d'un FXML (login.fxml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>

<VBox xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
      fx:controller="ma.bankdirect.controller.auth.LoginController"
      spacing="12" styleClass="login-box">
    <Label text="Connexion" styleClass="title"/>
    <TextField fx:id="usernameField" promptText="Identifiant"/>
    <PasswordField fx:id="passwordField" promptText="Mot de passe"/>
    <Label fx:id="errorLabel" styleClass="error" visible="false"/>
    <Button fx:id="loginButton" text="Se connecter" onAction="#onLogin" defaultButton="true"/>
    <ProgressIndicator fx:id="spinner" visible="false"/>
</VBox>
```

- `fx:controller` : la classe instanciée par `FXMLLoader`.
- `fx:id` : nom du champ du Controller à remplir (**doit être identique, majuscules comprises**).
- `onAction="#onLogin"` : méthode du Controller appelée au clic.

### 7.3 Le Controller correspondant

```java
public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator spinner;

    private final AuthService authService;

    public LoginController(AuthService authService) {   // injecté par la controllerFactory
        this.authService = authService;
    }

    @FXML private void initialize() {   // appelé APRÈS l'injection des @FXML
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
    }

    @FXML private void onLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();
        setLoading(true);
        Task<AuthResult> task = new Task<>() {
            @Override protected AuthResult call() { return authService.login(user, pass); }
        };
        task.setOnSucceeded(e -> {
            AuthResult r = task.getValue();
            SessionManager.get().start(r.user(), r.customer());
            Navigator.get().showShell();
            Navigator.get().go(Route.DASHBOARD);
        });
        task.setOnFailed(e -> {
            setLoading(false);
            showError(task.getException() instanceof BusinessException
                      ? task.getException().getMessage() : "Erreur technique");
        });
        new Thread(task, "login").start();
    }

    private void setLoading(boolean b) { spinner.setVisible(b); loginButton.setDisable(b); }
    private void showError(String m) { errorLabel.setText(m); errorLabel.setVisible(true); }
}
```

Points importants :

- Les champs `@FXML` sont `null` dans le **constructeur** ; ils sont remplis avant `initialize()`.
- Le Controller n'a **aucun** `new JdbcUserDAO()` : il reçoit un `AuthService` déjà construit.
- Il ne fait ni SQL, ni hash, ni vérification de statut : tout cela est dans `AuthService`.

### 7.4 Charger une vue et récupérer son Controller

```java
FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
loader.setControllerFactory(ServiceRegistry::createController);   // voir section 15
Parent root = loader.load();
LoginController c = loader.getController();   // seulement si tu dois lui passer des données
```

`getResource` renvoie `null` si le chemin est faux : c'est la source n°1 de `LoadException` (section 17).

### 7.5 Passer des données à un Controller

| Besoin | Solution recommandée |
| --- | --- |
| Utilisateur connecté | `SessionManager.get().currentUser()` (jamais en paramètre) |
| Objet spécifique à un écran (compte à afficher, reçu) | `Navigator.go(route, params)` puis méthode `initData(...)` du Controller |
| Service | Constructeur du Controller via `controllerFactory` |

```java
public interface Parameterized<T> { void initData(T data); }

public class AccountDetailController implements Parameterized<Long> {
    @Override public void initData(Long accountId) {
        // charge le compte et ses dernières opérations
    }
}
```

Piège : `initData` est appelé **après** `load()`, donc après `initialize()`. Ne charge donc pas de données dépendantes du paramètre dans `initialize()`.

---

## 8. Properties, ObservableList et TableView

### 8.1 Pourquoi les Properties

Une `Property` notifie ses écouteurs quand sa valeur change. C'est ce qui permet à un `Label` de se mettre à jour tout seul quand le modèle change (bindings) et à une `TableView` de se redessiner quand sa liste change.

| Type | Usage |
| --- | --- |
| `StringProperty` / `SimpleStringProperty` | Libellé, statut affiché |
| `ObjectProperty<BigDecimal>` | Montant (pas de `DoubleProperty` pour l'argent) |
| `ObjectProperty<LocalDateTime>` | Date |
| `IntegerProperty` | Compteur de notifications non lues |
| `ObservableList<T>` | Contenu d'une TableView, ListView, ComboBox |

**Choix pragmatique pour l'équipe** : les Models du domaine restent des POJO simples (sans Property) ; le Controller construit des **lignes d'affichage** (`TransactionRow`) ou utilise des lambdas dans `cellValueFactory`. Cela garde les Models propres et partageables.

### 8.2 TableView\<Transaction> : date, type, montant, description, statut

```xml
<TableView fx:id="txTable" VBox.vgrow="ALWAYS">
    <columns>
        <TableColumn fx:id="dateCol"   text="Date"        prefWidth="140"/>
        <TableColumn fx:id="typeCol"   text="Type"        prefWidth="140"/>
        <TableColumn fx:id="amountCol" text="Montant"     prefWidth="120"/>
        <TableColumn fx:id="labelCol"  text="Description" prefWidth="260"/>
        <TableColumn fx:id="statusCol" text="Statut"      prefWidth="100"/>
    </columns>
</TableView>
```

```java
public class TransactionHistoryController {
    @FXML private TableView<Transaction> txTable;
    @FXML private TableColumn<Transaction, String> dateCol, typeCol, amountCol, labelCol, statusCol;

    private final ObservableList<Transaction> items = FXCollections.observableArrayList();
    private final TransactionService transactionService;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public TransactionHistoryController(TransactionService s) { this.transactionService = s; }

    @FXML private void initialize() {
        txTable.setItems(items);
        dateCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCreatedAt().format(FMT)));
        typeCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getType().name()));
        amountCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                (c.getValue().getDirection() == Direction.DEBIT ? "-" : "+") + Money.format(c.getValue().getAmount())));
        labelCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getLabel()));
        statusCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getStatus().name()));
        reload();
    }

    public void reload() {
        long customerId = SessionManager.get().currentCustomer().getId();
        // appel en tâche de fond en vrai (section 14)
        items.setAll(transactionService.latestForCustomer(customerId, 50));
    }
}
```

`PropertyValueFactory<>("amount")` fonctionne aussi, mais cherche par réflexion une méthode `amountProperty()` ou `getAmount()` : une faute de frappe ne donne **aucune erreur**, juste une colonne vide. Les lambdas sont plus sûres.

### 8.3 Actualiser après un nouveau virement

Trois mécanismes, du plus simple au plus sophistiqué :

1. **Recharger à l'affichage (recommandé)** : la navigation recrée l'écran, `initialize()` rappelle le Service. Rien à synchroniser.
2. **`items.setAll(...)` ou `txTable.refresh()`** quand on reste sur le même écran après une opération.
3. **État partagé observable** (ex. `AccountStore` avec `ObservableList<Account>`) si plusieurs écrans visibles en même temps doivent réagir. À n'introduire que si nécessaire.

---

## 9. Navigation globale

### 9.1 Les stratégies possibles

| Stratégie | Principe | Problème |
| --- | --- | --- |
| A. Un `Stage` par écran | Chaque Controller ouvre une nouvelle fenêtre | Fenêtres multiples, mémoire, session difficile à gérer |
| B. Remplacer la `Scene` | `stage.setScene(new Scene(root))` | Perd la taille/plein écran, pas de menu persistant |
| C. **Shell + remplacement du centre** | Un `BorderPane` fixe, on change `setCenter(...)` | Demande un `Navigator` central (recommandé) |
| D. Vues mises en cache dans un `StackPane` | Toutes chargées, on bascule la visibilité | État obsolète si on ne recharge pas |

**Recommandation : C.** Une seule `Scene` pour l'application connectée, un menu persistant (comme les onglets d'une application mobile), un seul propriétaire de la navigation.

### 9.2 Qui possède quoi

- **`MainApp`** reçoit le `Stage` principal de JavaFX, le donne à `Navigator.init(stage)`, puis affiche le login. **Il n'y a pas d'autre `new Stage()`** dans le projet (sauf dialogues modaux).
- **`Navigator`** est le seul à appeler `FXMLLoader` pour les écrans et le seul à modifier la Scene ou le centre du shell.
- Les **Controllers** demandent `Navigator.go(Route.X)`. Ils ne connaissent ni les chemins FXML ni les autres Controllers.

### 9.3 Routes et Navigator

```java
public enum Route {
    LOGIN("/fxml/auth/login.fxml"),
    SHELL("/fxml/shell/shell.fxml"),
    DASHBOARD("/fxml/dashboard/dashboard.fxml"),
    ACCOUNTS("/fxml/account/account-list.fxml"),
    ACCOUNT_DETAIL("/fxml/account/account-detail.fxml"),
    TRANSACTIONS("/fxml/account/transaction-history.fxml"),
    TRANSFER("/fxml/transfer/transfer.fxml"),
    BENEFICIARIES("/fxml/transfer/beneficiaries.fxml"),
    CARDS("/fxml/card/cards.fxml"),
    BILLS("/fxml/bill/bills.fxml"),
    DOCUMENTS("/fxml/document/documents.fxml"),
    PROFILE("/fxml/profile/profile.fxml"),
    SUPPORT("/fxml/support/support.fxml");

    private final String fxml;
    Route(String fxml) { this.fxml = fxml; }
    public String fxml() { return fxml; }
}
```

```java
public final class Navigator {
    private static final Navigator INSTANCE = new Navigator();
    public static Navigator get() { return INSTANCE; }

    private Stage stage;
    private BorderPane shellRoot;          // null tant qu'on n'est pas connecté
    private Callback<Class<?>, Object> controllerFactory;

    public void init(Stage stage, Callback<Class<?>, Object> factory) {
        this.stage = stage; this.controllerFactory = factory;
    }

    public void showLogin() {
        shellRoot = null;
        stage.setScene(new Scene(load(Route.LOGIN, null), 480, 640));
    }

    public void showShell() {
        shellRoot = (BorderPane) load(Route.SHELL, null);
        stage.setScene(new Scene(shellRoot, 1100, 720));
    }

    public void go(Route route) { go(route, null); }

    public <T> void go(Route route, T params) {
        if (shellRoot == null) throw new IllegalStateException("Shell non affiché");
        shellRoot.setCenter(load(route, params));
    }

    @SuppressWarnings("unchecked")
    private <T> Parent load(Route route, T params) {
        try {
            URL url = Navigator.class.getResource(route.fxml());
            if (url == null) throw new IllegalStateException("FXML introuvable : " + route.fxml());
            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(controllerFactory);
            Parent root = loader.load();
            Object c = loader.getController();
            if (params != null && c instanceof Parameterized<?> p) ((Parameterized<T>) p).initData(params);
            return root;
        } catch (IOException e) {
            throw new IllegalStateException("Chargement impossible : " + route, e);
        }
    }
}
```

Pourquoi cette conception :

- **Un seul Stage** ; la Scene du shell est créée une fois, seul le centre change.
- Ajouter un écran = ajouter une constante dans `Route` + un FXML. Un contrôle centralisé des chemins évite les « FXML introuvable ».
- Revenir au dashboard : `Navigator.get().go(Route.DASHBOARD)` depuis n'importe où.

### 9.4 Éviter les dépendances circulaires entre Controllers

Mauvais : `TransferController` importe `DashboardController` pour appeler `refresh()`, et `DashboardController` importe `TransferController` pour ouvrir un virement.

Bon :

1. **Navigation** : tous deux passent par `Navigator` et `Route` (enum), qui ne dépend d'aucun Controller.
2. **Données** : tous deux passent par les Services et la base ; l'écran cible se recharge lui-même.
3. **Notification entre écrans visibles en même temps** (rare) : un petit bus d'événements (`AppEvents.publish(new AccountChanged(id))`) dont les Controllers s'abonnent. N'en crée un que si le besoin est réel, et désabonne-toi à la fermeture de l'écran.

Test simple pour une revue de code : fais une recherche de `Controller` dans les `import` d'un Controller. Si le résultat n'est pas vide, c'est un défaut.

---

## 10. Session utilisateur

### 10.1 Conception

```java
public final class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();
    public static SessionManager get() { return INSTANCE; }

    private User user;
    private Customer customer;
    private LocalDateTime loginTime;

    public void start(User u, Customer c) {
        this.user = u; this.customer = c; this.loginTime = LocalDateTime.now();
    }
    public boolean isLoggedIn() { return user != null; }
    public User currentUser() { requireLogin(); return user; }
    public Customer currentCustomer() { requireLogin(); return customer; }
    public void updateCustomer(Customer c) { this.customer = c; }   // après modification du profil
    public void clear() { user = null; customer = null; loginTime = null; }

    private void requireLogin() {
        if (user == null) throw new IllegalStateException("Aucune session active");
    }
}
```

**Un seul** `SessionManager`, dans le package `session`, créé par l'intégrateur. Tout membre qui crée sa propre variable `static User currentUser` doit être redirigé vers celui-ci.

### 10.2 Qui l'utilise

| Classe | Usage |
| --- | --- |
| `LoginController` | `start(...)` après une authentification réussie |
| `DashboardController`, `AccountController` | `currentCustomer().getId()` pour charger ses comptes |
| `TransferController` | idem, pour ses comptes et ses bénéficiaires |
| `ProfileController` | lecture, puis `updateCustomer(...)` après sauvegarde |
| `MainShellController` | nom du client dans l'en-tête, bouton de déconnexion |

**Règle de sécurité métier** : le Service ne fait jamais confiance à un `customerId` envoyé par l'interface pour une opération sensible ; il vérifie que le compte appartient bien au client de la session. Exemple : `TransferService` vérifie `account.getCustomerId() == session.currentCustomer().getId()`.

### 10.3 Logout

```java
@FXML private void onLogout() {
    if (!Dialogs.confirm("Voulez-vous vous déconnecter ?")) return;
    SessionManager.get().clear();
    Navigator.get().showLogin();     // nouvelle Scene, anciens Controllers abandonnés
}
```

Points à vérifier : arrêter les tâches ou `Timeline` en cours (notifications, minuteur d'inactivité), vider les champs sensibles, et s'assurer qu'aucun Controller ne conserve une copie de l'ancien utilisateur dans un champ statique.

---

## 11. Base de données : JDBC (et pourquoi pas JPA ici)

### 11.1 La chaîne technique

```
Java → Driver JDBC → Connection → SQL → PreparedStatement → ResultSet → mapping → Model
```

Dépendance Maven du driver (exemple MySQL/MariaDB) :

```xml
<dependency>
  <groupId>com.mysql</groupId>
  <artifactId>mysql-connector-j</artifactId>
  <version>8.4.0</version>
</dependency>
```

### 11.2 Un seul fournisseur de connexions

```java
public final class Database {
    private Database() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                AppConfig.get("db.url"), AppConfig.get("db.user"), AppConfig.get("db.password"));
    }
}
```

Pour un projet étudiant, `DriverManager` suffit : **une connexion courte par opération**, fermée avec try-with-resources. Si les performances deviennent un souci, remplace le contenu de `getConnection()` par un pool (HikariCP) sans toucher aucun DAO : c'est l'intérêt d'avoir **un seul point d'accès**. Interdit : une `static Connection` partagée par toute l'application (non thread-safe, fermée par l'un et utilisée par l'autre).

### 11.3 CRUD dans un DAO

```java
public class JdbcAccountDAO implements AccountDAO {

    private static final String SELECT_BY_ID =
        "SELECT id, customer_id, rib, type, currency, balance, status, opened_at FROM accounts WHERE id = ?";

    @Override
    public Optional<Account> findById(long id) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("findById account " + id, e);
        }
    }

    private Account map(ResultSet rs) throws SQLException {    // mapping SQL → objet
        Account a = new Account();
        a.setId(rs.getLong("id"));
        a.setCustomerId(rs.getLong("customer_id"));
        a.setRib(rs.getString("rib"));
        a.setType(AccountType.valueOf(rs.getString("type")));
        a.setCurrency(rs.getString("currency"));
        a.setBalance(rs.getBigDecimal("balance"));
        a.setStatus(AccountStatus.valueOf(rs.getString("status")));
        a.setOpenedAt(rs.getTimestamp("opened_at").toLocalDateTime());
        return a;
    }
}
```

| Opération | Méthode JDBC | Retour DAO |
| --- | --- | --- |
| SELECT | `executeQuery()` + `ResultSet` | `Optional<T>` ou `List<T>` |
| INSERT | `executeUpdate()` + `RETURN_GENERATED_KEYS` | l'id généré ou l'objet avec id |
| UPDATE | `executeUpdate()` | nombre de lignes modifiées (vérifié) |
| DELETE | `executeUpdate()` | idem |

Insertion avec récupération de l'id :

```java
String sql = "INSERT INTO beneficiaries(customer_id, full_name, rib, bank_name, created_at) VALUES (?,?,?,?,?)";
try (Connection c = Database.getConnection();
     PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
    ps.setLong(1, b.getCustomerId());
    ps.setString(2, b.getFullName());
    ps.setString(3, b.getRib());
    ps.setString(4, b.getBankName());
    ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
    ps.executeUpdate();
    try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) b.setId(keys.getLong(1)); }
}
```

**Erreurs à éviter** : concaténer des valeurs dans le SQL (injection SQL, utilise toujours `?`), oublier de fermer les ressources, `rs.getDouble` sur un montant, `rs.getInt("x")` sur un `NULL` sans vérifier `rs.wasNull()`, avaler `SQLException`.

### 11.4 JDBC ou JPA/Hibernate ?

| Critère | JDBC + DAO | JPA / Hibernate |
| --- | --- | --- |
| Courbe d'apprentissage | Faible, tu vois le SQL | Forte (entités, contexte de persistance, lazy loading) |
| Contrôle des transactions bancaires | Total et explicite | Possible mais plus abstrait |
| Intégration à 7 personnes | SQL lisible en relecture | Annotations et configuration à harmoniser |
| Quantité de code | Plus élevée (mapping manuel) | Plus faible |
| Risques spécifiques | SQL dupliqué | `LazyInitializationException`, modèle couplé à l'ORM |

**Recommandation pour votre projet : JDBC + DAO.** Votre objectif pédagogique inclut les transactions et le mapping, et la matrice de dépendances est plus simple à intégrer. JPA devient raisonnable seulement si toute l'équipe le maîtrise déjà ; surtout, **ne mélangez pas les deux** dans le même projet.

---

## 12. Transactions bancaires

### 12.1 Pourquoi un virement est une seule opération

Un virement de A vers B comprend : débiter A, créditer B, écrire les lignes d'historique. Si le programme plante après le débit : l'argent a disparu. Si la ligne d'historique est écrite mais pas le débit : l'historique ment. Il faut **tout ou rien** (atomicité).

```sql
BEGIN;
  UPDATE accounts SET balance = balance - 500 WHERE id = :A AND balance >= 500;  -- 1 ligne ?
  UPDATE accounts SET balance = balance + 500 WHERE id = :B;
  INSERT INTO transactions(...) VALUES (... DEBIT sur A ...);
  INSERT INTO transactions(...) VALUES (... CREDIT sur B ...);
COMMIT;       -- tout est enregistré
-- en cas d'erreur : ROLLBACK;   -- rien n'est enregistré
```

Le `AND balance >= 500` dans le `UPDATE` rend la vérification du solde **atomique** : deux virements simultanés ne peuvent pas dépasser le solde. Si `executeUpdate()` retourne 0, le service lève `InsufficientFundsException`.

### 12.2 Traduction en Java : TransactionManager

```java
public final class TransactionManager {
    public <T> T execute(Function<Connection, T> work) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.apply(conn);
                conn.commit();
                return result;
            } catch (RuntimeException e) {
                conn.rollback();
                throw e;                                  // on remonte l'erreur au Service/Controller
            }
        } catch (SQLException e) {
            throw new DataAccessException("Transaction échouée", e);
        }
    }
}
```

### 12.3 TransferService complet (simplifié)

```java
public class TransferService {
    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;
    private final BeneficiaryDAO beneficiaryDAO;
    private final TransactionManager tx;

    public TransferService(AccountDAO a, TransactionDAO t, BeneficiaryDAO b, TransactionManager tx) {
        this.accountDAO = a; this.transactionDAO = t; this.beneficiaryDAO = b; this.tx = tx;
    }

    public TransferReceipt internalTransfer(TransferRequest r) {
        long customerId = SessionManager.get().currentCustomer().getId();

        if (!Money.isPositive(r.amount())) throw new BusinessException("Montant invalide");
        if (r.fromAccountId() == r.toAccountId())  throw new BusinessException("Comptes identiques");

        Account from = accountDAO.findById(r.fromAccountId())
                .orElseThrow(() -> new BusinessException("Compte source introuvable"));
        Account to = accountDAO.findById(r.toAccountId())
                .orElseThrow(() -> new BusinessException("Compte destination introuvable"));

        if (from.getCustomerId() != customerId || to.getCustomerId() != customerId)
            throw new BusinessException("Opération non autorisée");
        if (from.getStatus() != AccountStatus.ACTIVE) throw new BusinessException("Compte source bloqué");

        String reference = "VIR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        return tx.execute(conn -> {
            // même ordre de verrouillage pour éviter les deadlocks
            accountDAO.lockForUpdate(conn, Math.min(from.getId(), to.getId()), Math.max(from.getId(), to.getId()));
            if (!accountDAO.debitIfEnough(conn, from.getId(), r.amount()))
                throw new InsufficientFundsException();
            accountDAO.credit(conn, to.getId(), r.amount());

            BigDecimal fromAfter = accountDAO.findBalance(conn, from.getId());
            BigDecimal toAfter   = accountDAO.findBalance(conn, to.getId());
            transactionDAO.insert(conn, Transaction.debit(from.getId(), r, reference, fromAfter, now));
            transactionDAO.insert(conn, Transaction.credit(to.getId(), r, reference, toAfter, now));
            return new TransferReceipt(reference, r.amount(), fromAfter, now);
        });
    }
}
```

Points d'intégration à contrôler dans le code d'un camarade :

1. Les DAO utilisés dans la transaction **prennent la `Connection` en paramètre** (surcharges `insert(conn, ...)`). Un DAO qui ouvre sa propre connexion dans le bloc casse l'atomicité.
2. Aucun `commit()` hors du `TransactionManager`.
3. Les vérifications de lecture (`findById`) peuvent être faites avant, mais **la vérification de solde qui compte** est celle du `UPDATE ... WHERE balance >= ?`.
4. Les mêmes règles s'appliquent au paiement de facture, à la recharge de carte, et à tout traitement qui touche un solde.

La notification et l'envoi éventuel de document de reçu se font **après** le commit (ou dans la même transaction si elles sont en base et doivent être cohérentes).

---

## 13. Controller, Service, DAO : règles concrètes

| Question à se poser | Réponse |
| --- | --- |
| « Je lis un champ, je formate un nombre pour l'afficher » | Controller |
| « Je vérifie le format du montant saisi » (vide, non numérique) | Controller (validation de forme) |
| « Je vérifie que le solde est suffisant, que le compte est actif » | Service (règle métier) |
| « J'écris un `SELECT`, un `UPDATE` » | DAO |
| « J'ouvre/ferme une transaction » | Service via `TransactionManager` |
| « Je navigue vers un autre écran » | Controller via `Navigator` |
| « Je calcule le total des comptes » | Service |
| « Je transforme `Transaction` en texte de colonne » | Controller (ou classe de présentation) |

Règles à imposer à l'équipe :

1. Un Controller n'importe **aucune** classe de `dao` ni de `java.sql`.
2. Un Service n'importe **aucune** classe de `javafx.*` ni de `controller`.
3. Un DAO ne contient **aucune** règle métier (pas de « si solde < montant » autrement que dans le SQL conditionnel).
4. Une méthode de Service = un cas d'usage (`transferToBeneficiary`, `payBill`), pas un `getX` générique.
5. Les erreurs métier sont des `BusinessException` ; les messages sont lisibles par l'utilisateur.
6. Le Controller capture les exceptions, **n'affiche jamais** `e.printStackTrace()` à l'écran.

Exemple de Controller correct pour le virement (la méthode appelle, ne décide pas) :

```java
@FXML private void onConfirm() {
    TransferRequest req = new TransferRequest(
            fromCombo.getValue().getId(), null, beneficiaryCombo.getValue().getId(),
            Money.of(amountField.getText()), labelField.getText());
    Async.run(() -> transferService.transferToBeneficiary(req),
              receipt -> Navigator.get().go(Route.TRANSFER, receipt),   // ou une route de succès
              err -> Dialogs.error(Errors.userMessage(err)));
}
```

---

## 14. Threads et JavaFX

### 14.1 Le principe

Tout ce qui touche à l'interface s'exécute sur le **JavaFX Application Thread**. Si tu y fais un appel base de données de 800 ms, la fenêtre est gelée pendant ce temps. Inversement, modifier un `Label` depuis un autre thread lève une exception (`Not on FX application thread`) ou, pire, bugue silencieusement.

```
FX thread : clic → affiche spinner → lance Task ───────┐
Thread tâche :                    Service → DAO → DB  │
FX thread : setOnSucceeded ← résultat ←────────────┘
            → masque le spinner, remplit la TableView
```

`Task#setOnSucceeded` et `setOnFailed` s'exécutent déjà sur le FX thread : tu peux y modifier l'interface sans `Platform.runLater`. `Platform.runLater` sert quand tu es dans un thread que tu gères toi-même.

### 14.2 Utilitaire commun `Async`

```java
public final class Async {
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "bank-worker");
        t.setDaemon(true);          // ne bloque pas la fermeture de l'application
        return t;
    });

    public static <T> void run(Supplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() { @Override protected T call() { return work.get(); } };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException()));
        POOL.execute(task);
    }
}
```

### 14.3 Exemple : le Dashboard

```java
@FXML private void initialize() {
    spinner.setVisible(true);
    long customerId = SessionManager.get().currentCustomer().getId();   // lu sur le FX thread
    Async.run(() -> accountService.getDashboardData(customerId),        // thread tâche : DB
              data -> {                                                 // FX thread : affichage
                  accountsList.setItems(FXCollections.observableList(data.accounts()));
                  totalLabel.setText(Money.format(data.total()));
                  spinner.setVisible(false);
              },
              err -> { spinner.setVisible(false); Dialogs.error(Errors.userMessage(err)); });
}
```

Points importants :

- **Lis la session et les champs de l'interface sur le FX thread**, passe-les en paramètres au thread tâche (variables locales `final`).
- Le thread tâche ne doit pas toucher à un `Node`.
- Un Service qui retourne un `record DashboardData(accounts, total, unreadCount)` évite d'enchaîner trois appels réseau/DB depuis l'interface.
- Si l'utilisateur quitte l'écran avant la fin de la tâche, prévois d'ignorer le résultat ou d'annuler la tâche.

---

## 15. Intégration de plusieurs modules

### 15.1 Le contrat à définir AVANT d'écrire du code

Rédige un fichier `docs/CONTRACT.md` validé par les 7 membres. Il contient au minimum :

| Élément | Contenu figé |
| --- | --- |
| Packages | arbre de la section 2, avec le propriétaire de chaque sous-package |
| Models | liste des champs et types de chaque entité (section 3), créés une seule fois par l'intégrateur |
| Enums | `model.enums` figé |
| Schéma DB | `schema.sql` et `seed.sql` uniques |
| Interfaces DAO | signatures des méthodes publiques |
| Signatures de Services | méthode, paramètres, retour, exceptions lancées |
| Routes | enum `Route` : un nom par écran |
| Paramètres de navigation | type donné à `initData` pour chaque route |
| Exceptions | hiérarchie `BusinessException` |
| Données partagées | seulement `SessionManager` ; pas d'autres statiques |
| Conventions FXML/CSS | dossier `resources/fxml/<module>/`, noms en kebab-case, `fx:id` en camelCase |

Exemple de contrat de service :

```java
// service/TransferService.java  (propriétaire : membre 4)
TransferReceipt internalTransfer(TransferRequest r)          throws BusinessException;
TransferReceipt transferToBeneficiary(TransferRequest r)     throws BusinessException;
List<Account>   getSourceAccounts(long customerId);
```

Un développeur peut coder son écran contre l'**interface** avant que l'implémentation soit terminée, en utilisant un faux service temporaire (`FakeTransferService`) qui retourne des données en dur.

### 15.2 Le point de câblage unique : ServiceRegistry

```java
public final class ServiceRegistry {
    private static final TransactionManager TX = new TransactionManager();

    private static final AccountDAO accountDAO = new JdbcAccountDAO();
    private static final TransactionDAO transactionDAO = new JdbcTransactionDAO();
    private static final BeneficiaryDAO beneficiaryDAO = new JdbcBeneficiaryDAO();
    private static final UserDAO userDAO = new JdbcUserDAO();
    private static final CustomerDAO customerDAO = new JdbcCustomerDAO();

    private static final AuthService authService = new AuthService(userDAO, customerDAO);
    private static final AccountService accountService = new AccountService(accountDAO, transactionDAO);
    private static final TransferService transferService =
            new TransferService(accountDAO, transactionDAO, beneficiaryDAO, TX);

    /** Utilisé par FXMLLoader.setControllerFactory */
    public static Object createController(Class<?> type) {
        if (type == LoginController.class)             return new LoginController(authService);
        if (type == DashboardController.class)         return new DashboardController(accountService);
        if (type == TransferController.class)          return new TransferController(transferService, accountService);
        if (type == TransactionHistoryController.class) return new TransactionHistoryController(accountService);
        // ... un cas par Controller
        try { return type.getDeclaredConstructor().newInstance(); }   // Controllers sans service
        catch (Exception e) { throw new IllegalStateException("Controller non câblé : " + type, e); }
    }
}
```

C'est ici, et **seulement ici**, qu'on fait `new JdbcXxxDAO()` et `new XxxService(...)`. Quand un membre livre son module, ta mission d'intégrateur se résume souvent à : (1) ajouter ses constantes `Route`, (2) ajouter ses lignes ici, (3) ajouter l'entrée de menu dans le shell.

### 15.3 Comment éviter les problèmes classiques

| Problème | Prévention |
| --- | --- |
| Deux classes `User` différentes | Package `model` appartient à l'intégrateur ; aucun autre module ne crée de Model sans passer par lui |
| Deux `Account` incompatibles | Champs du Model figés dans `CONTRACT.md` ; tout ajout via demande de merge |
| Deux systèmes de navigation | Seul `Navigator` charge des FXML d'écran ; interdiction de `new FXMLLoader` hors de lui |
| Plusieurs connexions mal gérées | Seul `Database.getConnection()` ; pas de `DriverManager` ailleurs (recherche globale) |
| Controller qui dépend d'un autre | Revue des `import` ; navigation par `Route` |
| Services dupliqués | Un service par domaine ; avant d'en créer un, vérifier le contrat |
| Conflits Maven | Seul l'intégrateur modifie `pom.xml` ; les autres demandent par message |
| Chemins FXML différents | Chemins dans l'enum `Route`, absolus (`/fxml/...`) |
| Ressources introuvables | Tout dans `src/main/resources` ; test de chargement de toutes les routes (voir ci-dessous) |

Test rapide qui charge tous les écrans (à exécuter avant chaque merge) :

```java
for (Route r : Route.values()) {
    URL u = Navigator.class.getResource(r.fxml());
    if (u == null) System.err.println("MANQUANT : " + r + " -> " + r.fxml());
}
```

### 15.4 Lire le module d'un autre membre : la grille en 8 questions

Quand un camarade te livre son travail, réponds à ces questions dans l'ordre :

1. **Quelles classes existent ?** Liste par package (`controller`, `service`, `dao`, `model`, FXML).
2. **Quel Controller démarre le parcours ?** Celui référencé par la `Route` d'entrée.
3. **Quel Service est appelé ?** Cherche les champs `...Service` du Controller.
4. **Quels DAO sont utilisés ?** Cherche les champs `...DAO` du Service.
5. **Quels Models sont concernés ?** Types en paramètres et retours.
6. **Quelle donnée revient ?** Type de retour de la méthode appelée (Model, `List`, `record`).
7. **Comment arrive-t-elle dans JavaFX ?** `setItems`, `setText`, `cellValueFactory`... et sur quel thread.
8. **Comment brancher le module ?** `Route` + `ServiceRegistry` + menu du shell + tables SQL à ajouter dans `schema.sql`.

Si tu ne peux pas répondre à l'une d'elles en cinq minutes, le module viole probablement le contrat : renvoie-le pour correction.

---

## 16. Git et Maven pour une équipe de 7

### 16.1 Workflow de branches

```
main      ●─────────────────────────●────   (versions stables, tags v0.1, v0.2, v1.0)
           \                         /
develop    ●──●──●──●──●──●──●──●   (intégration continue)
               \   /    \   /
feature/*       ●─●      ●─●          (un membre = une fonctionnalité)
```

- `main` : toujours compilable et démontrable. Seul l'intégrateur y merge, depuis `develop`.
- `develop` : branche d'intégration. On y arrive par **pull request** seulement.
- `feature/<module>-<sujet>` : par exemple `feature/transfer-internal`, `feature/cards-block`. Courte durée (2 à 4 jours maximum).

Cycle d'un membre :

```bash
git checkout develop && git pull
git checkout -b feature/transfer-internal
# ... travail, commits fréquents ...
git fetch origin
git merge origin/develop          # résout les conflits CÔTÉ MEMBRE, pas côté intégrateur
mvn clean verify                  # doit passer
git push -u origin feature/transfer-internal   # puis pull request vers develop
```

### 16.2 Les fichiers « chauds » (zones de conflit)

| Fichier | Qui le modifie | Stratégie |
| --- | --- | --- |
| `pom.xml` | Intégrateur seul | Demande de dépendance par message |
| `Route.java` | Tous (une constante chacun) | Une constante par ligne, ajoutées à la fin, conflits triviaux |
| `ServiceRegistry.java` | Tous | Un bloc par module, séparé par un commentaire `// ---- module X ----` |
| `schema.sql` / `seed.sql` | Tous | Ajout de tables en fin de fichier ; jamais de modification d'une table existante sans accord |
| `shell.fxml` (menu) | Intégrateur | Le membre indique la `Route` à ajouter |
| `theme.css` | Quasi personne | Chaque module a son propre `module-x.css` |

### 16.3 Checklist avant d'accepter une pull request

1. `mvn clean verify` réussit sur la branche **après** merge de `develop`.
2. L'application démarre ; le parcours du module fonctionne de bout en bout avec le jeu de données `seed.sql`.
3. Toutes les routes se chargent (test de la section 15.3).
4. Aucun `import` interdit (DAO dans un Controller, JavaFX dans un Service).
5. Pas de `System.out.println` oublié ni de mot de passe/identifiant réel dans le code.
6. Les changements de schéma sont dans `schema.sql` et le `seed.sql` fonctionne sur une base **vide**.
7. Pas de fichiers générés (`target/`, `.idea/`) dans le commit.

### 16.4 Résoudre un conflit

```bash
git merge origin/develop
# CONFLICT in Route.java
```

Dans le fichier, les marqueurs `<<<<<<<`, `=======`, `>>>>>>>` entourent les deux versions. Garde **les deux** si ce sont deux ajouts indépendants (deux constantes d'enum), supprime les marqueurs, vérifie la syntaxe (virgules, point-virgule final de l'enum), puis :

```bash
git add Route.java
mvn clean verify
git commit
```

Si le conflit concerne un Model ou un contrat (deux versions d'une même méthode de Service) : **ne tranche pas seul**, c'est un problème de contrat à régler avec les deux auteurs.

`.gitignore` minimal : `target/`, `.idea/`, `*.iml`, `.vscode/`, `app-local.properties` (configuration locale du mot de passe DB).

### 16.5 Maven : base du `pom.xml`

```xml
<properties>
  <maven.compiler.release>21</maven.compiler.release>
  <javafx.version>21.0.2</javafx.version>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencies>
  <dependency><groupId>org.openjfx</groupId><artifactId>javafx-controls</artifactId><version>${javafx.version}</version></dependency>
  <dependency><groupId>org.openjfx</groupId><artifactId>javafx-fxml</artifactId><version>${javafx.version}</version></dependency>
  <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId><version>8.4.0</version></dependency>
</dependencies>

<build>
  <plugins>
    <plugin>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-maven-plugin</artifactId>
      <version>0.0.8</version>
      <configuration><mainClass>ma.bankdirect.MainApp</mainClass></configuration>
    </plugin>
  </plugins>
</build>
```

Lancement : `mvn javafx:run`.

**Choix recommandé : pas de `module-info.java`** (mode classpath). Avec les modules Java, il faut `opens ma.bankdirect.controller... to javafx.fxml` pour chaque package de Controllers, et chaque dépendance doit avoir son `requires` : source fréquente d'erreurs d'intégration. Si vous utilisez `module-info.java`, c'est l'intégrateur qui le tient à jour.

Pour que le `.jar` démarre hors de Maven, la classe de `main()` ne doit pas étendre `Application` : d'où la classe `Launcher` qui appelle `Application.launch(MainApp.class, args)`.

---

## 17. Debugging : symptôme → cause → diagnostic → solution

| Symptôme | Cause probable | Diagnostic | Solution |
| --- | --- | --- | --- |
| `LoadException` / `Location is not set` | `getResource(...)` a retourné `null` : mauvais chemin ou FXML pas copié dans `target/classes` | Afficher l'URL ; vérifier `target/classes/fxml/...` | Chemin absolu `/fxml/...` ; fichier dans `src/main/resources` ; `mvn clean` |
| `Controller class not found` / `ClassNotFoundException` | `fx:controller` avec un mauvais package ou une faute de nom | Comparer avec le chemin du `.java` | Corriger le nom complet ; recompiler |
| `LoadException: ... has no setter` / `Unable to coerce` | Attribut FXML inexistant ou mal typé | Lire la ligne indiquée dans la trace | Corriger l'attribut |
| Champ `@FXML` à `null` (NullPointerException) | `fx:id` différent du nom du champ, `@FXML` oublié, ou accès dans le constructeur | Comparer `fx:id` et champ caractère par caractère | Aligner les noms ; utiliser `initialize()` |
| `No such method: onLogin` / handler introuvable | `onAction="#onLogin"` sans méthode (ou méthode non `@FXML` privée) | Chercher le nom exact | Ajouter `@FXML private void onLogin()` |
| `JavaFX runtime components are missing` | `main()` dans une classe qui étend `Application` ; modules JavaFX absents | Lancer par `mvn javafx:run` ou via `Launcher` | Classe `Launcher` ; dépendances `javafx-*` dans le pom |
| `IllegalAccessException` / `module ... does not open` | `module-info.java` sans `opens ... to javafx.fxml` | Lire le message : il nomme le package | Ajouter `opens` ou supprimer le `module-info` |
| Écran vide / pas de changement de Scene | Mauvaise racine placée dans la Scene, `setCenter` sur un shell `null` | Logs dans `Navigator.go` | Appeler `showShell()` avant `go(...)` |
| CSS non appliqué | Feuille non ajoutée, mauvais chemin, `styleClass` mal écrite | `scene.getStylesheets()` et warnings console | `scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm())` |
| TableView affiche des lignes vides | `cellValueFactory` absent ou `PropertyValueFactory` avec nom erroné | Vérifier que `items` n'est pas vide, puis les factories | Utiliser des lambdas ; `setItems(...)` |
| TableView non mise à jour | Liste remplacée sans `setItems`, modification d'un objet non observable | Contrôler quelle liste est liée | `items.setAll(...)` ; `table.refresh()` |
| Donnée `null` dans l'écran suivant | `initData` pas appelé ou lu dans `initialize()` | Log au début des deux méthodes | Charger dans `initData` ; paramètres via `Navigator.go(route, params)` |
| `Not on FX application thread` | Modification d'un Node depuis une tâche | Lire la pile d'appels | Retourner le résultat à `setOnSucceeded` ou `Platform.runLater` |
| Interface qui gèle | Appel DB sur le FX thread | Thread dump, ou chronométrer l'appel | `Async.run(...)` |
| `Communications link failure` / `Access denied` | Base arrêtée, mauvais URL, utilisateur ou mot de passe | Tester l'URL avec un client SQL ; vérifier XAMPP | Démarrer MySQL ; corriger `app.properties` |
| `SQLSyntaxErrorException` / `Unknown column` | SQL non aligné avec `schema.sql` | Copier la requête dans un client SQL | Corriger la requête ou le schéma |
| `Data truncated` / enum invalide | Valeur d'enum absente en base, ou casse différente | Regarder la valeur lue/écrite | Aligner `seed.sql` avec les enums |
| Données non actualisées après une opération | Rien ne recharge ; transaction non commitée ; cache | Vérifier la base directement, puis l'écran | Recharger à l'affichage ; vérifier le `commit` |

Méthode générale : **lis la première ligne de la cause racine** (`Caused by:` tout en bas de la trace), pas la première ligne de la trace. Puis isole : l'écran charge-t-il sans service ? Le service répond-il avec un `main()` de test ? La requête marche-t-elle dans un client SQL ?

---

## 18. Écrans à reproduire (inspirés de BMCE Direct)

> **Avertissement.** Je ne décris que des fonctionnalités de la liste fournie par ton équipe. L'organisation exacte des menus, les libellés et le visuel de l'application mobile réelle ne sont pas vérifiés ici : **À adapter selon l'observation de l'application réelle.** Le découpage ci-dessous est une proposition de travail.

Pour chaque écran : objectif → composants → données → actions / écran suivant → `Service` / `DAO` / `Model`.

### 18.1 AUTHENTIFICATION

| Écran | Objectif et composants | Données, actions, suite | Service / DAO / Model |
| --- | --- | --- | --- |
| Login | Se connecter ; `TextField`, `PasswordField`, `Button`, `Label` erreur, `ProgressIndicator` | Identifiant, mot de passe ; « Se connecter » → Dashboard | `AuthService` / `UserDAO`, `CustomerDAO` / `User`, `Customer` |
| Mot de passe oublié (simulation) | Réinitialisation simulée ; `TextField`, `Button` | Identifiant ; envoi simulé → Login | `AuthService` / `UserDAO` / `User` |
| Confirmation (code) | Valider une opération sensible ; `PasswordField`, `Button` | Code simulé ; Valider/Annuler → opération appelante | `SecurityService` / (éventuel `OtpDAO`) / — |

### 18.2 ACCUEIL

| Écran | Objectif et composants | Données, actions, suite | Service / DAO / Model |
| --- | --- | --- | --- |
| Dashboard | Vue d'ensemble ; `ScrollPane`, cartes (`VBox`), `ListView`, boutons rapides, cloche | Comptes, solde total, dernières opérations, notifications non lues ; raccourcis → Virement, Cartes, Factures | `AccountService`, `NotificationService` / `AccountDAO`, `TransactionDAO`, `NotificationDAO` / `Account`, `Transaction`, `Notification` |

### 18.3 MA SITUATION

| Écran | Objectif et composants | Données, actions, suite | Service / DAO / Model |
| --- | --- | --- | --- |
| Liste des comptes | Consulter comptes et soldes ; `ListView`/cartes | RIB masqué, type, solde ; clic → Détail compte | `AccountService` / `AccountDAO` / `Account` |
| Détail compte | Informations d'un compte ; `Label`, `TabPane` | RIB complet, solde, dernières opérations ; → Historique, Virement, Relevé | `AccountService` / `AccountDAO`, `TransactionDAO` / `Account`, `Transaction` |
| Mes cartes (consultation) | Voir les cartes ; liste | Numéro masqué, type, statut, expiration ; → Gestion carte | `CardService` / `CardDAO` / `Card` |

### 18.4 TRANSACTIONS

| Écran | Objectif et composants | Données, actions, suite | Service / DAO / Model |
| --- | --- | --- | --- |
| Historique | Lister les opérations ; `TableView`, `DatePicker` ×2, `ComboBox` type | Date, type, montant, libellé, statut ; filtre ; clic → Détail opération | `TransactionService` / `TransactionDAO` / `Transaction` |
| Détail opération | Voir une opération | Référence, contrepartie, solde après ; Retour | idem |
| Virement interne | Entre ses comptes ; 2 `ComboBox<Account>`, `TextField`, `Button` | Compte source/destination, montant, motif ; → Confirmation → Succès | `TransferService` / `AccountDAO`, `TransactionDAO` / `Account`, `Transaction` |
| Virement vers bénéficiaire | Vers un tiers ; `ComboBox<Beneficiary>`, `TextField` | Bénéficiaire, montant, motif ; → Confirmation → Succès ; « Nouveau bénéficiaire » | `TransferService` / `BeneficiaryDAO`, ... / `Beneficiary`, ... |
| Liste des bénéficiaires | Gérer ; `TableView`/`ListView`, boutons | Nom, RIB, banque ; Ajouter, Supprimer → formulaire | `BeneficiaryService` / `BeneficiaryDAO` / `Beneficiary` |
| Ajout bénéficiaire | Créer ; `GridPane`, `TextField` | Nom, RIB, banque ; Enregistrer → liste ou virement | idem (validation du format RIB dans le Service) |
| Confirmation | Valider (code) | Récapitulatif de l'opération | `SecurityService` |
| Succès / reçu | Résultat ; `Label`, boutons | Référence, montant, nouveau solde ; → Dashboard, Historique | `TransferReceipt` (record) |

### 18.5 MES PRODUITS / SERVICES

| Écran | Objectif et composants | Données, actions, suite | Service / DAO / Model |
| --- | --- | --- | --- |
| Gestion carte | Bloquer/débloquer, plafond, paiement en ligne ; `ToggleButton`/`CheckBox` | Statut, plafonds ; sauvegarde (avec confirmation) | `CardService` / `CardDAO` / `Card` |
| Recharge carte prépayée | Alimenter depuis un compte ; `ComboBox`, `TextField` | Compte source, carte, montant → Confirmation → Reçu | `CardService` / `CardDAO`, `AccountDAO`, `TransactionDAO` / `Card`, `Account` |
| Paiement de factures | Payer ; `ListView<Bill>`, `ComboBox<Account>` | Créancier, référence, montant ; Payer → Confirmation → Reçu | `BillService` / `BillDAO`, `AccountDAO`, `TransactionDAO` / `Bill`, `BillPayment` |
| Cheque book (chéquier) | Demande/suivi (simulation) | Statut de demande ; Demander | `ChequebookService` / `ChequebookDAO` (à créer) / `ChequebookRequest` |
| Produits et services | Catalogue informatif ; cartes de présentation | Libellé, description ; → Contact conseiller | `ProductService` / `ProductDAO` (ou fichier) / `Product` |

### 18.6 MENU, PROFIL / PARAMÈTRES, DOCUMENTS, CONTACT

| Écran | Objectif et composants | Données, actions, suite | Service / DAO / Model |
| --- | --- | --- | --- |
| Menu / shell | Navigation persistante ; `BorderPane`, `VBox` de boutons | Entrées → `Route` ; Déconnexion | `Navigator`, `SessionManager` |
| Profil | Voir/modifier informations ; `GridPane`, `TextField` | Nom, email, téléphone, adresse ; Enregistrer | `ProfileService` / `CustomerDAO` / `Customer` |
| Paramètres / sécurité | Changer le mot de passe ; `PasswordField` ×3 | Ancien, nouveau, confirmation | `AuthService` / `UserDAO` / `User` |
| Notifications | Lire/marquer lu ; `ListView<Notification>` | Titre, date, lu/non lu | `NotificationService` / `NotificationDAO` / `Notification` |
| Documents / relevés | Consulter/exporter ; `TableView<Document>`, `DatePicker` | Type, période ; Ouvrir/Exporter (PDF généré ou fichier fictif) | `DocumentService` / `DocumentDAO` / `Document` |
| Contact / conseiller | Informations du conseiller ; `Label`, `Hyperlink` | Nom, téléphone, email, agence | `AdvisorService` / `AdvisorDAO` / `Advisor` |
| Assistance (FAQ) | Aide ; `Accordion`/`ListView` | Questions fréquentes (contenu statique) | — |

Les écrans signalés « (à créer) » ou « simulation » sont des choix académiques : **À adapter selon l'observation de l'application réelle.**

---

## 19. Parcours utilisateur complets

| # | Parcours |
| --- | --- |
| P1 | LOGIN → DASHBOARD → ACCOUNT LIST → ACCOUNT DETAIL → TRANSACTION HISTORY → TRANSACTION DETAIL |
| P2 | DASHBOARD → TRANSFER → INTERNAL TRANSFER → SELECT ACCOUNTS → AMOUNT → CONFIRM → SUCCESS → HISTORY |
| P3 | DASHBOARD → TRANSFER → BENEFICIARY → NEW BENEFICIARY → ENTER RIB → SAVE → TRANSFER → CONFIRM → SUCCESS |
| P4 | DASHBOARD → BILLS → SELECT BILL → SELECT ACCOUNT → CONFIRM → RECEIPT → HISTORY |
| P5 | DASHBOARD → CARDS → CARD DETAIL → TOP-UP → SELECT ACCOUNT → AMOUNT → CONFIRM → RECEIPT |
| P6 | DASHBOARD → CARDS → CARD DETAIL → BLOCK / SET LIMIT → CONFIRM → CARD DETAIL |
| P7 | DASHBOARD → DOCUMENTS → SELECT PERIOD → OPEN / EXPORT |
| P8 | DASHBOARD → PROFILE → EDIT → SAVE → PROFILE (session mise à jour) |
| P9 | DASHBOARD → NOTIFICATIONS → NOTIFICATION DETAIL → related screen |
| P10 | DASHBOARD → CONTACT → ADVISOR INFO |
| P11 | ANY SCREEN → LOGOUT → LOGIN |

Chaque parcours devient **un scénario de test d'intégration** : tu le déroules à la main après chaque merge dans `develop`, avec les données de `seed.sql`.

---

## 20. Checklist de l'intégrateur

« Je suis capable d'intégrer le projet si je sais… »

**NIVEAU 1 — Architecture**

- [ ] Dessiner la chaîne View → Controller → Service → DAO → DB (aller et retour) pour un virement.
- [ ] Dire pour chaque classe d'un module à quelle couche elle appartient et ce qu'elle n'a pas le droit d'importer.
- [ ] Repérer un Controller qui contourne le Service.

**NIVEAU 2 — Java**

- [ ] Lire interfaces, enums, `record`, exceptions, `Optional`, streams et lambdas dans le code d'un autre.
- [ ] Manipuler l'argent avec `BigDecimal` et les dates avec `java.time`.
- [ ] Fermer correctement les ressources (try-with-resources).

**NIVEAU 3 — JavaFX**

- [ ] Expliquer Stage/Scene/Parent/Node et choisir le bon conteneur de layout.
- [ ] Relier `fx:id`, `fx:controller`, `onAction` aux champs et méthodes `@FXML`.
- [ ] Remplir une `TableView`/`ComboBox` avec une `ObservableList`.

**NIVEAU 4 — Database**

- [ ] Lire/écrire un DAO JDBC avec `PreparedStatement` et mapping `ResultSet` → Model.
- [ ] Démarrer la base, exécuter `schema.sql` puis `seed.sql` sur une base vide.
- [ ] Diagnostiquer une `SQLException` depuis un client SQL.

**NIVEAU 5 — Business logic**

- [ ] Expliquer pourquoi un virement est une seule transaction DB et repérer un `commit` mal placé.
- [ ] Vérifier qu'un Service contrôle propriétaire, statut, solde et montant.
- [ ] Distinguer `BusinessException` et `DataAccessException`.

**NIVEAU 6 — Navigation**

- [ ] Ajouter une `Route` et son FXML, et la brancher au menu.
- [ ] Transmettre une donnée à un écran avec `initData` et lire l'utilisateur via `SessionManager`.
- [ ] Garantir un seul `Stage` et aucune dépendance Controller → Controller.

**NIVEAU 7 — Integration**

- [ ] Tenir `CONTRACT.md` à jour (packages, Models, signatures de Services, routes).
- [ ] Câbler un nouveau module dans `ServiceRegistry`.
- [ ] Appliquer la grille en 8 questions à n'importe quel module livré.
- [ ] Dérouler les parcours P1 à P11 sans erreur.

**NIVEAU 8 — Debugging**

- [ ] Retrouver la cause racine dans une trace (`Caused by:`) et appliquer la table de la section 17.
- [ ] Repérer un appel DB sur le FX thread ou une modification d'interface hors du FX thread.

**NIVEAU 9 — Git/Maven**

- [ ] Tenir `main` / `develop` / `feature/*`, relire une pull request avec la checklist 16.3.
- [ ] Résoudre un conflit sur `Route`, `ServiceRegistry` ou `schema.sql`.
- [ ] Exécuter `mvn clean verify` et `mvn javafx:run`.

**NIVEAU 10 — Final packaging**

- [ ] Produire un livrable qui démarre sur une autre machine : `mvn clean package`, jar exécutable avec ses dépendances (plugin `maven-shade-plugin` ou `maven-assembly-plugin`) et classe `Launcher`.
- [ ] Option : créer un installateur/une image avec `jpackage` (JDK 17+) pour la démo.
- [ ] Fournir `README` : prérequis (JDK, MySQL), création de la base, commande de lancement, comptes de test.
- [ ] Préparer un jeu de données de démo (`seed.sql`) cohérent avec les parcours P1 à P11.

---

*Plan d'action conseillé pour démarrer cette semaine : (1) valider avec l'équipe l'arborescence, les Models et le schéma ; (2) créer le squelette `Main` + `Navigator` + `SessionManager` + `ServiceRegistry` + shell vide ; (3) pousser sur `develop` ; (4) chaque membre branche son module sur ce squelette.*
