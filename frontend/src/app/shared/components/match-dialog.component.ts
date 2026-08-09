import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Match } from '../../core/models/models';
import { AuthService } from '../../core/auth/auth.service';
import { SlotTimePipe } from '../../core/pipes/slot-time.pipe';

/**
 * Detail d'un match : les 4 places, qui a paye, et l'action possible pour
 * le membre courant. Aucune regle metier ici : le composant affiche ce que
 * le backend a calcule et relaie l'intention de l'utilisateur.
 */
@Component({
  selector: 'app-match-dialog',
  standalone: true,
  imports: [CommonModule, SlotTimePipe],
  template: `
    <div class="backdrop" (click)="close.emit()">
      <div class="dialog" (click)="$event.stopPropagation()" data-cy="match-dialog">
        <header>
          <span class="title">{{ title }}</span>
          <span class="kicker">{{ organizerLabel }}</span>
        </header>

        <div class="body">
          <div class="meta tabular">
            <span>{{ match.siteName }} · terrain {{ match.courtNumber }}</span>
            <span>{{ match.startTime | date: 'EEEE d MMMM' }} · {{ match.startTime | slotTime }}</span>
          </div>

          <div class="places">
            <span class="places-label">Joueurs et paiements — 60 € divises en 4</span>
            <div class="place" *ngFor="let player of match.participants" [class.unpaid]="!player.paid">
              <span class="identity">
                <span class="name">{{ player.fullName }}{{ isMe(player.playerId) ? ' (vous)' : '' }}</span>
                <span class="matricule tabular">{{ player.matricule }}</span>
              </span>
              <span class="state">
                <span class="badge" [class.paid]="player.paid">{{ player.paid ? 'Payé' : 'En attente' }}</span>
                <span class="amount tabular">15 €</span>
              </span>
            </div>
            <div class="place free" *ngFor="let index of freeSlotIndexes">
              <span class="identity">
                <span class="name">Place libre</span>
                <span class="matricule">{{ freeSlotHint }}</span>
              </span>
              <span class="amount tabular">15 €</span>
            </div>
          </div>

          <p class="notice" [class.alert]="alert">{{ notice }}</p>

          <div class="actions">
            <button
              class="btn"
              type="button"
              data-cy="match-action"
              [disabled]="!actionEnabled || busy"
              (click)="action.emit(match)"
            >
              {{ actionLabel }}
            </button>
            <button class="btn btn-ghost" type="button" (click)="close.emit()">Fermer</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .backdrop {
        position: fixed;
        inset: 0;
        background: rgba(18, 33, 26, 0.45);
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 24px;
        z-index: 40;
      }
      .dialog {
        width: min(560px, 100%);
        background: var(--bg);
        border: 1px solid var(--line);
        max-height: 88vh;
        overflow: auto;
      }
      header {
        display: flex;
        align-items: baseline;
        justify-content: space-between;
        gap: 16px;
        padding: 18px 24px;
        background: var(--ink);
        color: var(--bg);
      }
      .title { font-family: var(--font-display); font-size: 24px; font-weight: 600; text-transform: uppercase; }
      .kicker { font-size: 12.5px; letter-spacing: 0.14em; text-transform: uppercase; color: var(--lime); }
      .body { padding: 24px; display: flex; flex-direction: column; gap: 20px; }
      .meta { display: flex; justify-content: space-between; gap: 16px; font-size: 13.5px; color: var(--ink-60); }
      .places { display: flex; flex-direction: column; gap: 8px; }
      .places-label {
        font-family: var(--font-display);
        font-size: 13px;
        letter-spacing: 0.16em;
        text-transform: uppercase;
        color: var(--ink-45);
      }
      .place {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 14px;
        padding: 13px 15px;
        background: var(--surface);
        border: 1px solid var(--line);
      }
      .place.unpaid { border-color: var(--red); }
      .place.free { background: #fafbf8; opacity: 0.75; }
      .identity { display: flex; flex-direction: column; gap: 2px; }
      .name { font-family: var(--font-display); font-size: 17px; font-weight: 600; text-transform: uppercase; }
      .matricule { font-size: 12px; color: var(--ink-45); }
      .state { display: flex; align-items: center; gap: 12px; }
      .badge { font-size: 12px; letter-spacing: 0.12em; text-transform: uppercase; color: var(--red); }
      .badge.paid { color: var(--green); }
      .amount { font-family: var(--font-display); font-size: 18px; font-weight: 600; }
      .notice {
        margin: 0;
        padding: 13px 15px;
        font-size: 13px;
        line-height: 1.55;
        background: var(--surface);
        border: 1px solid var(--line);
        color: var(--ink-60);
      }
      .notice.alert { background: var(--red-soft); border-color: var(--red); color: var(--red); }
      .actions { display: flex; gap: 10px; }
      .actions .btn { flex: 1; }
      .actions .btn-ghost { flex: 0 0 auto; }
    `,
  ],
})
export class MatchDialogComponent {
  @Input({ required: true }) match!: Match;
  @Input() balanceDue = 0;
  @Input() busy = false;
  @Output() close = new EventEmitter<void>();
  @Output() action = new EventEmitter<Match>();

