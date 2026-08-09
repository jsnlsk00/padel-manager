package be.ephec.padel.payments;

import be.ephec.padel.common.exceptions.BusinessException;
import be.ephec.padel.matches.MatchBooking;
import be.ephec.padel.matches.MatchVisibility;
import be.ephec.padel.members.Member;
import be.ephec.padel.members.MemberRepository;
import be.ephec.padel.members.MemberType;
import be.ephec.padel.sites.Court;
import be.ephec.padel.sites.Site;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentService : R11 parts et soldes")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private PaymentService paymentService;

    private MatchBooking match;
    private Member player;

    @BeforeEach
    void setUp() {
        Site site = new Site("Uccle", "adresse", LocalTime.of(8, 0), LocalTime.of(22, 0));
        ReflectionTestUtils.setField(site, "id", 1L);
        Court court = site.addCourt(1);
        ReflectionTestUtils.setField(court, "id", 101L);

        player = new Member("G1042", "Thomas", "Leroy", "t@padel.be", "hash",
                MemberType.GLOBAL, null);
        ReflectionTestUtils.setField(player, "id", 1L);

        match = new MatchBooking(court, player, LocalDateTime.of(2026, 8, 25, 18, 30),
                MatchVisibility.PUBLIC);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(memberRepository.save(any(Member.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Nested
    @DisplayName("paiement d'une part")
    class PayShare {

        @Test
        @DisplayName("la part vaut 15 EUR et valide la place")
        void shareIsFifteenAndValidatesPlace() {
            match.addPlayer(player, false);

            Payment payment = paymentService.payShare(match, player);

            assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("15.00"));
            assertThat(match.participationOf(1L)).get()
                    .extracting(p -> p.isPaid()).isEqualTo(true);
        }

        @Test
        @DisplayName("le solde du est encaisse avec la part")
        void balanceIsAddedToShare() {
            // Arrange : le joueur traine 30 EUR de solde
            match.addPlayer(player, false);
            player.addBalance(new BigDecimal("30.00"));

            // Act
            Payment payment = paymentService.payShare(match, player);

            // Assert : 15 + 30, et le solde est efface
            assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("45.00"));
            assertThat(payment.isBalancePayment()).isTrue();
            assertThat(player.getBalanceDue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("payer deux fois est refuse")
        void doublePaymentRejected() {
            match.addPlayer(player, true);

            assertThatThrownBy(() -> paymentService.payShare(match, player))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("deja payee");
        }

        @Test
        @DisplayName("payer pour un match auquel on ne participe pas est refuse")
        void nonParticipantRejected() {
            assertThatThrownBy(() -> paymentService.payShare(match, player))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ne participez pas");
        }
    }

    @Nested
    @DisplayName("reglement d'un solde")
    class PayBalance {

        @Test
        @DisplayName("le solde est encaisse et remis a zero")
        void balanceCleared() {
            player.addBalance(new BigDecimal("45.00"));

            Payment payment = paymentService.paySolde(player);

            assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("45.00"));
            assertThat(payment.getMatch()).isNull();
            assertThat(player.owesBalance()).isFalse();
        }

        @Test
        @DisplayName("sans solde, le reglement est refuse")
        void noBalanceRejected() {
            assertThatThrownBy(() -> paymentService.paySolde(player))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Aucun solde");
        }
    }
}
