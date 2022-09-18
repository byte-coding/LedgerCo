package util;

import model.Bank;
import model.Emi;
import model.LockDetail;
import model.User;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLockUtilImpl implements LockUtil {
    private final Map<User, Map<Bank, Emi>> userBankEmiDetailMapping = new ConcurrentHashMap<>();
    private final Map<User, Map<Bank, LockDetail>> lockDetailMap = new ConcurrentHashMap<>();
    private final int lockDurationInSeconds = 20;

    @Override
    public void lock(Emi emi) {
        if (userBankEmiDetailMapping.getOrDefault(emi.getUser(), new HashMap<>()).get(emi.getBank()) == null) {
            throw new RuntimeException("User Emi Details not found in InMemoryLocking");
        }
        synchronized (userBankEmiDetailMapping.get(emi.getUser()).get(emi.getBank())) {
            if (!userBankEmiDetailMapping.getOrDefault(emi.getUser(), new HashMap<>()).containsKey(emi.getBank())) {
                throw new RuntimeException("User Emi Details not found in InMemoryLocking!");
            }
            if (lockDetailMap.getOrDefault(emi.getUser(), new HashMap<>()).containsKey(emi.getBank())) {
                throw new RuntimeException("Already locked by the current user!");
            }
            LockDetail<Emi> lockDetail = new LockDetail<>(emi, new Date(), lockDurationInSeconds);
            lockDetailMap.putIfAbsent(emi.getUser(), new HashMap<>());
            lockDetailMap.get(emi.getUser()).put(emi.getBank(), lockDetail);
        }
    }

    @Override
    public void unlock(Emi emi) {
        if (userBankEmiDetailMapping.getOrDefault(emi.getUser(), new HashMap<>()).get(emi.getBank()) == null) {
            throw new RuntimeException("User Emi Details not found in InMemoryLocking");
        }
        synchronized (userBankEmiDetailMapping.get(emi.getUser()).get(emi.getBank())) {
            if (!userBankEmiDetailMapping.getOrDefault(emi.getUser(), new HashMap<>()).containsKey(emi.getBank())) {
                throw new RuntimeException("User Emi Details not found in InMemoryLocking");
            }
            if (!lockDetailMap.getOrDefault(emi.getUser(), new HashMap<>()).containsKey(emi.getBank())) {
                throw new RuntimeException("Already unlocked by the current user!");
            }
            lockDetailMap.get(emi.getUser()).remove(emi.getBank());
        }
    }

    public void registerEmiDetail(Emi emi) {
        //thread safe due to conncurrent hashmap bring used
        userBankEmiDetailMapping.putIfAbsent(emi.getUser(), new ConcurrentHashMap<>());
        userBankEmiDetailMapping.get(emi.getUser()).put(emi.getBank(), emi);
    }
}
