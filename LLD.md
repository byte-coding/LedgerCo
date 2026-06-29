# LedgerCo — Low-Level Design (LLD)

## 1. Package Structure

```
src/main/java/
├── Driver.java                        # Application entry point
├── controller/
│   ├── LoanController.java
│   └── PaymentController.java
├── model/
│   ├── Bank.java
│   ├── Balance.java
│   ├── Emi.java
│   ├── EmiPayment.java
│   ├── EmiType.java          (enum)
│   ├── LockDetail.java
│   ├── PaymentLedger.java
│   ├── PaymentStatus.java    (enum)
│   └── User.java
├── service/
│   ├── BankService.java      (interface)
│   ├── BankServiceImpl.java
│   ├── LoanService.java      (interface)
│   ├── LoanServiceImpl.java
│   ├── PaymentService.java   (interface)
│   ├── PaymentServiceImpl.java
│   ├── UserService.java      (interface)
│   └── UserServiceImpl.java
└── util/
    ├── LockUtil.java         (interface)
    └── InMemoryLockUtilImpl.java

src/main/resources/
└── input.txt
```

---

## 2. Class Diagram

```
                        ┌────────────┐
                        │   Driver   │
                        └─────┬──────┘
                              │ uses
             ┌────────────────┼────────────────┐
             ▼                                  ▼
   ┌──────────────────┐               ┌───────────────────┐
   │  LoanController  │               │ PaymentController │
   └────────┬─────────┘               └─────────┬─────────┘
            │ uses                              │ uses
            ▼                                  ▼
  ┌──────────────────┐              ┌────────────────────┐
  │   LoanService    │◄─────────────│   PaymentService   │
  │   (interface)    │  createEmi   │   (interface)      │
  └────────┬─────────┘              └─────────┬──────────┘
           │impl                              │impl
  ┌────────▼─────────┐              ┌─────────▼──────────┐
  │  LoanServiceImpl │              │ PaymentServiceImpl  │
  └────────┬─────────┘              └─────────┬──────────┘
           │ uses                             │ uses
  ┌────────▼─────────┐              ┌─────────▼──────────┐
  │   UserService    │              │      LockUtil       │
  │   BankService    │              │    (interface)      │
  └──────────────────┘              └─────────┬──────────┘
                                              │impl
                                   ┌──────────▼─────────┐
                                   │ InMemoryLockUtilImpl│
                                   └────────────────────┘

Domain Models (all @Data Lombok):
  User · Bank · Emi · EmiPayment · PaymentLedger · Balance · LockDetail<T>

Enums:
  EmiType {MONTHLY, QUARTELY, YEAR}
  PaymentStatus {SUCCESS, FAILURE}
```

---

## 3. Domain Model Details

### 3.1 User
```java
@Data
@AllArgsConstructor
class User {
    String name;   // natural key
}
```

### 3.2 Bank
```java
@Data
@AllArgsConstructor
class Bank {
    String name;   // natural key
}
```

### 3.3 Emi
Central aggregate — represents one active loan.

```java
@Data
@AllArgsConstructor
class Emi {
    User    user;
    Bank    bank;
    EmiType type;           // MONTHLY (only implemented)
    int     amount;         // principal
    int     noOfYears;
    int     rateOfInterest; // annual %
    Date    createdAt;
}
```

### 3.4 EmiPayment
Derived from `Emi`; holds the calculated schedule.

```java
@Data
@AllArgsConstructor
class EmiPayment {
    Emi emi;
    int recurringPayment;   // ⌈(principal + interest) / (years × 12)⌉
    int totalEmiCycle;      // years × 12
}
```

**Calculation:**
```
totalDue          = principal + ⌈principal × years × rate / 100⌉
recurringPayment  = ⌈totalDue / totalEmiCycle⌉
totalEmiCycle     = years × 12
```

### 3.5 PaymentLedger + PaymentDetail
Append-only ledger for all payments on a loan.

```java
@Data
class PaymentLedger {
    Emi emi;
    List<PaymentDetail> paymentDetails = new ArrayList<>();

    @Data
    @AllArgsConstructor
    static class PaymentDetail {
        int           amountPaid;
        Date          paidOn;
        int           emiNumber;   // which cycle this lump sum targets
        PaymentStatus status;      // SUCCESS | FAILURE
    }
}
```

### 3.6 Balance (read model / output DTO)
```java
@Data
@AllArgsConstructor
class Balance {
    String bankName;
    String userName;
    int    amountPaid;   // total paid up to queried cycle
    int    noOfEmiLeft;  // remaining cycles = ⌈outstanding / recurringPayment⌉
}
```

### 3.7 LockDetail<T>
```java
@Data
@AllArgsConstructor
class LockDetail<T> {
    T    data;
    Date lockTime;
    int  lockDurationInSeconds;   // 20 seconds
}
```

---

## 4. Service Interface Contracts

### 4.1 UserService
```java
interface UserService {
    User create(String name);    // idempotent — returns existing if present
    User get(String name);
}
```
**Internal state:** `HashMap<String, User> userMap`

