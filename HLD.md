# LedgerCo — High-Level Design (HLD)

## 1. Overview

LedgerCo is an in-memory loan management system that models the lifecycle of a bank loan: origination, EMI calculation, lump-sum payment recording, and balance enquiry. The system is built as a console application in Java 8 that reads commands from a flat file and writes results to stdout.

---

## 2. Goals & Non-Goals

### Goals
- Allow multiple banks to offer loans to multiple users.
- Calculate monthly EMI (Equated Monthly Instalment) from principal, tenure, and interest rate.
- Accept lump-sum pre-payments against specific EMI cycles.
- Query outstanding balance and remaining EMI count at any cycle number.
- Ensure thread-safe concurrent access to shared loan state.

### Non-Goals
- Persistent storage (database, file system).
- HTTP / REST API exposure.
- Authentication or authorisation.
- Quarterly / yearly EMI types (stubs exist but are unimplemented).
- Partial-payment failure rollback / compensating transactions.

---

## 3. Functional Requirements

| # | Requirement |
|---|-------------|
| FR-1 | A user can take a loan from a bank with given principal, tenure (years), and annual interest rate. |
| FR-2 | Only one active loan is allowed per user-bank pair. |
| FR-3 | A user can make a lump-sum payment against any EMI cycle number. |
| FR-4 | A user can query their remaining balance (amount outstanding + EMI cycles left) at any cycle number. |
| FR-5 | Concurrent operations on the same loan must be serialised. |

---

## 4. Non-Functional Requirements

| # | Requirement |
|---|-------------|
| NFR-1 | All state is in-memory; no I/O persistence. |
| NFR-2 | Thread safety guaranteed via per-EMI locking with a 20-second TTL. |
| NFR-3 | EMI payment and lock registration complete asynchronously during loan creation. |
| NFR-4 | Integer arithmetic; monetary amounts rounded up (ceiling). |

---

## 5. System Context Diagram

```
┌─────────────────────────────────────────────────────┐
│                    External Actor                   │
│             (input.txt / future caller)             │
└───────────────────────────┬─────────────────────────┘
                            │  LOAN / PAYMENT / BALANCE
                            ▼
┌─────────────────────────────────────────────────────┐
│                   LedgerCo System                   │
│                                                     │
│   ┌──────────┐    ┌──────────────┐   ┌──────────┐  │
│   │  Driver  │───▶│  Controllers │──▶│ Services │  │
│   └──────────┘    └──────────────┘   └──────────┘  │
│        │                                    │       │
│        ▼                               In-Memory    │
│   input.txt                            Store        │
│                                                     │
└─────────────────────────────────────────────────────┘
            │
            ▼
        stdout  (BALANCE output)
```

---

## 6. High-Level Architecture

The system follows a **layered architecture** with three logical tiers:

```
┌───────────────────────────────────────────────────────┐
│   Presentation / Input Layer                          │
│   Driver.java — parses input.txt line by line         │
└───────────────────────┬───────────────────────────────┘
                        │
┌───────────────────────▼───────────────────────────────┐
│   Controller Layer                                    │
│   LoanController  ·  PaymentController                │
└───────────────────────┬───────────────────────────────┘
                        │
┌───────────────────────▼───────────────────────────────┐
│   Service / Business-Logic Layer                      │
│   LoanService  ·  PaymentService                      │
│   UserService  ·  BankService                         │
└───────────────────────┬───────────────────────────────┘
                        │
┌───────────────────────▼───────────────────────────────┐
│   In-Memory Data Store                                │
│   HashMap / ConcurrentHashMap collections             │
└───────────────────────────────────────────────────────┘
```

---

## 7. Core Components

### 7.1 Driver (Entry Point)
Reads each line from `input.txt`, tokenises by space, and delegates to the appropriate controller based on the command keyword (`LOAN`, `PAYMENT`, `BALANCE`).

### 7.2 Controllers
Stateless command handlers that resolve entity names to domain objects and forward calls to services.

| Controller | Responsibility |
|------------|----------------|
| `LoanController` | Handles `LOAN` commands; orchestrates user/bank lookup and loan creation. |
| `PaymentController` | Handles `PAYMENT` and `BALANCE` commands; routes to `PaymentService`. |

