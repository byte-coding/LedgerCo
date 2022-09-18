package model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Date;

@Getter
@ToString
@AllArgsConstructor
public class Emi {
    private User user;
    private Bank bank;
    private EmiType type;
    private int amount;
    private int noOfYears;
    private int rateOfInterest;
    private Date createdAt;
}
