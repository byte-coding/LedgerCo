package service;


import model.*;
import util.LockUtil;

import java.util.*;

import static java.util.Optional.ofNullable;

public class PaymentServiceImpl implements PaymentService {

    private Map<Emi, EmiPayment> userEmiPayment = new HashMap<>();
    private Map<Emi, PaymentLedger> userEmiPaymentLedger = new HashMap<>();
    private LockUtil lockUtil;

    public PaymentServiceImpl(LockUtil lockUtil) {
        this.lockUtil = lockUtil;
    }

    public void createEmiPayment(Emi emi) {
        int interest = (int) Math.ceil(emi.getAmount() * emi.getNoOfYears() * emi.getRateOfInterest() / 100.0);
        int totalAmount = emi.getAmount() + interest;

        EmiPayment emiPayment = null;
        switch (emi.getType()) {
            case MONTHLY:
                int totalEmiMonths = emi.getNoOfYears() * 12;
                int monthlyRecurringPayment = (int) Math.ceil(totalAmount / (double) totalEmiMonths);
                emiPayment = new EmiPayment(emi, monthlyRecurringPayment, totalEmiMonths);
        }
        if (emiPayment == null) {
            throw new RuntimeException();
        }
        userEmiPayment.put(emi, emiPayment);
    }

    @Override
    public void pay(Emi emi, int lumpSumAmount, int emiNo) {
        if (!userEmiPayment.containsKey(emi)) {
            throw new RuntimeException("User Emi Payment Details not Found");
        }
        if (lumpSumAmount < 0) {
            throw new RuntimeException("lumpsump amount can not be Negative");
        }
        EmiPayment emiPayment = userEmiPayment.get(emi);
        try {
            lockUtil.lock(emi);
            PaymentLedger.PaymentDetail paymentDetail = new PaymentLedger.PaymentDetail(lumpSumAmount, new Date(), emiNo, PaymentStatus.SUCCESS);
            PaymentLedger paymentLedger = userEmiPaymentLedger.get(emi);
            if (paymentLedger == null) {
                paymentLedger = new PaymentLedger(emi, Collections.singletonList(paymentDetail));
            } else {
                paymentLedger.getPaymentDetails().add(paymentDetail);
            }
            userEmiPaymentLedger.put(emi, paymentLedger);
        } finally {
            lockUtil.unlock(emi);
        }
    }

    @Override
    public Balance balance(Emi emi, int emiNo) {
        EmiPayment emiPayment = userEmiPayment.get(emi);
        checkIfEmiNumberIsValid(emiPayment, emiNo);

        try {
            lockUtil.lock(emi);

            int totalLumpSumPaid = 0;
            for (PaymentLedger.PaymentDetail paymentDetail : ofNullable(userEmiPaymentLedger.get(emi)).map(PaymentLedger::getPaymentDetails).orElse(new ArrayList<>())) {
                if (paymentDetail.getEmiNumber() <= emiNo && PaymentStatus.SUCCESS.equals(paymentDetail.getStatus())) {
                    totalLumpSumPaid += paymentDetail.getAmountPaid();
                }
            }

            int interestAmount = (int) Math.ceil(emi.getAmount() * emi.getNoOfYears() * emi.getRateOfInterest() / 100.0);
            int emiPaidTillYet = emiPayment.getRecurringPayment() * emiNo;
            int amountPaid = totalLumpSumPaid + emiPaidTillYet;
            int amountToBePaid = emi.getAmount() + interestAmount - amountPaid;

            int remainingEmiCycles = (int) Math.ceil(amountToBePaid / (double) emiPayment.getRecurringPayment());
            return new Balance(emi.getBank().getName(), emi.getUser().getName(), amountPaid, remainingEmiCycles);
        } finally {
            lockUtil.unlock(emi);
        }
    }

    private static void checkIfEmiNumberIsValid(EmiPayment emiPayment, int emiNo) {
        if (emiNo > emiPayment.getTotalEmiCycle()) {
            System.out.println(emiPayment.getEmi());
            throw new RuntimeException("Input Emi cycle is more than actual emi cycle");
        }
    }
}
