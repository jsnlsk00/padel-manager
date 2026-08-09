import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';
import { MatchService } from '../../../core/services/match.service';
import { MemberService } from '../../../core/services/member.service';
import { PaymentService } from '../../../core/services/payment.service';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Match, Member } from '../../../core/models/models';
import { MatchDialogComponent } from '../../../shared/components/match-dialog.component';
import { BalanceCardComponent } from '../../../shared/components/balance-card.component';
import { SlotTimePipe } from '../../../core/pipes/slot-time.pipe';

@Component({
  selector: 'app-my-matches',
  standalone: true,
  imports: [CommonModule, MatchDialogComponent, BalanceCardComponent, SlotTimePipe],
  template: `
    <div class="page">
      <header class="head">
        <div>
          <h1>Mes matchs</h1>
          <p class="sub">Ceux que vous organisez et ceux que vous avez rejoints.</p>
        </div>
        <app-balance-card
          class="balance"
          [amount]="me?.balanceDue ?? 0"
          (settle)="payBalance()"
        ></app-balance-card>
      </header>

      <p class="notice alert" *ngIf="error">{{ error }}</p>
      <p class="hint" *ngIf="!loading && !upcoming.length && !past.length">
        Aucun match pour l'instant. Rendez-vous sur le planning pour en creer un.
      </p>

      <section *ngIf="upcoming.length">
        <h2>A venir</h2>
        <div class="list">
          <button type="button" class="item card" *ngFor="let match of upcoming" (click)="selected = match" [attr.data-cy]="'match-' + match.id">
            <span class="when">{{ match.startTime | date: 'EEEE d MMMM' }}</span>
            <span class="time tabular">{{ match.startTime | slotTime }}</span>
            <span class="where">{{ match.siteName }} · terrain {{ match.courtNumber }}</span>
            <span class="players tabular">{{ match.paidParticipantsCount }}/4 payes</span>
            <span class="tag" [ngClass]="tagClass(match)">{{ tag(match) }}</span>
          </button>
        </div>
      </section>

      <section *ngIf="past.length">
        <h2>Historique</h2>
        <div class="list past">
          <button type="button" class="item card" *ngFor="let match of past" (click)="selected = match">
            <span class="when">{{ match.startTime | date: 'EEEE d MMMM' }}</span>
            <span class="time tabular">{{ match.startTime | slotTime }}</span>
            <span class="where">{{ match.siteName }} · terrain {{ match.courtNumber }}</span>
            <span class="players tabular">{{ match.paidParticipantsCount }}/4 payes</span>
            <span class="tag">{{ match.status }}</span>
          </button>
        </div>
      </section>
    </div>

    <app-match-dialog
      *ngIf="selected"
      [match]="selected"
      [balanceDue]="me?.balanceDue ?? 0"
      (close)="selected = null"
      (action)="act($event)"
    ></app-match-dialog>
  `,
  styles: [
    `
      .page { padding: 28px 32px 56px; display: flex; flex-direction: column; gap: 28px; }
      .head { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; flex-wrap: wrap; }
      h1 { font-size: 38px; font-weight: 700; }
      h2 { font-size: 20px; font-weight: 600; color: var(--ink-60); margin-bottom: 12px; }
      .sub { margin: 6px 0 0; color: var(--ink-60); font-size: 13.5px; }
      .balance { max-width: 320px; width: 100%; }
      .list { display: flex; flex-direction: column; gap: 8px; }
      .item {
        display: grid;
        grid-template-columns: 1.4fr 1fr 1.4fr 0.8fr 0.9fr;
        gap: 16px;
        align-items: center;
        text-align: left;
        padding: 15px 18px;
        cursor: pointer;
        font: inherit;
        color: inherit;
      }
      .item:hover { border-color: var(--ink); }
      .past .item { opacity: 0.6; }
      .when { font-family: var(--font-display); font-size: 18px; font-weight: 600; text-transform: uppercase; }
      .time, .where, .players { font-size: 13.5px; color: var(--ink-60); }
      .tag { font-size: 11.5px; letter-spacing: 0.12em; text-transform: uppercase; text-align: right; color: var(--ink-45); }
      .tag.paid { color: var(--green); }
      .tag.due { color: var(--red); }
      .hint { color: var(--ink-45); }
      .notice.alert { padding: 13px 15px; background: var(--red-soft); border: 1px solid var(--red); color: var(--red); font-size: 13.5px; }
      @media (max-width: 900px) {
        .page { padding: 20px 16px 40px; }
        .item { grid-template-columns: 1fr 1fr; }
      }
    `,
  ],
})
export class MyMatchesComponent {
  private matchService = inject(MatchService);
  private memberService = inject(MemberService);
  private paymentService = inject(PaymentService);
  private errors = inject(ErrorMessageService);
  private auth = inject(AuthService);

  upcoming: Match[] = [];
  past: Match[] = [];
  me: Member | null = null;
  selected: Match | null = null;
  error = '';
  loading = true;

  constructor() {
    this.load();
  }

  tag(match: Match): string {
    const matricule = this.auth.currentUser?.matricule;
    const mine = match.participants.find((player) => player.matricule === matricule);
    if (mine && !mine.paid) {
      return 'A payer';
    }
    if (match.organizerId && match.freeSlots > 0 && match.visibility === 'PRIVATE') {
      return 'Incomplet';
    }
    return 'Confirme';
  }

  tagClass(match: Match): Record<string, boolean> {
    const label = this.tag(match);
    return { due: label === 'A payer' || label === 'Incomplet', paid: label === 'Confirme' };
  }

  act(match: Match): void {
    const matricule = this.auth.currentUser?.matricule;
    const mine = match.participants.find((player) => player.matricule === matricule);
    // Les deux branches renvoient des types differents (PaymentResult / Match) ; seul
    // l'achevement de la requete nous interesse ici, d'ou le typage en Observable<unknown>.
    const request: Observable<unknown> = mine
      ? this.paymentService.payShare(match.id)
      : this.matchService.joinMatch(match.id);
    request.subscribe({
      next: () => {
        this.selected = null;
        this.load();
      },
      error: (error: unknown) => {
        this.error = this.errors.toMessage(error);
        this.selected = null;
      },
    });
  }

  payBalance(): void {
    this.paymentService.payBalance().subscribe({
      next: () => this.load(),
      error: (error) => (this.error = this.errors.toMessage(error)),
    });
  }

  private load(): void {
    this.memberService.me().subscribe((me) => (this.me = me));
    this.matchService.getMyMatches().subscribe({
      next: (matches) => {
        const now = new Date();
        this.upcoming = matches.filter((match) => new Date(match.startTime) >= now);
        this.past = matches.filter((match) => new Date(match.startTime) < now).reverse();
        this.loading = false;
      },
      error: (error) => {
        this.error = this.errors.toMessage(error);
        this.loading = false;
      },
    });
  }
}
