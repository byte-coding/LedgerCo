import controller.LoanController;
import controller.PaymentController;
import model.Balance;
import service.*;
import util.InMemoryLockUtilImpl;
import util.LockUtil;

import javax.naming.OperationNotSupportedException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Driver {
    public static void main(String[] args) throws Exception {
        System.out.println("EMI App");

        UserService userService = new UserServiceImpl();
        BankService bankService = new BankServiceImpl();

        LockUtil lockUtil = new InMemoryLockUtilImpl();
        PaymentService paymentService = new PaymentServiceImpl(lockUtil);
        LoanService loanService = new LoanServiceImpl(paymentService, lockUtil);

        LoanController loanController = new LoanController(loanService, bankService, userService);
        PaymentController paymentController = new PaymentController(paymentService, bankService, userService, loanService);

        File file = new File(System.getProperty("user.dir") + "/src/main/resources/" + "input.txt");
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String line;
        List<String> inputs = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            inputs.add(line);
        }

        for (String input : inputs) {
            String[] splitInput = input.split(" ");
            String commandType = splitInput[0];
            String bankName = splitInput[1];
            String browerName = splitInput[2];
            int emiNumber;
            switch (commandType) {
                case "LOAN":
                    int principal = Integer.parseInt(splitInput[3]);
                    int years = Integer.parseInt(splitInput[4]);
                    int rate = Integer.parseInt(splitInput[5]);
                    loanController.create(bankName, browerName, principal, years, rate);
                    break;
                case "PAYMENT":
                    int lumpsum = Integer.parseInt(splitInput[3]);
                    emiNumber = Integer.parseInt(splitInput[4]);
                    paymentController.pay(bankName, browerName, lumpsum, emiNumber);
                    break;
                case "BALANCE":
                    emiNumber = Integer.parseInt(splitInput[3]);
                    Balance balance = paymentController.balance(bankName, browerName, emiNumber);
                    System.out.println(balance.getBankName() + " " + balance.getUserName() + " " + balance.getAmountPaid() + " " + balance.getNoOfEmiLeft());
                    break;
                default:
                    throw new OperationNotSupportedException();
            }
        }
    }
}
