package service;


import model.Bank;
import model.Emi;
import model.User;

public interface LoanService {
    void create(Bank bank, User user, int principalAmount, int noOfYears, int rateOfInterest);

    Emi get(Bank bank, User user);
}
