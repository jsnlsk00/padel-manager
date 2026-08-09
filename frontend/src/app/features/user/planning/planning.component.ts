import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin, Observable } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { MatchService } from '../../../core/services/match.service';
import { MemberService } from '../../../core/services/member.service';
import { PaymentService } from '../../../core/services/payment.service';
import { SiteService } from '../../../core/services/site.service';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { CreateMatchRequest, Match, Member, Planning, Site, Slot } from '../../../core/models/models';
import { MatchDialogComponent } from '../../../shared/components/match-dialog.component';
import { CreateMatchDialogComponent } from '../../../shared/components/create-match-dialog.component';
import { BalanceCardComponent } from '../../../shared/components/balance-card.component';
import { MemberCategoryPipe } from '../../../core/pipes/member-category.pipe';
import { SlotTimePipe } from '../../../core/pipes/slot-time.pipe';

type SlotRow = { time: string; cells: Slot[] };

/** Planning de reservation : grille terrains x creneaux d'une journee. */
@Component({
  selector: 'app-planning',
  standalone: true,
  imports: [
    CommonModule,
    MatchDialogComponent,
    CreateMatchDialogComponent,
    BalanceCardComponent,
    MemberCategoryPipe,
    SlotTimePipe,
  ],
  template: `
    <div class="page">
      <section class="grid-col">
        <header class="head">
          <div>
            <h1>{{ planning?.siteName ?? 'Planning' }}</h1>
            <p class="sub" *ngIf="site">
              {{ site.courts.length }} terrains · {{ site.openingTime }} – {{ site.closingTime }} ·
              matchs de 1h30, 15 min de battement
            </p>
          </div>
          <div class="controls">
            <select
              class="input select"
              data-cy="site-select"
              [value]="siteId"
              (change)="changeSite($event)"
            >
              <option *ngFor="let option of sites" [value]="option.id" [disabled]="!allowed(option)">
                {{ option.name }}{{ allowed(option) ? '' : ' (hors rattachement)' }}
              </option>
            </select>
            <label class="filter">
              <input type="checkbox" [checked]="onlyJoinable" (change)="toggleJoinable()" />
              Uniquement les matchs a rejoindre
            </label>
          </div>
        </header>

        <div class="days">
          <button
            type="button"
            *ngFor="let day of days"
            class="day"
            [class.selected]="day.iso === selectedDay"
            [attr.data-cy]="'day-' + day.iso"
            (click)="selectDay(day.iso)"
          >
            <span class="dow">{{ day.label }}</span>
            <span class="num tabular">{{ day.num }}</span>
            <span class="mon">{{ day.month }}</span>
          </button>
        </div>

        <p class="loading" *ngIf="loading">Chargement du planning…</p>
        <p class="notice alert" *ngIf="error" data-cy="planning-error">{{ error }}</p>

        <div class="closed card" *ngIf="planning?.closed" data-cy="closed-day">
          <span class="closed-title">Site ferme</span>
          <span class="closed-reason">{{ planning?.closureReason }}</span>
        </div>

        <div class="grid card" *ngIf="planning && !planning.closed" data-cy="planning-grid">
          <div class="row header">
            <div class="hour">Heure</div>
            <div class="cell head-cell" *ngFor="let court of planning.courts">
              Terrain {{ court.number }}
            </div>
          </div>
          <div class="row" *ngFor="let row of rows">
            <div class="hour tabular">{{ row.time }}</div>
            <button
              type="button"
              class="cell"
              *ngFor="let cell of row.cells"
              [ngClass]="cellClass(cell)"
              [attr.data-cy]="'slot-' + cell.courtNumber + '-' + row.time"
              (click)="openSlot(cell)"
            >
              <span class="cell-title">{{ cellTitle(cell) }}</span>
              <span class="cell-sub tabular">{{ cellSubtitle(cell) }}</span>
            </button>
          </div>
        </div>

        <div class="legend" *ngIf="planning && !planning.closed">
          <span><i class="sw free"></i>Libre</span>
          <span><i class="sw public"></i>Public a rejoindre</span>
          <span><i class="sw full"></i>Complet</span>
          <span><i class="sw unpaid"></i>Impaye, place liberee</span>
          <span><i class="sw closed"></i>Fermeture</span>
          <span><i class="sw mine"></i>Vos matchs</span>
        </div>
      </section>

      <aside class="side">
        <div class="card status" *ngIf="me">
          <div class="status-head">
            <span>Votre statut</span>
            <span class="matricule tabular">{{ me.matricule }}</span>
          </div>
          <div class="status-body">
            <div><span>Categorie</span><span>{{ me.type | memberCategory }}</span></div>
            <div><span>Fenetre</span><span class="tabular">{{ me.bookingWindowDays }} jours</span></div>
            <div><span>Reservable jusqu'au</span><span class="tabular">{{ limitDay }}</span></div>
            <div *ngIf="me.bannedUntil" class="penalty">
              <span>Penalite</span><span class="tabular">jusqu'au {{ me.bannedUntil }}</span>
            </div>
          </div>
        </div>

        <app-balance-card [amount]="me?.balanceDue ?? 0" (settle)="payBalance()"></app-balance-card>

        <div class="mine">
          <span class="mine-label">Vos prochains matchs</span>
          <button
            type="button"
            class="mine-item card"
            *ngFor="let match of myMatches"
            [class.unpaid]="!isPaidByMe(match)"
            (click)="openMatch(match)"
          >
            <span class="mine-when">
              {{ match.startTime | date: 'EEE d MMM' }} · {{ match.startTime | slotTime: false }}
            </span>
            <span class="mine-where">{{ match.siteName }} · terrain {{ match.courtNumber }}</span>
          </button>
          <p class="hint" *ngIf="!myMatches.length">Aucun match a venir.</p>
        </div>
      </aside>
    </div>

    <app-match-dialog
      *ngIf="selectedMatch"
      [match]="selectedMatch"
      [balanceDue]="me?.balanceDue ?? 0"
      [busy]="busy"
      (close)="selectedMatch = null"
      (action)="actOn($event)"
    ></app-match-dialog>

    <app-create-match-dialog
      *ngIf="creationSlot && planning"
      [courtId]="creationSlot.courtId"
      [courtNumber]="creationSlot.courtNumber"
      [startTime]="creationSlot.startTime"
      [siteName]="planning.siteName"
      [blockedReason]="creationBlockedReason"
      [serverError]="creationError"
      [busy]="busy"
      (close)="creationSlot = null"
      (create)="create($event)"
    ></app-create-match-dialog>
  `,
  styles: [
    `
      .page { display: grid; grid-template-columns: 1fr 320px; align-items: start; }
      .grid-col { padding: 28px 32px 48px; min-width: 0; }
      .head { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; flex-wrap: wrap; margin-bottom: 22px; }
      h1 { font-size: 38px; font-weight: 700; }
      .sub { margin: 6px 0 0; font-size: 13.5px; color: var(--ink-60); }
      .controls { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
      .select { width: auto; padding: 9px 12px; font-size: 13.5px; }
      .filter { display: flex; align-items: center; gap: 8px; padding: 9px 12px; background: var(--surface); border: 1px solid rgba(18,33,26,.18); font-size: 13.5px; cursor: pointer; }
      .filter input { accent-color: var(--ink); width: 15px; height: 15px; }
      .days { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 22px; }
      .day { padding: 10px 16px; background: var(--surface); border: 1px solid var(--line); border-radius: 2px; cursor: pointer; text-align: center; }
      .day.selected { background: var(--lime); border-color: var(--ink); }
      .dow { display: block; font-size: 11px; letter-spacing: 0.14em; text-transform: uppercase; opacity: 0.62; }
      .num { display: block; font-family: var(--font-display); font-size: 22px; font-weight: 600; line-height: 1.15; }
      .mon { display: block; font-size: 11px; opacity: 0.62; }
      .grid { overflow: hidden; }
      .row { display: flex; border-bottom: 1px solid rgba(18,33,26,.08); }
      .row.header { background: #eef1ea; border-bottom: 1px solid var(--line); }
      .hour { width: 88px; flex: none; padding: 14px 12px; font-family: var(--font-display); font-size: 17px; font-weight: 600; font-variant-numeric: tabular-nums; color: var(--ink-60); }
      .row.header .hour { font-size: 12px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--ink-45); }
      .cell {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 3px;
        align-items: flex-start;
        text-align: left;
        padding: 13px 12px;
        border: none;
        border-left: 1px solid rgba(18,33,26,.1);
        background: var(--surface);
        color: var(--ink);
        cursor: pointer;
        font: inherit;
      }
      .head-cell { font-family: var(--font-display); font-size: 14px; font-weight: 600; letter-spacing: 0.08em; text-transform: uppercase; cursor: default; background: none; }
      .cell-title { font-family: var(--font-display); font-size: 15px; font-weight: 600; letter-spacing: 0.04em; text-transform: uppercase; }
      .cell-sub { font-size: 12px; opacity: 0.72; }
      .cell.free { color: var(--ink-45); }
      .cell.public { background: var(--lime); }
      .cell.full { background: var(--ink); color: var(--bg); }
      .cell.unpaid { background: var(--red-soft); color: var(--red); }
      .cell.hidden { background: #fafbf8; color: rgba(18,33,26,.25); cursor: default; }
      .cell.mine { box-shadow: inset 0 0 0 2px var(--ink); }
      .legend { display: flex; gap: 20px; flex-wrap: wrap; margin-top: 16px; font-size: 12.5px; color: var(--ink-60); }
      .legend span { display: flex; align-items: center; gap: 7px; }
      .sw { width: 12px; height: 12px; display: block; border: 1px solid rgba(18,33,26,.25); }
      .sw.free { background: var(--surface); }
      .sw.public { background: var(--lime); }
      .sw.full { background: var(--ink); }
      .sw.unpaid { background: var(--red-soft); }
      .sw.closed { background: #f0f2ee; }
      .sw.mine { background: var(--surface); box-shadow: inset 0 0 0 2px var(--ink); }
      .closed { padding: 40px; display: flex; flex-direction: column; gap: 8px; align-items: center; background-image: repeating-linear-gradient(45deg, rgba(18,33,26,.05) 0 6px, transparent 6px 12px); }
      .closed-title { font-family: var(--font-display); font-size: 26px; font-weight: 700; text-transform: uppercase; }
      .closed-reason { color: var(--ink-60); }
      .side { padding: 28px 32px 48px; border-left: 1px solid var(--line); display: flex; flex-direction: column; gap: 22px; }
      .status-head { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; background: var(--ink); color: var(--bg); font-family: var(--font-display); font-size: 14px; letter-spacing: 0.16em; text-transform: uppercase; }
      .status-head .matricule { color: var(--lime); }
      .status-body { padding: 16px; display: flex; flex-direction: column; gap: 11px; font-size: 13.5px; }
      .status-body > div { display: flex; justify-content: space-between; gap: 12px; }
      .status-body > div > span:first-child { color: var(--ink-60); }
      .penalty { color: var(--red); }
      .mine-label { display: block; font-family: var(--font-display); font-size: 14px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--ink-45); margin-bottom: 10px; }
      .mine { display: flex; flex-direction: column; }
      .mine-item { display: flex; flex-direction: column; gap: 4px; text-align: left; padding: 13px 14px; cursor: pointer; font: inherit; color: inherit; margin-bottom: 8px; }
      .mine-item.unpaid { border-color: var(--red); }
      .mine-when { font-family: var(--font-display); font-size: 17px; font-weight: 600; text-transform: uppercase; }
      .mine-where { font-size: 12.5px; color: var(--ink-60); }
      .hint { font-size: 13px; color: var(--ink-45); }
      .loading { color: var(--ink-45); }
      .notice.alert { padding: 13px 15px; background: var(--red-soft); border: 1px solid var(--red); color: var(--red); font-size: 13.5px; }
      @media (max-width: 1100px) {
        .page { grid-template-columns: 1fr; }
        .side { border-left: none; border-top: 1px solid var(--line); }
        .grid-col { padding: 20px 16px 32px; }
      }
    `,
  ],
})
export class PlanningComponent {
  private siteService = inject(SiteService);
  private matchService = inject(MatchService);
  private memberService = inject(MemberService);
  private paymentService = inject(PaymentService);
  private errors = inject(ErrorMessageService);
  private auth = inject(AuthService);

