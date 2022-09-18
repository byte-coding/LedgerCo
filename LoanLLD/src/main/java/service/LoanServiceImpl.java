package service;


import model.Bank;
import model.Emi;
import model.EmiType;
import model.User;
import util.LockUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LoanServiceImpl implements LoanService {

    private Map<User, Map<Bank, Emi>> userEmiMap = new HashMap<>();
    private PaymentService paymentService;
    private LockUtil lockUtil;

    public LoanServiceImpl(PaymentService paymentService, LockUtil lockUtil) {
        this.paymentService = paymentService;
        this.lockUtil = lockUtil;
    }

    @Override
    public void create(Bank bank, User user, int principalAmount, int noOfYears, int rateOfInterest) {
        if (checkIfLoanAlreadyExists(bank, user)) {
            throw new RuntimeException("Duplicate Entry Found for Loan");
        }
        Emi emi = new Emi(user, bank, EmiType.MONTHLY, principalAmount, noOfYears, rateOfInterest, new Date());
        userEmiMap.putIfAbsent(emi.getUser(), new HashMap<>());
        userEmiMap.get(emi.getUser()).put(bank, emi);

        //trigger for payment callback
        Thread emiPayment = new Thread(() -> paymentService.createEmiPayment(emi));
        Thread registerEmiDetailForInMemoryLock = new Thread(() -> lockUtil.registerEmiDetail(emi));

        emiPayment.start();
        registerEmiDetailForInMemoryLock.start();
        try {
            emiPayment.join();
            registerEmiDetailForInMemoryLock.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Emi get(Bank bank, User user) {
        if (!checkIfLoanAlreadyExists(bank, user)) {
            throw new RuntimeException("Loan doesn't exist");
        }
        return userEmiMap.get(user).get(bank);
    }

    private boolean checkIfLoanAlreadyExists(Bank bank, User user) {
        Emi emi = userEmiMap.getOrDefault(user, new HashMap<>()).get(bank);
        return emi != null && emi.getBank().equals(bank) && emi.getUser().equals(user);
    }
}
