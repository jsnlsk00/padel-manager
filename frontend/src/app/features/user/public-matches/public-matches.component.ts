import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatchService } from '../../../core/services/match.service';
import { MemberService } from '../../../core/services/member.service';
import { SiteService } from '../../../core/services/site.service';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { Match, Member, Site } from '../../../core/models/models';
import { MatchDialogComponent } from '../../../shared/components/match-dialog.component';
import { SlotTimePipe } from '../../../core/pipes/slot-time.pipe';

/** Matchs publics ouverts : filtres date, site, prix, places restantes. */
@Component({
  selector: 'app-public-matches',
  standalone: true,
  imports: [CommonModule, MatchDialogComponent, SlotTimePipe],
  template: `
    <div class="page">
      <header class="head">
        <div>
          <h1>Matchs publics</h1>
          <p class="sub">Premier paye, premier servi. 15 € la place.</p>
        </div>
        <div class="filters">
          <select class="input select" [value]="siteFilter" (change)="setSite($event)" data-cy="site-filter">
            <option value="">Tous les sites</option>
            <option *ngFor="let site of sites" [value]="site.id">{{ site.name }}</option>
          </select>
          <input class="input date" type="date" [value]="dayFilter" (change)="setDay($event)" />
          <label class="filter">
            <input type="checkbox" [checked]="onlyTwoOrMore" (change)="toggleSeats()" />
            Au moins 2 places
          </label>
        </div>
      </header>

      <p class="notice alert" *ngIf="error">{{ error }}</p>
      <p class="hint" *ngIf="!loading && !filtered.length">Aucun match public ne correspond a ces filtres.</p>

      <div class="grid">
        <button
          type="button"
          class="match card"
          *ngFor="let match of filtered"
          (click)="selected = match"
          [attr.data-cy]="'public-match-' + match.id"
        >
          <span class="top">
            <span class="seats">{{ match.freeSlots }} place{{ match.freeSlots > 1 ? 's' : '' }}</span>
            <span class="price tabular">15 €</span>
          </span>
          <span class="when">{{ match.startTime | date: 'EEEE d MMMM' }}</span>
          <span class="time tabular">{{ match.startTime | slotTime }}</span>
          <span class="where">{{ match.siteName }} · terrain {{ match.courtNumber }}</span>
          <span class="host">Organise par {{ match.organizerName }}</span>
        </button>
      </div>
    </div>

    <app-match-dialog
      *ngIf="selected"
      [match]="selected"
      [balanceDue]="me?.balanceDue ?? 0"
      (close)="selected = null"
      (action)="join($event)"
    ></app-match-dialog>
  `,
  styles: [
    `
      .page { padding: 28px 32px 56px; display: flex; flex-direction: column; gap: 24px; }
      .head { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; flex-wrap: wrap; }
      h1 { font-size: 38px; font-weight: 700; }
      .sub { margin: 6px 0 0; color: var(--ink-60); font-size: 13.5px; }
      .filters { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
      .select, .date { width: auto; padding: 9px 12px; font-size: 13.5px; }
      .filter { display: flex; align-items: center; gap: 8px; padding: 9px 12px; background: var(--surface); border: 1px solid rgba(18,33,26,.18); font-size: 13.5px; cursor: pointer; }
      .filter input { accent-color: var(--ink); width: 15px; height: 15px; }
      .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; }
      .match { display: flex; flex-direction: column; gap: 5px; align-items: flex-start; text-align: left; padding: 18px; cursor: pointer; font: inherit; color: inherit; }
      .match:hover { border-color: var(--ink); }
      .top { display: flex; justify-content: space-between; width: 100%; margin-bottom: 6px; }
      .seats { background: var(--lime); padding: 3px 9px; font-family: var(--font-display); font-size: 13px; font-weight: 600; letter-spacing: 0.08em; text-transform: uppercase; }
      .price { font-family: var(--font-display); font-size: 18px; font-weight: 600; }
      .when { font-family: var(--font-display); font-size: 21px; font-weight: 600; text-transform: uppercase; }
      .time { font-size: 14px; }
      .where, .host { font-size: 12.5px; color: var(--ink-60); }
      .hint { color: var(--ink-45); }
      .notice.alert { padding: 13px 15px; background: var(--red-soft); border: 1px solid var(--red); color: var(--red); font-size: 13.5px; }
      @media (max-width: 900px) { .page { padding: 20px 16px 40px; } }
    `,
  ],
})
export class PublicMatchesComponent {
  private matchService = inject(MatchService);
  private siteService = inject(SiteService);
  private memberService = inject(MemberService);
  private errors = inject(ErrorMessageService);

  matches: Match[] = [];
  sites: Site[] = [];
  me: Member | null = null;
  selected: Match | null = null;
  siteFilter = '';
  dayFilter = '';
  onlyTwoOrMore = false;
  error = '';
  loading = true;

  constructor() {
    this.siteService.getSites().subscribe((sites) => (this.sites = sites));
    this.memberService.me().subscribe((me) => (this.me = me));
    this.load();
  }

  get filtered(): Match[] {
    return this.matches.filter((match) => {
      if (this.dayFilter && !match.startTime.startsWith(this.dayFilter)) {
        return false;
      }
      if (this.onlyTwoOrMore && match.freeSlots < 2) {
        return false;
      }
      return true;
    });
  }

  setSite(event: Event): void {
    this.siteFilter = (event.target as HTMLSelectElement).value;
    this.load();
  }

  setDay(event: Event): void {
    this.dayFilter = (event.target as HTMLInputElement).value;
  }

  toggleSeats(): void {
    this.onlyTwoOrMore = !this.onlyTwoOrMore;
  }

  join(match: Match): void {
    this.matchService.joinMatch(match.id).subscribe({
      next: () => {
        this.selected = null;
        this.load();
      },
      error: (error) => {
        this.error = this.errors.toMessage(error);
        this.selected = null;
      },
    });
  }

  private load(): void {
    this.loading = true;
    const siteId = this.siteFilter ? Number(this.siteFilter) : undefined;
    this.matchService.getPublicMatches(siteId).subscribe({
      next: (matches) => {
        this.matches = matches;
        this.loading = false;
      },
      error: (error) => {
        this.error = this.errors.toMessage(error);
        this.loading = false;
      },
    });
  }
}
