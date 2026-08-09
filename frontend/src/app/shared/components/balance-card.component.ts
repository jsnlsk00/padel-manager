import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

/** Solde du : tant qu'il est non nul, aucune nouvelle reservation n'est possible. */
@Component({
  selector: 'app-balance-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card balance" [class.due]="amount > 0" data-cy="balance-card">
      <div class="head">
        <span class="label">Solde du</span>
        <span class="amount tabular" data-cy="balance-amount">{{ amount | number: '1.0-2' }} €</span>
      </div>
      <p>{{ message }}</p>
      <button *ngIf="amount > 0" class="btn btn-dark" type="button" (click)="settle.emit()" data-cy="pay-balance">
        Regler le solde
      </button>
    </div>
  `,
  styles: [
    `
      .balance { padding: 16px; }
      .balance.due { background: var(--red-soft); border-color: var(--red); color: var(--red); }
      .head { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
      .label {
        font-family: var(--font-display);
        font-size: 14px;
        letter-spacing: 0.16em;
        text-transform: uppercase;
      }
      .amount { font-family: var(--font-display); font-size: 32px; font-weight: 700; }
      p { margin: 10px 0 0; font-size: 13px; line-height: 1.55; }
      .btn { margin-top: 12px; width: 100%; }
    `,
  ],
})
export class BalanceCardComponent {
  @Input() amount = 0;
  @Output() settle = new EventEmitter<void>();

  get message(): string {
    return this.amount > 0
      ? "Un match que vous avez organise n'a pas ete complete : le solde vous est impute. Aucune nouvelle reservation tant qu'il est du."
      : 'Aucun solde. Vous pouvez creer un match dans votre fenetre de reservation.';
  }
}
