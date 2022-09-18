package service;


import model.Balance;
import model.Emi;

public interface PaymentService {

    void createEmiPayment(Emi emi);

    void pay(Emi emi, int lumpSumAmount, int emiNo);

    Balance balance(Emi emi, int emiNo);
}
