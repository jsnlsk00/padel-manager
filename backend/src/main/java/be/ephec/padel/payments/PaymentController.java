package be.ephec.padel.payments;

import be.ephec.padel.auth.CurrentMemberService;
import be.ephec.padel.matches.MatchService;
import be.ephec.padel.members.Member;
import be.ephec.padel.payments.dto.PaymentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Paiements")
public class PaymentController {

    private final PaymentService paymentService;
    private final MatchService matchService;
    private final CurrentMemberService currentMember;

    public PaymentController(PaymentService paymentService,
                            MatchService matchService,
                            CurrentMemberService currentMember) {
        this.paymentService = paymentService;
        this.matchService = matchService;
        this.currentMember = currentMember;
    }

    @PostMapping("/match/{matchId}")
    @Operation(summary = "Paie sa part de 15 EUR, solde du inclus le cas echeant")
    public ResponseEntity<PaymentDto> payShare(@PathVariable Long matchId) {
        Member member = currentMember.requireMember();
        Payment payment = paymentService.payShare(matchService.findById(matchId), member);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PaymentDto.from(payment, member.getBalanceDue()));
    }

    @PostMapping("/balance")
    @Operation(summary = "Regle le solde du, hors match")
    public ResponseEntity<PaymentDto> payBalance() {
        Member member = currentMember.requireMember();
        Payment payment = paymentService.paySolde(member);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PaymentDto.from(payment, member.getBalanceDue()));
    }

    @GetMapping("/me")
    @Operation(summary = "Historique de mes paiements")
    public List<PaymentDto> myPayments() {
        Member member = currentMember.requireMember();
        return paymentService.historyOf(member.getId()).stream()
                .map(p -> PaymentDto.from(p, member.getBalanceDue()))
                .toList();
    }
}
