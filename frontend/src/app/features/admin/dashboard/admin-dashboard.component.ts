import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/auth/auth.service';
import { StatsService } from '../../../core/services/stats.service';
import { MemberService } from '../../../core/services/member.service';
import { SiteService } from '../../../core/services/site.service';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { Member, Site, Stats } from '../../../core/models/models';
import { MemberCategoryPipe } from '../../../core/pipes/member-category.pipe';

/**
 * Tableau de bord d'administration. L'admin global bascule entre son site et
 * l'ensemble du reseau ; l'admin de site ne voit que son propre site.
 */
@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MemberCategoryPipe],
  template: `
    <div class="page">
      <header class="head">
        <div>
          <h1>{{ stats?.scope === 'GLOBAL' ? 'Reseau — tous les sites' : siteName }}</h1>
          <p class="sub">
            {{ stats?.scope === 'GLOBAL' ? 'Portee globale' : 'Portee limitee a votre site' }} ·
            fenetre de 7 jours
          </p>
        </div>
        <div class="scope" *ngIf="isGlobalAdmin">
          <button
            type="button"
            *ngFor="let option of scopes"
            [class.selected]="scope === option.value"
            (click)="setScope(option.value)"
            [attr.data-cy]="'scope-' + option.value"
          >
            {{ option.label }}
          </button>
        </div>
      </header>

      <p class="notice alert" *ngIf="error" data-cy="admin-error">{{ error }}</p>
      <p class="hint" *ngIf="loading">Chargement des statistiques…</p>

      <ng-container *ngIf="stats as data">
        <div class="kpis">
          <div class="kpi card">
            <span class="kpi-label">Chiffre d'affaires</span>
            <span class="kpi-value tabular">{{ data.revenue | number: '1.0-0' }} €</span>
            <span class="kpi-note">encaisse sur la periode</span>
          </div>
          <div class="kpi card">
            <span class="kpi-label">Occupation</span>
            <span class="kpi-value tabular">{{ data.occupancyRate }} %</span>
            <span class="kpi-note tabular">{{ data.totalMatches }} matchs</span>
          </div>
          <div class="kpi card">
            <span class="kpi-label">Impayes</span>
            <span class="kpi-value tabular">{{ data.outstanding | number: '1.0-0' }} €</span>
            <span class="kpi-note tabular">{{ data.unpaid.length }} places non reglees</span>
          </div>
          <div class="kpi card">
            <span class="kpi-label">Matchs publics</span>
            <span class="kpi-value tabular">{{ publicShare(data) }} %</span>
            <span class="kpi-note tabular">
              {{ data.publicMatches }} publics · {{ data.privateMatches }} prives
            </span>
          </div>
        </div>

        <div class="two">
          <section class="card">
            <h2>Occupation et chiffre d'affaires par site</h2>
            <div class="site-list">
              <div class="site" *ngFor="let site of data.perSite">
                <div class="site-head">
                  <span class="site-name">{{ site.siteName }}</span>
                  <span class="site-detail tabular">{{ site.courts }} terrains · {{ site.matches }} matchs</span>
                </div>
                <div class="bar"><span [style.width.%]="site.occupancyRate"></span></div>
                <div class="site-foot tabular">
                  <span>{{ site.occupancyRate }} % d'occupation</span>
                  <span>{{ site.revenue | number: '1.0-0' }} €</span>
                </div>
              </div>
            </div>
          </section>

          <section class="card">
            <h2>Creneaux les plus demandes</h2>
            <div class="slots">
              <div class="slot" *ngFor="let slot of data.slotDemand">
                <span class="slot-time tabular">{{ slot.startTime.substring(0, 5) }}</span>
                <span class="slot-bar"><span [style.width.%]="slotWidth(slot.matches, data)"></span></span>
                <span class="slot-count tabular">{{ slot.matches }}</span>
              </div>
            </div>
          </section>
        </div>

        <div class="two">
          <section class="card">
            <h2>
              Impayes et penalites
              <span class="total tabular">{{ data.outstanding | number: '1.0-0' }} € a recouvrer</span>
            </h2>
            <table>
              <thead>
                <tr>
                  <th>Joueur</th>
                  <th>Match</th>
                  <th>Motif</th>
                  <th class="right">Montant</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let row of data.unpaid.slice(0, 8)">
                  <td><span class="mat tabular">{{ row.matricule }}</span> {{ row.fullName }}</td>
                  <td class="tabular">{{ row.startTime | date: 'EEE d MMM, HH:mm' }}</td>
                  <td class="muted">{{ row.reason }}</td>
                  <td class="right tabular due">{{ row.amount | number: '1.0-0' }} €</td>
                </tr>
                <tr *ngIf="!data.unpaid.length">
                  <td colspan="4" class="muted">Aucun impaye.</td>
                </tr>
              </tbody>
            </table>
          </section>

          <div class="stack">
            <section class="card">
              <h2>Membres par categorie</h2>
              <div class="members">
                <div class="member" *ngFor="let row of data.membersByType">
                  <span class="member-label">
                    <span class="member-name">{{ row.label }}</span>
                    <span class="member-note">{{ row.type | memberCategory: true }}</span>
                  </span>
                  <span class="member-count tabular">{{ row.count }}</span>
                </div>
              </div>
            </section>

            <section class="card">
              <h2>Fermetures a venir</h2>
              <div class="closures">
                <div class="closure" *ngFor="let closure of closures">
                  <span class="tabular">{{ closure.closedOn }}</span>
                  <span class="muted">{{ closure.reason }}</span>
                  <span class="scope-tag">{{ closure.global ? 'Reseau' : 'Site' }}</span>
                </div>
                <p class="hint" *ngIf="!closures.length">Aucune fermeture declaree.</p>

                <form class="closure-form" [formGroup]="closureForm" (ngSubmit)="submitClosure()">
                  <span class="field-label">Declarer une fermeture</span>
                  <select
                    class="input"
                    formControlName="siteId"
                    *ngIf="isGlobalAdmin"
                    data-cy="closure-site">
                    <option *ngFor="let site of sites" [value]="site.id">{{ site.name }}</option>
                  </select>
                  <div class="closure-row">
                    <input
                      class="input"
                      type="date"
                      [min]="today"
                      formControlName="closedOn"
                      data-cy="closure-date" />
                    <input
                      class="input"
                      placeholder="Motif (ex. maintenance)"
                      formControlName="reason"
                      data-cy="closure-reason" />
                  </div>
                  <button
                    class="btn"
                    type="submit"
                    [disabled]="closureForm.invalid || savingClosure"
                    data-cy="closure-submit">
                    {{ savingClosure ? 'Enregistrement...' : 'Declarer la fermeture' }}
                  </button>
                  <p class="hint alert" *ngIf="closureError">{{ closureError }}</p>
                </form>
              </div>
            </section>
          </div>
        </div>

        <section class="card" *ngIf="members.length">
          <h2>Membres visibles dans votre portee</h2>
          <table>
            <thead>
              <tr>
                <th>Matricule</th>
                <th>Nom</th>
                <th>Categorie</th>
                <th>Site</th>
                <th class="right">Solde</th>
                <th class="right">Penalite</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let member of members.slice(0, 12)">
                <td class="mat tabular">{{ member.matricule }}</td>
                <td>{{ member.firstName }} {{ member.lastName }}</td>
                <td class="muted">{{ member.type | memberCategory }}</td>
                <td class="muted">{{ member.homeSiteName ?? '—' }}</td>
                <td class="right tabular" [class.due]="member.balanceDue > 0">
                  {{ member.balanceDue | number: '1.0-0' }} €
                </td>
                <td class="right tabular muted">{{ member.bannedUntil ?? '—' }}</td>
              </tr>
            </tbody>
          </table>
        </section>
      </ng-container>
    </div>
  `,
  styles: [
    `
      .page { padding: 28px 32px 56px; display: flex; flex-direction: column; gap: 22px; }
      .head { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; flex-wrap: wrap; }
      h1 { font-size: 38px; font-weight: 700; }
      .sub { margin: 6px 0 0; color: var(--ink-60); font-size: 13.5px; }
      .scope { display: flex; border: 1px solid rgba(18,33,26,.18); background: var(--surface); }
      .scope button {
        padding: 11px 18px;
        border: none;
        background: none;
        font-family: var(--font-display);
        font-size: 14px;
        font-weight: 600;
        letter-spacing: 0.1em;
        text-transform: uppercase;
        cursor: pointer;
        color: var(--ink);
      }
      .scope button.selected { background: var(--lime); }
      .kpis { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
      .kpi { padding: 18px 20px; display: flex; flex-direction: column; gap: 8px; }
      .kpi-label { font-family: var(--font-display); font-size: 12.5px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--ink-45); }
      .kpi-value { font-family: var(--font-display); font-size: 40px; font-weight: 700; line-height: 1; }
      .kpi-note { font-size: 12.5px; color: var(--ink-60); }
      .two { display: grid; grid-template-columns: 1.35fr 1fr; gap: 20px; align-items: start; }
      .stack { display: flex; flex-direction: column; gap: 20px; }
      h2 {
        display: flex;
        align-items: baseline;
        justify-content: space-between;
        gap: 12px;
        padding: 14px 18px;
        border-bottom: 1px solid var(--line);
        font-size: 14px;
        letter-spacing: 0.16em;
      }
      .total { font-family: var(--font-body); font-size: 12.5px; letter-spacing: 0; text-transform: none; color: var(--red); }
      .site-list { padding: 6px 18px 18px; }
      .site { padding: 16px 0; border-bottom: 1px solid rgba(18,33,26,.08); display: flex; flex-direction: column; gap: 9px; }
      .site-head { display: flex; justify-content: space-between; align-items: baseline; gap: 14px; }
      .site-name { font-family: var(--font-display); font-size: 19px; font-weight: 600; text-transform: uppercase; }
      .site-detail, .site-foot { font-size: 12.5px; color: var(--ink-60); }
      .site-foot { display: flex; justify-content: space-between; }
      .bar { height: 12px; background: #eef1ea; position: relative; }
      .bar span { position: absolute; inset: 0 auto 0 0; background: var(--lime); border-right: 2px solid var(--ink); }
      .slots { padding: 16px 18px; display: flex; flex-direction: column; gap: 9px; }
      .slot { display: grid; grid-template-columns: 56px 1fr 34px; gap: 12px; align-items: center; }
      .slot-time { font-family: var(--font-display); font-size: 16px; font-weight: 600; }
      .slot-bar { height: 10px; background: #eef1ea; position: relative; display: block; }
      .slot-bar span { position: absolute; inset: 0 auto 0 0; background: var(--ink); }
      .slot-count { font-size: 12.5px; text-align: right; color: var(--ink-60); }
      table { width: 100%; border-collapse: collapse; font-size: 13.5px; }
      th { text-align: left; padding: 10px 18px; font-weight: 500; font-size: 12px; letter-spacing: 0.1em; text-transform: uppercase; color: var(--ink-45); }
      td { padding: 12px 18px; border-top: 1px solid rgba(18,33,26,.08); }
      .right { text-align: right; }
      .muted { color: var(--ink-60); }
      .due { color: var(--red); }
      .mat { color: var(--ink-45); }
      .members { padding: 16px 18px; display: flex; flex-direction: column; gap: 12px; }
      .member { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; padding-bottom: 10px; border-bottom: 1px solid rgba(18,33,26,.07); }
      .member-label { display: flex; flex-direction: column; gap: 2px; }
      .member-name { font-family: var(--font-display); font-size: 17px; font-weight: 600; text-transform: uppercase; }
      .member-note { font-size: 12px; color: var(--ink-45); }
      .member-count { font-family: var(--font-display); font-size: 26px; font-weight: 700; }
      .closures { padding: 16px 18px; display: flex; flex-direction: column; gap: 10px; }
      .closure-form {
        display: flex; flex-direction: column; gap: 8px;
        margin-top: 6px; padding-top: 14px; border-top: 1px solid rgba(18,33,26,.08);
      }
      .closure-row { display: grid; grid-template-columns: 1fr 1.4fr; gap: 8px; }
      .closure-form .input { padding: 9px 12px; font-size: 13.5px; }
      .closure-form .btn { padding: 10px 16px; font-size: 14px; }
      .hint.alert { color: var(--red); }
      @media (max-width: 600px) { .closure-row { grid-template-columns: 1fr; } }
      .closure { display: grid; grid-template-columns: 100px 1fr auto; gap: 12px; align-items: baseline; font-size: 13px; }
      .scope-tag { font-size: 11px; letter-spacing: 0.12em; text-transform: uppercase; color: var(--ink-45); }
      .hint { color: var(--ink-45); font-size: 13px; }
      .notice.alert { padding: 13px 15px; background: var(--red-soft); border: 1px solid var(--red); color: var(--red); font-size: 13.5px; }
      @media (max-width: 1100px) {
        .kpis { grid-template-columns: repeat(2, 1fr); }
        .two { grid-template-columns: 1fr; }
        .page { padding: 20px 16px 40px; }
      }
    `,
  ],
})
export class AdminDashboardComponent {
  private statsService = inject(StatsService);
  private memberService = inject(MemberService);
  private siteService = inject(SiteService);
  private fb = inject(FormBuilder);
  private errors = inject(ErrorMessageService);
  private auth = inject(AuthService);