  sites: Site[] = [];
  site: Site | null = null;
  siteId = 0;
  planning: Planning | null = null;
  rows: SlotRow[] = [];
  days: { iso: string; label: string; num: string; month: string }[] = [];
  selectedDay = this.toIso(new Date());
  onlyJoinable = false;
  me: Member | null = null;
  myMatches: Match[] = [];
  selectedMatch: Match | null = null;
  creationSlot: Slot | null = null;
  creationError = '';
  error = '';
  loading = true;
  busy = false;

  constructor() {
    this.buildDays();
    const stored = sessionStorage.getItem('padel.site');
    forkJoin({
      sites: this.siteService.getSites(),
      me: this.memberService.me(),
      matches: this.matchService.getMyMatches(),
    }).subscribe({
      next: ({ sites, me, matches }) => {
        this.sites = sites;
        this.me = me;
        this.myMatches = matches.filter((match) => new Date(match.startTime) >= new Date());
        const preferred = me.homeSiteId ?? (stored ? Number(stored) : sites[0]?.id);
        this.siteId = sites.some((site) => site.id === preferred) ? Number(preferred) : sites[0].id;
        this.loadPlanning();
      },
      error: (error) => {
        this.error = this.errors.toMessage(error);
        this.loading = false;
      },
    });
  }