### 7.3 Services
The business-logic core. Each service owns the lifecycle of its entity type.

| Service | Responsibility |
|---------|----------------|
| `UserService` | Create / lookup `User` by name. |
| `BankService` | Create / lookup `Bank` by name. |
| `LoanService` | Create loan (EMI object), enforce uniqueness per user-bank pair, spawn async tasks for EMI calculation and lock registration. |
| `PaymentService` | Calculate EMI payment schedules, record lump-sum payments, compute balance at a given cycle. |

### 7.4 Lock Utility
`InMemoryLockUtilImpl` provides per-EMI mutual-exclusion using `ConcurrentHashMap` and Java `synchronized` blocks. Locks carry a 20-second TTL to prevent starvation.

---

## 8. Key Workflows (High Level)

### 8.1 Loan Creation
```
Input: LOAN <bank> <user> <principal> <years> <rate>

Driver → LoanController.create()
       → UserService.create() + BankService.create()
       → LoanService.create()
            ├── [Thread A] PaymentService.createEmiPayment()  ← calculates EMI schedule
            └── [Thread B] LockUtil.registerEmiDetail()        ← registers lock slot
       ← join both threads
```

### 8.2 Payment Recording
```
Input: PAYMENT <bank> <user> <lumpSum> <emiNumber>

Driver → PaymentController.pay()
       → PaymentService.pay()
            ├── acquire lock on EMI
            ├── append PaymentDetail to PaymentLedger
            └── release lock
```

### 8.3 Balance Query
```
Input: BALANCE <bank> <user> <emiNumber>

Driver → PaymentController.balance()
       → PaymentService.balance()
            ├── acquire lock on EMI
            ├── sum (scheduled payments + lump-sum payments) up to emiNumber
            ├── compute outstanding = totalDue - totalPaid
            ├── remaining cycles = ⌈outstanding / monthlyPayment⌉
            └── release lock
       → print: <bank> <user> <amountPaid> <emiLeft>
```

---

## 9. Data Flow Diagram

```
input.txt
    │
    ▼
┌──────────┐
│  Driver  │──tokenise──▶ [LOAN | PAYMENT | BALANCE]
└──────────┘
      │              │                  │
      ▼              ▼                  ▼
LoanController  PaymentController  PaymentController
      │              │                  │
      ▼              ▼                  ▼
 LoanService    PaymentService     PaymentService
      │        (record payment)    (read ledger)
      │              │                  │
      ▼              ▼                  ▼
 In-Memory       PaymentLedger      Balance
 Emi Store        (write)           object → stdout
```

---

## 10. Technology Choices

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Language | Java 8 | Mature, thread-safe concurrency primitives. |
| Build | Maven | Standard Java dependency management. |
| Boilerplate reduction | Lombok | Eliminates repetitive getters/setters. |
| Testing | JUnit 5 | Modern assertion API. |
| Persistence | None (in-memory) | Scope is an LLD exercise; persistence is out of scope. |
| Concurrency | `synchronized` + `ConcurrentHashMap` | Simple, sufficient for the scale of this exercise. |

---

## 11. Scalability & Extension Points

| Area | Current State | Future Option |
|------|---------------|---------------|
| Storage | In-memory HashMap | Swap services with JPA/Redis-backed repositories. |
| EMI types | Only MONTHLY | Implement QUARTERLY / YEARLY branches in `PaymentService`. |
| API | Command-line | Add Spring Boot REST layer on top of existing services. |
| Locking | In-process lock | Replace with distributed lock (Redis SETNX) for multi-node. |
| Concurrency model | Bare threads | Replace with `ExecutorService` / `CompletableFuture`. |

---

## 12. Assumptions & Constraints

1. A user can hold at most one loan per bank.
2. Interest is simple (not compound): `Interest = Principal × Years × Rate / 100`.
3. All monetary values are integers; fractional amounts are always rounded up.
4. The `BALANCE` query at EMI cycle *N* assumes all scheduled EMIs 1–N have been paid in addition to any recorded lump sums.
5. Application state is ephemeral — it is not preserved across restarts.
