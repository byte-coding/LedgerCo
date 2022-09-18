package service;


import model.Bank;

public interface BankService {
    Bank create(String name);

    Bank get(String name);
}
