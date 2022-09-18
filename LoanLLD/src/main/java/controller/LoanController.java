package controller;

import model.Bank;
import model.User;
import service.BankService;
import service.LoanService;
import service.UserService;

public class LoanController {

    private LoanService loanService;
    private BankService bankService;
    private UserService userService;

    public LoanController(LoanService loanService, BankService bankService, UserService userService) {
        this.loanService = loanService;
        this.bankService = bankService;
        this.userService = userService;
    }

    public void create(String bankName, String userName, int principalAmount, int noOfYears, int rateOfInterest) {
        User user = userService.create(userName);
        Bank bank = bankService.create(bankName);
        loanService.create(bank, user, principalAmount, noOfYears, rateOfInterest);
    }
}
