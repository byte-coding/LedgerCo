package util;


import model.Emi;

public interface LockUtil {
    void lock(Emi emi);

    void unlock(Emi emi);

    void registerEmiDetail(Emi emi);
}
