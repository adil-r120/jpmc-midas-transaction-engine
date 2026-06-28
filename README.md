# JPMC Midas Core - Software Engineering Program

This is the codebase for the JPMC Midas Core system. It is a backend application designed to process financial transactions in real-time.

Here is a breakdown of what has been built and how to run everything.

---

## What the system does (Task by Task)

### Task 1: Project Setup & Boot Verification
Set up the base dependencies in `pom.xml` (Spring Boot Web, Data JPA, Kafka, and H2 database) and configured `application.yml` on port `33400`. The test verifies that the Spring context starts up successfully.
- **Output code:** `---begin output --- 1142725631254665682354316777216387420489 ---end output ---`

### Task 2: Kafka Consumer
Implemented `TransactionListener` to listen to the `trader-updates` Kafka topic. It pulls transaction messages and deserializes them into Java objects.
- **Verification:** Tested via `TaskTwoTests`. The first four transaction amounts are: `122.86, 42.87, 161.79, 22.22`.

### Task 3: Database & Validation
Integrated H2 database persistence. When a transaction comes in:
- Checks if the sender and recipient exist in the database.
- Checks if the sender has enough balance.
- If valid, updates their balances, saves the changes, and records the transaction in the `TransactionRecord` table.
- **Waldorf's final balance:** `627` (rounded down from `627.86`).

### Task 4: Incentive API
Connected Midas Core to an external Incentive REST API (`http://localhost:8080/incentive`).
- Sends the transaction details to the endpoint, which calculates a reward (incentive).
- The incentive amount is added to the recipient's balance (not deducted from the sender).
- Saves the transaction along with the incentive in the database.
- **Wilbur's final balance:** `3089` (rounded down from `3089.42`).

### Task 5: Balance Query Endpoint
Added a GET `/balance?userId=X` endpoint to allow users to query their balance.
- Returns a `Balance` JSON object containing the balance amount.
- Returns a balance of `0` if the user doesn't exist.
- **Output code:**
  ```text
  ---begin output ---
  Balance {amount=0.0}
  Balance {amount=1326.98}
  Balance {amount=2567.52}
  Balance {amount=2740.33}
  Balance {amount=140.96999}
  Balance {amount=10.419973}
  Balance {amount=845.49005}
  Balance {amount=657.49}
  Balance {amount=99.189995}
  Balance {amount=3434.0002}
  Balance {amount=2157.1902}
  Balance {amount=779421.3}
  Balance {amount=0.0}
  ---end output ---
  ```

---

## Project Structure

The project classes are organized into functional packages under `src/main/java/com/jpmc/midascore`:

- **`component/`**: Base logic wrappers and event handlers.
  - `DatabaseConduit`: Helper conduit wrapping user/transaction entity access.
  - `TransactionListener`: Subscribes to the transaction topic on Kafka, processes validation, queries the Incentives API, and updates balances.
- **`controller/`**: Rest controllers exposing API endpoints.
  - `BalanceController`: GET `/balance?userId=X` endpoint handler.
- **`entity/`**: Database relational table models.
  - `UserRecord`: User database entity (id, name, balance).
  - `TransactionRecord`: Pinned transaction records containing sender, recipient, amount, and incentive.
- **`foundation/`**: Domain DTOs and configuration models.
  - `Transaction`, `Balance`, `Incentive`: JSON serializable data models.
- **`repository/`**: CRUD database queries.
  - `UserRepository`, `TransactionRepository`: Spring Data JPA repositories.

---

## How to Run & Test

Before running, set your Java 17+ environment variable in the terminal:
```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-23"
```

### 1. Run the Incentive API
Since Tasks 4 and 5 query the external Incentive microservice, run it in the background first:
```powershell
java -jar services/transaction-incentive-api.jar
```

### 2. Build the project
```powershell
.\mvnw.cmd clean install
```

### 3. Run individual tests
- **Task 1:** `.\mvnw.cmd test -Dtest=TaskOneTests`
- **Task 2:** `.\mvnw.cmd test -Dtest=TaskTwoTests`
- **Task 3:** `.\mvnw.cmd test -Dtest=TaskThreeTests`
- **Task 4:** `.\mvnw.cmd test -Dtest=TaskFourTests`
- **Task 5:** `.\mvnw.cmd test -Dtest=TaskFiveTests`