  readonly scopes = [
    { value: 'site' as const, label: 'Mon site' },
    { value: 'global' as const, label: 'Reseau' },
  ];

  stats: Stats | null = null;
  members: Member[] = [];
  closures: { closedOn: string; reason: string; global: boolean }[] = [];
  sites: Site[] = [];
  scope: 'site' | 'global' = 'global';
  error = '';
  loading = true;

  /** Bornes et etat du formulaire de declaration de fermeture. */
  readonly today = new Date().toISOString().slice(0, 10);
  readonly closureForm = this.fb.nonNullable.group({
    siteId: [0, Validators.required],
    closedOn: ['', Validators.required],
    reason: ['', [Validators.required, Validators.minLength(3)]],
  });
  savingClosure = false;
  closureError = '';

  constructor() {
    const user = this.auth.currentUser;
    this.scope = this.isGlobalAdmin ? 'global' : 'site';
    this.siteService.getSites().subscribe((sites) => {
      this.sites = sites;
      if (!this.closureForm.controls.siteId.value) {
        this.closureForm.patchValue({ siteId: user?.adminSiteId ?? sites[0]?.id ?? 0 });
      }
    });
    this.memberService.list().subscribe({
      next: (members) => (this.members = members),
      error: () => undefined,
    });
    if (user?.adminSiteId) {
      this.loadClosures(user.adminSiteId);
    }
    this.load();
  }