  get limitDay(): string {
    if (!this.me) {
      return '';
    }
    const limit = new Date();
    limit.setDate(limit.getDate() + this.me.bookingWindowDays);
    return limit.toLocaleDateString('fr-BE', { day: '2-digit', month: 'long' });
  }

  get creationBlockedReason(): string {
    if (!this.me || !this.creationSlot) {
      return '';
    }
    if (this.me.balanceDue > 0) {
      return `Reservation bloquee : un solde de ${this.me.balanceDue} € reste du.`;
    }
    if (this.me.bannedUntil && new Date(this.me.bannedUntil) >= new Date()) {
      return `Penalite active jusqu'au ${this.me.bannedUntil} : aucune reservation possible.`;
    }
    const target = new Date(this.creationSlot.startTime);
    const limit = new Date();
    limit.setDate(limit.getDate() + this.me.bookingWindowDays);
    if (target > limit) {
      return `Hors fenetre : vous reservez au plus tot ${this.me.bookingWindowDays} jours avant le match.`;
    }
    return '';
  }

  allowed(site: Site): boolean {
    if (!this.me || this.me.type !== 'SITE') {
      return true;
    }
    return this.me.homeSiteId === site.id;
  }

  changeSite(event: Event): void {
    this.siteId = Number((event.target as HTMLSelectElement).value);
    sessionStorage.setItem('padel.site', String(this.siteId));
    this.loadPlanning();
  }

