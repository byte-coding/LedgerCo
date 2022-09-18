package controller;

import model.Balance;
import model.Bank;
import model.Emi;
import model.User;
import service.BankService;
import service.LoanService;
import service.PaymentService;
import service.UserService;

public class PaymentController {

    private PaymentService paymentService;
    private BankService bankService;
    private UserService userService;
    private LoanService loanService;

    public PaymentController(PaymentService paymentService, BankService bankService, UserService userService, LoanService loanService) {
        this.paymentService = paymentService;
        this.bankService = bankService;
        this.userService = userService;
        this.loanService = loanService;
    }

    public void pay(String bankName, String userName, int lumpSumAmount, int emiNo) {
        User user = userService.get(userName);
        Bank bank = bankService.get(bankName);
        Emi emi = loanService.get(bank, user);
        paymentService.pay(emi, lumpSumAmount, emiNo);
    }

    public Balance balance(String bankName, String userName, int emiNo) {
        User user = userService.get(userName);
        Bank bank = bankService.get(bankName);
        Emi emi = loanService.get(bank, user);
        return paymentService.balance(emi, emiNo);
    }
}
