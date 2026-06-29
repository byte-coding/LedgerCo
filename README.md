# LedgerCo

A command-driven loan management system that handles loan origination, EMI calculation, lump-sum pre-payments, and balance enquiries — all in memory, with thread-safe concurrent access.

---

## Prerequisites

- Java 8+
- Maven 3.6+

---

## Build & Run

```bash
# Build
mvn clean package

# Run
mvn exec:java -Dexec.mainClass="Driver"
```

The application reads commands from `src/main/resources/input.txt` and prints `BALANCE` results to stdout.

To run tests:
```bash
mvn test
```

---

## Command Reference

### LOAN
Create a loan for a user at a bank.
```
LOAN <bankName> <userName> <principal> <years> <rateOfInterest>
```

### PAYMENT
Record a lump-sum pre-payment against a specific EMI cycle.
```
PAYMENT <bankName> <userName> <lumpSum> <emiNumber>
```

### BALANCE
Query total amount paid and EMI cycles remaining at a given cycle number.
```
BALANCE <bankName> <userName> <emiNumber>
```
Output format: `<bankName> <userName> <amountPaid> <emiLeft>`

---

## Example

**input.txt**
```
LOAN IDIDI Dale 10000 5 4
LOAN MBI Harry 50000 6 8
PAYMENT IDIDI Dale 1000 5
BALANCE IDIDI Dale 5
BALANCE MBI Harry 12
```

**stdout**
```
IDIDI Dale 2000 50
MBI Harry 16800 60
```

**How `IDIDI Dale 2000 50` is derived:**
```
interest         = ⌈10000 × 5 × 4 / 100⌉ = 2000
totalDue         = 12000
monthlyPayment   = ⌈12000 / 60⌉ = 200
scheduled paid   = 200 × 5 = 1000
lump sum paid    = 1000  (PAYMENT at EMI 5)
totalPaid        = 2000
outstanding      = 12000 − 2000 = 10000
emiLeft          = ⌈10000 / 200⌉ = 50
```

---

## Project Structure

```
LedgerCo/
├── HLD.md                          # High-Level Design
├── LLD.md                          # Low-Level Design
└── LoanLLD/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/
        │   │   ├── Driver.java                     # Entry point
        │   │   ├── controller/
        │   │   │   ├── LoanController.java
        │   │   │   └── PaymentController.java
        │   │   ├── model/                          # Domain entities
        │   │   │   ├── Emi.java
        │   │   │   ├── EmiPayment.java
        │   │   │   ├── PaymentLedger.java
        │   │   │   ├── Balance.java
        │   │   │   └── ...
        │   │   ├── service/                        # Business logic
        │   │   │   ├── LoanServiceImpl.java
        │   │   │   ├── PaymentServiceImpl.java
        │   │   │   └── ...
        │   │   └── util/
        │   │       └── InMemoryLockUtilImpl.java   # Per-EMI locking
        │   └── resources/
        │       └── input.txt                       # Command input
        └── test/
            └── java/
                └── LoanApplicationTest.java
```

---

## Design Docs

- [High-Level Design (HLD)](HLD.md) — architecture, workflows, tech choices
- [Low-Level Design (LLD)](LLD.md) — class diagram, algorithms, concurrency model, edge cases