  selectDay(iso: string): void {
    this.selectedDay = iso;
    this.loadPlanning();
  }

  toggleJoinable(): void {
    this.onlyJoinable = !this.onlyJoinable;
    this.buildRows();
  }

  openSlot(slot: Slot): void {
    if (this.onlyJoinable && slot.match && !this.isJoinable(slot.match)) {
      return;
    }
    if (slot.match) {
      this.selectedMatch = slot.match;
    } else {
      this.creationError = '';
      this.creationSlot = slot;
    }
  }

  openMatch(match: Match): void {
    this.selectedMatch = match;
  }

  isPaidByMe(match: Match): boolean {
    const matricule = this.auth.currentUser?.matricule;
    return match.participants.some((player) => player.matricule === matricule && player.paid);
  }

  cellClass(slot: Slot): Record<string, boolean> {
    const match = slot.match;
    if (!match) {
      return { free: true };
    }
    if (this.onlyJoinable && !this.isJoinable(match)) {
      return { hidden: true };
    }
    const matricule = this.auth.currentUser?.matricule;
    const mine = match.participants.some((player) => player.matricule === matricule);
    return {
      full: match.participants.length === 4 && match.paidParticipantsCount === 4,
      unpaid: match.participants.length === 4 && match.paidParticipantsCount < 4,
      public: match.visibility === 'PUBLIC' && match.freeSlots > 0,
      mine,
    };
  }

  cellTitle(slot: Slot): string {
    const match = slot.match;
    if (!match) {
      return 'Libre';
    }
    if (this.onlyJoinable && !this.isJoinable(match)) {
      return '—';
    }
    if (match.participants.length === 4 && match.paidParticipantsCount === 4) {
      return 'Complet';
    }
    if (match.participants.length === 4) {
      return 'Impaye';
    }
    return match.visibility === 'PUBLIC' ? 'Public' : 'Prive';
  }