  private auth = inject(AuthService);

  get myParticipation() {
    const matricule = this.auth.currentUser?.matricule;
    return this.match.participants.find((player) => player.matricule === matricule) ?? null;
  }

  isMe(playerId: number): boolean {
    return this.myParticipation?.playerId === playerId;
  }

  get freeSlotIndexes(): number[] {
    return Array.from({ length: this.match.freeSlots }, (_, index) => index);
  }

  get freeSlotHint(): string {
    return this.match.visibility === 'PRIVATE'
      ? "ajoutee par l'organisateur"
      : 'premier paye, premier servi';
  }

  get title(): string {
    if (this.match.participants.length === 4 && this.match.paidParticipantsCount === 4) {
      return 'Match complet';
    }
    if (this.match.participants.length === 4) {
      return 'Place a liberer';
    }
    return this.match.visibility === 'PUBLIC' ? 'Match public' : 'Match prive';
  }

  get organizerLabel(): string {
    return this.match.organizerId === this.myParticipation?.playerId
      ? 'Vous organisez'
      : `Organise par ${this.match.organizerName}`;
  }

  get canPayMyShare(): boolean {
    return this.myParticipation !== null && !this.myParticipation.paid;
  }

  get canJoin(): boolean {
    return (
      this.myParticipation === null &&
      this.match.visibility === 'PUBLIC' &&
      this.match.freeSlots > 0 &&
      this.balanceDue === 0
    );
  }

  get actionEnabled(): boolean {
    return this.canPayMyShare || this.canJoin;
  }

  get actionLabel(): string {
    if (this.canPayMyShare) {
      return this.balanceDue > 0
        ? `Payer 15 € + solde ${this.balanceDue} €`
        : 'Payer ma part — 15 €';
    }
    if (this.canJoin) {
      return 'Rejoindre et payer 15 €';
    }
    return 'Aucune action possible';
  }

  get alert(): boolean {
    return this.match.participants.length === 4 && this.match.paidParticipantsCount < 4;
  }

  get notice(): string {
    if (this.balanceDue > 0 && this.myParticipation === null) {
      return `Vous ne pouvez pas rejoindre : un solde de ${this.balanceDue} € reste du.`;
    }
    if (this.match.participants.length === 4 && this.match.paidParticipantsCount < 4) {
      return "Un joueur n'a pas encore paye : sa place sera liberee la veille du match et le match passera public.";
    }
    if (this.match.visibility === 'PRIVATE' && this.match.freeSlots > 0) {
      return "Match prive incomplet : il basculera automatiquement en public la veille, et l'organisateur ecopera d'une semaine de delai de reservation.";
    }
    if (this.match.visibility === 'PUBLIC') {
      return 'Match public : la place est validee des le paiement. Premier paye, premier servi.';
    }
    return 'Match complet et integralement paye.';
  }
}