  get isGlobalAdmin(): boolean {
    return this.auth.hasRole('ROLE_ADMIN_GLOBAL');
  }

  get siteName(): string {
    const siteId = this.stats?.siteId ?? this.auth.currentUser?.adminSiteId;
    return this.sites.find((site) => site.id === siteId)?.name ?? 'Mon site';
  }

  publicShare(stats: Stats): number {
    return stats.totalMatches === 0
      ? 0
      : Math.round((stats.publicMatches / stats.totalMatches) * 100);
  }

  slotWidth(matches: number, stats: Stats): number {
    const max = Math.max(1, ...stats.slotDemand.map((slot) => slot.matches));
    return Math.round((matches / max) * 100);
  }

  setScope(scope: 'site' | 'global'): void {
    this.scope = scope;
    this.load();
  }

  private load(): void {
    this.loading = true;
    this.error = '';
    const adminSiteId = this.auth.currentUser?.adminSiteId;

    const request =
      this.scope === 'global' && this.isGlobalAdmin
        ? this.statsService.global()
        : this.statsService.site(adminSiteId ?? this.sites[0]?.id ?? 1);

    request.subscribe({
      next: (stats) => {
        this.stats = stats;
        this.loading = false;
        const target = stats.siteId ?? this.auth.currentUser?.adminSiteId ?? this.sites[0]?.id ?? null;
        if (target) {
          this.loadClosures(target);
        }
      },
      error: (error) => {
        this.error = this.errors.toMessage(error);
        this.loading = false;
      },
    });
  }

  /**
   * Declare une fermeture sur le site cible. Le backend refuse la creation si
   * l'admin n'a pas la portee sur ce site (403) ; le message est reaffiche tel quel.
   */
  submitClosure(): void {
    this.closureForm.markAllAsTouched();
    const siteId = Number(this.closureForm.controls.siteId.value);
    if (this.closureForm.invalid || !siteId) {
      return;
    }

    this.savingClosure = true;
    this.closureError = '';
    const { closedOn, reason } = this.closureForm.getRawValue();

    this.siteService.addClosure(siteId, closedOn, reason).subscribe({
      next: () => {
        this.savingClosure = false;
        this.closureForm.patchValue({ closedOn: '', reason: '' });
        this.closureForm.controls.closedOn.markAsUntouched();
        this.closureForm.controls.reason.markAsUntouched();
        this.loadClosures(siteId);
        this.load();
      },
      error: (error) => {
        this.closureError = this.errors.toMessage(error);
        this.savingClosure = false;
      },
    });
  }

  private loadClosures(siteId: number): void {
    this.siteService.getClosures(siteId).subscribe({
      next: (closures) => (this.closures = closures),
      error: () => (this.closures = []),
    });
  }
}
