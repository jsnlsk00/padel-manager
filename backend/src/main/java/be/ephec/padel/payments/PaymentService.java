package be.ephec.padel.payments;

import be.ephec.padel.common.exceptions.BusinessException;
import be.ephec.padel.matches.MatchBooking;
import be.ephec.padel.matches.MatchParticipation;
import be.ephec.padel.members.Member;
import be.ephec.padel.members.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Paiements. Un match coute 60 EUR divises en 4 parts de 15 EUR, payables a l'avance (R11).
 * Le solde eventuellement du par un organisateur est ajoute a son prochain paiement.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;

    public PaymentService(PaymentRepository paymentRepository, MemberRepository memberRepository) {
        this.paymentRepository = paymentRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * Regle la part du joueur. Si le joueur traine un solde, celui-ci est encaisse
     * dans le meme mouvement : "au moment de son paiement, le solde du sera ajoute".
     */
    @Transactional
    public Payment payShare(MatchBooking match, Member player) {
        MatchParticipation participation = match.participationOf(player.getId())
                .orElseThrow(() -> new BusinessException("Vous ne participez pas a ce match"));

        if (participation.isPaid()) {
            throw new BusinessException("Votre part est deja payee");
        }

        BigDecimal amount = MatchBooking.SHARE;
        boolean withBalance = player.owesBalance();
        if (withBalance) {
            amount = amount.add(player.getBalanceDue());
            player.clearBalance();
            memberRepository.save(player);
        }

        participation.markPaid();
        return paymentRepository.save(
                new Payment(match, player, amount, LocalDateTime.now(), withBalance));
    }

    /** Reglement d'un solde seul, sans match associe. */
    @Transactional
    public Payment paySolde(Member member) {
        if (!member.owesBalance()) {
            throw new BusinessException("Aucun solde a regler");
        }
        BigDecimal amount = member.getBalanceDue();
        member.clearBalance();
        memberRepository.save(member);
        return paymentRepository.save(new Payment(null, member, amount, LocalDateTime.now(), true));
    }

    @Transactional(readOnly = true)
    public List<Payment> historyOf(Long memberId) {
        return paymentRepository.findByPayerIdOrderByPaidAtDesc(memberId);
    }

    @Transactional(readOnly = true)
    public BigDecimal revenue(Long siteId) {
        BigDecimal sum = paymentRepository.sumRevenue(siteId);
        return sum == null ? BigDecimal.ZERO : sum;
    }
}
