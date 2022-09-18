package model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Balance {
    private String bankName;
    private String userName;
    private int amountPaid;
    private int noOfEmiLeft;

    public String toString() {
        return bankName + " " + userName + " " + amountPaid + " " + noOfEmiLeft;
    }
}