  cellSubtitle(slot: Slot): string {
    const match = slot.match;
    if (!match) {
      return 'Creer un match';
    }
    if (this.onlyJoinable && !this.isJoinable(match)) {
      return '';
    }
    if (match.participants.length === 4 && match.paidParticipantsCount < 4) {
      return 'Place liberee · 15 €';
    }
    if (match.freeSlots > 0 && match.visibility === 'PUBLIC') {
      return `${match.freeSlots} place${match.freeSlots > 1 ? 's' : ''} · 15 €`;
    }
    if (match.freeSlots > 0) {
      return `${match.participants.length}/4 joueurs`;
    }
    return '4 joueurs · paye';
  }

  actOn(match: Match): void {
    this.busy = true;
    const matricule = this.auth.currentUser?.matricule;
    const mine = match.participants.find((player) => player.matricule === matricule);
    // Voir my-matches : union de deux Observables de types differents.
    const request: Observable<unknown> = mine
      ? this.paymentService.payShare(match.id)
      : this.matchService.joinMatch(match.id);

    request.subscribe({
      next: () => {
        this.busy = false;
        this.selectedMatch = null;
        this.refresh();
      },
      error: (error: unknown) => {
        this.busy = false;
        this.error = this.errors.toMessage(error);
        this.selectedMatch = null;
      },
    });
  }

  create(request: CreateMatchRequest): void {
    this.busy = true;
    this.creationError = '';
    this.matchService.createMatch(request).subscribe({
      next: (match) => {
        this.paymentService.payShare(match.id).subscribe({
          next: () => {
            this.busy = false;
            this.creationSlot = null;
            this.refresh();
          },
          error: (error) => {
            this.busy = false;
            this.creationError = this.errors.toMessage(error);
            this.refresh();
          },
        });
      },
      error: (error) => {
        this.busy = false;
        this.creationError = this.errors.toMessage(error);
      },
    });
  }

  payBalance(): void {
    this.paymentService.payBalance().subscribe({
      next: () => this.refresh(),
      error: (error) => (this.error = this.errors.toMessage(error)),
    });
  }

  private isJoinable(match: Match): boolean {
    return match.freeSlots > 0 || match.paidParticipantsCount < match.participants.length;
  }

  private refresh(): void {
    this.memberService.me().subscribe((me) => (this.me = me));
    this.matchService
      .getMyMatches()
      .subscribe((matches) => (this.myMatches = matches.filter((m) => new Date(m.startTime) >= new Date())));
    this.loadPlanning();
  }

  private loadPlanning(): void {
    this.loading = true;
    this.error = '';
    this.site = this.sites.find((site) => site.id === this.siteId) ?? null;
    this.siteService.getPlanning(this.siteId, this.selectedDay).subscribe({
      next: (planning) => {
        this.planning = planning;
        this.buildRows();
        this.loading = false;
      },
      error: (error) => {
        this.error = this.errors.toMessage(error);
        this.loading = false;
      },
    });
  }

  private buildRows(): void {
    if (!this.planning) {
      this.rows = [];
      return;
    }
    const grouped = new Map<string, Slot[]>();
    for (const slot of this.planning.slots) {
      const time = new Date(slot.startTime).toTimeString().slice(0, 5);
      const cells = grouped.get(time) ?? [];
      cells.push(slot);
      grouped.set(time, cells);
    }
    this.rows = [...grouped.entries()].map(([time, cells]) => ({ time, cells }));
  }

  private buildDays(): void {
    const formatter = new Intl.DateTimeFormat('fr-BE', { weekday: 'short' });
    const monthFormatter = new Intl.DateTimeFormat('fr-BE', { month: 'short' });
    this.days = Array.from({ length: 7 }, (_, offset) => {
      const date = new Date();
      date.setDate(date.getDate() + offset);
      return {
        iso: this.toIso(date),
        label: formatter.format(date).replace('.', ''),
        num: String(date.getDate()).padStart(2, '0'),
        month: monthFormatter.format(date).replace('.', ''),
      };
    });
  }

  /** Format local YYYY-MM-DD : toISOString decalerait la date selon le fuseau. */
  private toIso(date: Date): string {
    return [
      date.getFullYear(),
      String(date.getMonth() + 1).padStart(2, '0'),
      String(date.getDate()).padStart(2, '0'),
    ].join('-');
  }
}
