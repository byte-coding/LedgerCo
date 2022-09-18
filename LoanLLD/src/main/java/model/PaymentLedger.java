package model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;
import java.util.List;

@Getter
@AllArgsConstructor
public class PaymentLedger {
    private Emi emi;
    private List<PaymentDetail> paymentDetails;

    @Getter
    @AllArgsConstructor
    public static class PaymentDetail {
        private int amountPaid;
        private Date paidOn;
        private int emiNumber;
        private PaymentStatus status;
    }
}