### 4.2 BankService
```java
interface BankService {
    Bank create(String name);    // idempotent
    Bank get(String name);
}
```
**Internal state:** `HashMap<String, Bank> bankMap`

### 4.3 LoanService
```java
interface LoanService {
    Emi create(User user, Bank bank,
               int principal, int years, int rate);
    Emi get(User user, Bank bank);
}
```
**Internal state:** `HashMap<User, HashMap<Bank, Emi>> userEmiMap`

**Uniqueness invariant:** throws / returns null if `userEmiMap[user][bank]` already exists.

### 4.4 PaymentService
```java
interface PaymentService {
    void       createEmiPayment(Emi emi);
    void       pay(Emi emi, int lumpSum, int emiNumber);
    Balance    balance(Emi emi, int emiNumber);
}
```
**Internal state:**
- `HashMap<Emi, EmiPayment>       emiPaymentMap`
- `HashMap<Emi, PaymentLedger>    emiPaymentLedgerMap`

### 4.5 LockUtil
```java
interface LockUtil {
    void   registerEmiDetail(User user, Bank bank, Emi emi);
    Emi    getEmiDetail(User user, Bank bank);
}
```
**Internal state:**
- `ConcurrentHashMap<User, ConcurrentHashMap<Bank, Emi>>              emiMap`
- `ConcurrentHashMap<User, ConcurrentHashMap<Bank, LockDetail<Emi>>>  emiLockMap`

---

## 5. Sequence Diagrams

### 5.1 LOAN Command

```
Driver          LoanController      UserSvc   BankSvc   LoanSvc       PaymentSvc   LockUtil
  │                  │                 │         │          │               │           │
  │──LOAN cmd───────▶│                 │         │          │               │           │
  │                  │──create(user)──▶│         │          │               │           │
  │                  │◀── User ────────│         │          │               │           │
  │                  │──create(bank)───────────▶│          │               │           │
  │                  │◀── Bank ──────────────────│          │               │           │
  │                  │──create(u,b,p,y,r)──────────────────▶│              │           │
  │                  │                 │         │          │──spawn T1─────▶            │
  │                  │                 │         │          │──spawn T2──────────────────▶│
  │                  │                 │         │          │  [join T1, T2]              │
  │                  │                 │         │     T1: createEmiPayment(emi)          │
  │                  │                 │         │     T2: registerEmiDetail(u,b,emi)     │
  │◀─────────────────────────────────────────────│               │           │           │
```

### 5.2 PAYMENT Command

```
Driver     PaymentController     LoanSvc    LockUtil      PaymentSvc
  │               │                 │           │              │
  │──PAYMENT────▶│                 │           │              │
  │               │──get(u,b)──────▶│           │              │
  │               │◀─── Emi ────────│           │              │
  │               │──pay(emi, amt, n)─────────────────────────▶│
  │               │                 │           │  acquire lock│
  │               │                 │           │◀─────────────│
  │               │                 │   synchronized(emi){     │
  │               │                 │      append PaymentDetail│
  │               │                 │   }                      │
  │               │                 │           │  release lock│
  │◀──────────────────────────────────────────────────────────│
```

### 5.3 BALANCE Command

```
Driver     PaymentController     LoanSvc    LockUtil      PaymentSvc
  │               │                 │           │              │
  │──BALANCE────▶│                 │           │              │
  │               │──get(u,b)──────▶│           │              │
  │               │◀─── Emi ────────│           │              │
  │               │──balance(emi,n)────────────────────────────▶│
  │               │                 │  acquire lock             │
  │               │                 │  sumLumpSums(≤n)          │
  │               │                 │  totalPaid = n×recurring + sumLumps
  │               │                 │  outstanding = totalDue - totalPaid
  │               │                 │  emiLeft = ⌈outstanding/recurring⌉
  │               │                 │  release lock             │
  │               │◀──────── Balance object ──────────────────│
  │◀─ print ──────│
```

---

## 6. Concurrency Design

### 6.1 Loan Creation — Asynchronous Init

`LoanServiceImpl.create()` spawns two threads and waits on both:

```java
Thread t1 = new Thread(() -> paymentService.createEmiPayment(emi));
Thread t2 = new Thread(() -> lockUtil.registerEmiDetail(user, bank, emi));
t1.start(); t2.start();
t1.join();  t2.join();
```

Both tasks are idempotent with respect to the same `Emi` instance, so there is no data race between them.

### 6.2 Payment / Balance — Per-EMI Lock

`InMemoryLockUtilImpl` stores a `LockDetail` per `(User, Bank)` pair. The `PaymentServiceImpl` acquires the `Emi` object via `lockUtil.getEmiDetail()` and uses it as the monitor:

```java
Emi lock = lockUtil.getEmiDetail(user, bank);
synchronized (lock) {
    // safe read or write on emiPaymentLedgerMap
}
```

**Lock TTL:** `LockDetail.lockDurationInSeconds = 20`. The TTL is stored but not currently enforced in code (no watchdog thread). It documents intent for future implementation.

### 6.3 Thread-Safety Summary

