package service;


import model.Bank;

import java.util.HashMap;
import java.util.Map;

public class BankServiceImpl implements BankService {
    private Map<String, Bank> bankMap = new HashMap<>();

    @Override
    public Bank create(String name) {
        if (bankMap.containsKey(name)) {
            return bankMap.get(name);
        }
        bankMap.put(name, new Bank(name));
        return bankMap.get(name);
    }

    @Override
    public Bank get(String name) {
        if (!bankMap.containsKey(name)) {
            throw new RuntimeException("Bank not Found!");
        }
        return bankMap.get(name);
    }
}
