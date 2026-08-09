import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CreateMatchRequest, MatchVisibility } from '../../core/models/models';
import { SlotTimePipe } from '../../core/pipes/slot-time.pipe';

/**
 * Creation d'un match sur un creneau libre. Sur un match prive l'organisateur
 * saisit jusqu'a 3 matricules ; sur un match public le formulaire l'en empeche (R10).
 */
@Component({
  selector: 'app-create-match-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SlotTimePipe],
  template: `
    <div class="backdrop" (click)="close.emit()">
      <div class="dialog" (click)="$event.stopPropagation()" data-cy="create-dialog">
        <header>
          <span class="title">Creer un match</span>
          <span class="kicker">Terrain {{ courtNumber }}</span>
        </header>

        <form class="body" [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <div class="meta tabular">
            <span>{{ siteName }} · terrain {{ courtNumber }}</span>
            <span>{{ startTime | date: 'EEEE d MMMM' }} · {{ startTime | slotTime }}</span>
          </div>

          <div class="visibility">
            <span class="field-label">Type de match</span>
            <div class="choices">
              <button
                type="button"
                *ngFor="let option of options"
                class="choice"
                [class.selected]="form.controls.visibility.value === option.value"
                (click)="setVisibility(option.value)"
              >
                <span class="choice-name">{{ option.label }}</span>
                <span class="choice-note">{{ option.note }}</span>
              </button>
            </div>
          </div>

          <div class="guests" *ngIf="isPrivate">
            <span class="field-label">Joueurs invites (matricules)</span>
            <div class="guest" *ngFor="let control of guests.controls; let i = index">
              <input
                class="input"
                [formControl]="asControl(control)"
                [attr.data-cy]="'guest-' + i"
                placeholder="G1044"
                (input)="upper(i)"
              />
              <button type="button" class="btn btn-ghost small" (click)="removeGuest(i)">Retirer</button>
            </div>
            <button type="button" class="btn btn-ghost" *ngIf="guests.length < 3" (click)="addGuest()">
              Ajouter un joueur
            </button>
            <p class="hint">
              Un match prive exige 4 joueurs. S'il n'est pas complet la veille, il devient public
              et une penalite d'une semaine s'applique a l'organisateur.
            </p>
          </div>

          <p class="hint" *ngIf="!isPrivate">
            Sur un match public, les 3 autres places sont ouvertes a tous les membres et validees
            au paiement. Vous ne pouvez pas reserver pour quelqu'un d'autre.
          </p>

          <p class="notice alert" *ngIf="blockedReason">{{ blockedReason }}</p>
          <p class="notice alert" *ngIf="serverError" data-cy="create-error">{{ serverError }}</p>

          <div class="actions">
            <button class="btn" type="submit" data-cy="create-submit" [disabled]="!!blockedReason || busy">
              Confirmer et payer 15 €
            </button>
            <button class="btn btn-ghost" type="button" (click)="close.emit()">Annuler</button>
          </div>
        </form>
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
      .dialog { width: min(560px, 100%); background: var(--bg); border: 1px solid var(--line); max-height: 88vh; overflow: auto; }
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
      .choices { display: flex; gap: 8px; }
      .choice {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 3px;
        align-items: flex-start;
        text-align: left;
        padding: 13px 15px;
        background: var(--surface);
        border: 1px solid var(--line);
        border-radius: 2px;
        cursor: pointer;
      }
      .choice.selected { background: var(--lime); border-color: var(--ink); }
      .choice-name { font-family: var(--font-display); font-size: 18px; font-weight: 600; text-transform: uppercase; }
      .choice-note { font-size: 12px; color: var(--ink-60); }
      .guests { display: flex; flex-direction: column; gap: 8px; }
      .guest { display: flex; gap: 8px; }
      .guest .input { text-transform: uppercase; }
      .small { padding: 8px 14px; font-size: 13px; }
      .hint { margin: 0; font-size: 12.5px; line-height: 1.5; color: var(--ink-45); }
      .notice { margin: 0; padding: 13px 15px; font-size: 13px; line-height: 1.55; }
      .notice.alert { background: var(--red-soft); border: 1px solid var(--red); color: var(--red); }
      .actions { display: flex; gap: 10px; }
      .actions .btn:first-child { flex: 1; }
    `,
  ],
})
export class CreateMatchDialogComponent {
  @Input({ required: true }) courtId!: number;
  @Input({ required: true }) courtNumber!: number;
  @Input({ required: true }) startTime!: string;
  @Input({ required: true }) siteName!: string;
  @Input() blockedReason = '';
  @Input() serverError = '';
  @Input() busy = false;
  @Output() close = new EventEmitter<void>();
  @Output() create = new EventEmitter<CreateMatchRequest>();

  private fb = inject(FormBuilder);

  readonly options: { value: MatchVisibility; label: string; note: string }[] = [
    { value: 'PRIVATE', label: 'Prive', note: 'Vous ajoutez les 3 joueurs' },
    { value: 'PUBLIC', label: 'Public', note: 'Places ouvertes a tous' },
  ];

  readonly form = this.fb.nonNullable.group({
    visibility: ['PRIVATE' as MatchVisibility, Validators.required],
    guests: this.fb.array<string>([]),
  });

  get guests(): FormArray {
    return this.form.controls.guests as unknown as FormArray;
  }

  get isPrivate(): boolean {
    return this.form.controls.visibility.value === 'PRIVATE';
  }

  asControl(control: unknown) {
    return control as ReturnType<FormBuilder['control']>;
  }

  setVisibility(visibility: MatchVisibility): void {
    this.form.controls.visibility.setValue(visibility);
    if (visibility === 'PUBLIC') {
      this.guests.clear();
    }
  }

  addGuest(): void {
    if (this.guests.length < 3) {
      this.guests.push(this.fb.nonNullable.control('', Validators.pattern(/^[GSLgsl][0-9]{4,5}$/)));
    }
  }

  removeGuest(index: number): void {
    this.guests.removeAt(index);
  }

  upper(index: number): void {
    const control = this.guests.at(index);
    control.setValue(String(control.value ?? '').toUpperCase(), { emitEvent: false });
  }

  submit(): void {
    if (this.form.invalid || this.blockedReason) {
      return;
    }
    const matricules = this.guests.controls
      .map((control) => String(control.value ?? '').trim())
      .filter((value) => value.length > 0);

    this.create.emit({
      courtId: this.courtId,
      startTime: this.startTime,
      visibility: this.form.controls.visibility.value,
      privatePlayersMatricules: this.isPrivate ? matricules : undefined,
    });
  }
}
