package model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class EmiPayment {
    private Emi emi;
    private int recurringPayment;
    private int totalEmiCycle;
}
