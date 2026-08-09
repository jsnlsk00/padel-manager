package be.ephec.padel.payments.dto;

import be.ephec.padel.payments.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentDto(
        Long id,
        Long matchId,
        String payerMatricule,
        BigDecimal amount,
        LocalDateTime paidAt,
        boolean balancePayment,
        BigDecimal remainingBalance
) {

    public static PaymentDto from(Payment payment, BigDecimal remainingBalance) {
        return new PaymentDto(
                payment.getId(),
                payment.getMatch() == null ? null : payment.getMatch().getId(),
                payment.getPayer().getMatricule(),
                payment.getAmount(),
                payment.getPaidAt(),
                payment.isBalancePayment(),
                remainingBalance);
    }
}