| Data Structure | Access Pattern | Guard |
|----------------|---------------|-------|
| `userMap` (UserService) | Single-threaded (Driver loop) | None needed |
| `bankMap` (BankService) | Single-threaded | None needed |
| `userEmiMap` (LoanService) | Single-threaded | None needed |
| `emiPaymentMap` (PaymentService) | Written once by T1, read later | Thread join before access |
| `emiPaymentLedgerMap` | Concurrent R/W | `synchronized(emiObject)` |
| `emiMap` / `emiLockMap` (LockUtil) | Concurrent init by T2 | `ConcurrentHashMap` |

---

## 7. Algorithm: Balance Calculation

```
Inputs:
  emi          — loan aggregate
  queryEmiNo   — the cycle number being queried

Lookup:
  emiPayment  ← emiPaymentMap[emi]
  ledger      ← emiPaymentLedgerMap[emi]

Step 1 — Aggregate lump-sum payments up to queryEmiNo:
  lumpSum = Σ { p.amountPaid | p ∈ ledger.paymentDetails
                               ∧ p.emiNumber ≤ queryEmiNo
                               ∧ p.status = SUCCESS }

Step 2 — Add scheduled payments:
  totalPaid = lumpSum + (emiPayment.recurringPayment × queryEmiNo)

Step 3 — Compute outstanding and remaining cycles:
  outstanding = (emi.amount + interest) − totalPaid
             where interest = ⌈emi.amount × emi.noOfYears × emi.rateOfInterest / 100⌉
  emiLeft     = max(0, ⌈outstanding / emiPayment.recurringPayment⌉)

Output: Balance(bankName, userName, totalPaid, emiLeft)
```

---

## 8. Input / Output Format

### Input (`input.txt`)
```
LOAN    <bankName> <userName> <principal> <years> <rateOfInterest>
PAYMENT <bankName> <userName> <lumpSum>   <emiNumber>
BALANCE <bankName> <userName> <emiNumber>
```

### Output (stdout — only for BALANCE)
```
<bankName> <userName> <amountPaid> <noOfEmiLeft>
```

### Worked Example
```
Input:
  LOAN IDIDI Dale 10000 5 4
  BALANCE IDIDI Dale 5

Calculation:
  interest        = ⌈10000 × 5 × 4 / 100⌉ = 2000
  totalDue        = 12000
  totalEmiCycle   = 60
  recurringPayment= ⌈12000 / 60⌉ = 200
  At EMI 5:
    totalPaid   = 200 × 5 = 1000
    outstanding = 12000 − 1000 = 11000
    emiLeft     = ⌈11000 / 200⌉ = 55

Output:
  IDIDI Dale 1000 55
```

```
Input:
  LOAN IDIDI Dale 10000 5 4
  PAYMENT IDIDI Dale 1000 5
  BALANCE IDIDI Dale 5

Calculation (with lump sum):
  lumpSumBefore5  = 1000
  totalPaid       = 1000 + 200×5 = 2000
  outstanding     = 12000 − 2000 = 10000
  emiLeft         = ⌈10000 / 200⌉ = 50

Output:
  IDIDI Dale 2000 50
```

---

## 9. Error / Edge-Case Handling

| Scenario | Current Behaviour |
|----------|------------------|
| Duplicate loan (same user + bank) | `LoanServiceImpl` checks map; returns silently without overwriting. |
| Unknown user / bank on PAYMENT or BALANCE | `UserService.get()` / `BankService.get()` return `null`; NPE propagates (no explicit guard). |
| Payment amount ≤ 0 | `PaymentServiceImpl.pay()` validates and skips ledger write. |
| EMI cycle number beyond total cycles | `balance()` computes `emiLeft = 0` (outstanding ≤ 0). |
| Concurrent payment on same EMI | Serialised via `synchronized(emiObject)` block. |

---

## 10. Testing

`LoanApplicationTest.java` exercises the full flow end-to-end:

| Test Case | Scenario |
|-----------|----------|
| `createLoanTest` | Single loan creation and balance at cycle 0. |
| `paymentTest` | Loan + lump-sum payment + balance verification. |
| `multipleLoansTest` | Two users with loans from different banks. |
| *(add)* Duplicate loan | Should not overwrite existing Emi. |
| *(add)* Concurrent payments | Two threads paying simultaneously — ledger must contain both. |

---

## 11. Known Limitations & Tech Debt

| # | Issue | Impact |
|---|-------|--------|
| 1 | Lock TTL not enforced (no watchdog) | Stale lock conceptually exists but never expires in code. |
| 2 | `EmiType.QUARTELY` is a typo for `QUARTERLY` | Minor; no runtime impact. |
| 3 | `QUARTELY` and `YEAR` EMI types unimplemented | `PaymentService` has no branch for these. |
| 4 | Bare `Thread` instead of `ExecutorService` | Harder to manage lifecycle; no rejection policy. |
| 5 | `UserService.get()` returns `null` (no `Optional`) | Callers must null-check; NPE risk on invalid commands. |
| 6 | No persistence | All state lost on restart. |
| 7 | Integer overflow risk | `principal × years × rate` may exceed `int` range for large values. |
