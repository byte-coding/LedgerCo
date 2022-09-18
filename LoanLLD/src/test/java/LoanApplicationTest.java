import controller.LoanController;
import controller.PaymentController;
import model.Balance;
import org.junit.jupiter.api.Assertions;
import service.*;
import util.InMemoryLockUtilImpl;
import util.LockUtil;

public class LoanApplicationTest {
    static UserService userService = new UserServiceImpl();
    static BankService bankService = new BankServiceImpl();

    static LockUtil lockUtil = new InMemoryLockUtilImpl();
    static PaymentService paymentService = new PaymentServiceImpl(lockUtil);
    static LoanService loanService = new LoanServiceImpl(paymentService, lockUtil);

    static LoanController loanController = new LoanController(loanService, bankService, userService);
    static PaymentController paymentController = new PaymentController(paymentService, bankService, userService, loanService);
    public static void main(String[] args) {
        createLoanPaymentbalance();
    }

    private static void createLoanPaymentbalance() {
        loanController.create("IDIDI", "Dale", 10000, 5, 4);
        loanController.create("MBI", "Harry", 2000, 2, 2);

        Balance balance = paymentController.balance("IDIDI", "Dale", 5);
        Assertions.assertEquals(balance.toString() ,"IDIDI Dale 1000 55");

        balance = paymentController.balance("IDIDI", "Dale", 40);
        Assertions.assertEquals(balance.toString() ,"IDIDI Dale 8000 20");

        balance = paymentController.balance("MBI", "Harry", 12);
        Assertions.assertEquals(balance.toString() ,"MBI Harry 1044 12");

        balance = paymentController.balance("MBI", "Harry", 0);
        Assertions.assertEquals(balance.toString() ,"MBI Harry 0 24");
    }
}
