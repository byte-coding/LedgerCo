# LedgerCo

A command-driven loan management system that handles loan origination, EMI calculation, lump-sum pre-payments, and balance enquiries — all in memory, with thread-safe concurrent access.

---

## Prerequisites

- JDK 8 or newer (tested on 17 and 21)
- Maven 3.6+

---

## Build & Run

```bash
git clone https://github.com/byte-coding/LedgerCo.git
cd LedgerCo/LoanLLD   # run everything from here: input.txt is resolved relative to this folder

# Build and run the tests
mvn clean package

# Run
mvn compile exec:java
```

The application reads commands from `src/main/resources/input.txt` and prints an `EMI App` header followed by one line per `BALANCE` command.

To run only the tests:
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
EMI App
IDIDI Dale 2000 50
MBI Harry 12336 60
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

**How `MBI Harry 12336 60` is derived:**
```
interest         = ⌈50000 × 6 × 8 / 100⌉ = 24000
totalDue         = 74000
monthlyPayment   = ⌈74000 / 72⌉ = 1028
totalPaid        = 1028 × 12 = 12336
emiLeft          = 72 − 12 = 60
```

---

## Project Structure

```
LedgerCo/
├── .gitignore
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
