import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { SiteService } from '../../../core/services/site.service';
import { MemberType, Site } from '../../../core/models/models';

/** Ecran d'entree : matricule + mot de passe, puis choix du site de travail. */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="page">
      <section class="pitch">
        <h1>
          Votre matricule,<br />
          <span class="mark">et c'est parti.</span>
        </h1>
        <p>
          La lettre de votre matricule determine les sites accessibles et le delai a partir
          duquel vous pouvez creer un match.
        </p>
        <div class="rules">
          <div class="rule" *ngFor="let rule of rules">
            <span class="code">{{ rule.code }}</span>
            <span class="text">{{ rule.text }}</span>
            <span class="delay tabular">{{ rule.delay }}</span>
          </div>
        </div>
      </section>

      <section class="form">
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label class="field-label" for="matricule">Matricule</label>
          <input
            id="matricule"
            data-cy="matricule"
            class="input matricule-input"
            formControlName="matricule"
            placeholder="G1042"
            autocomplete="username"
            (input)="onMatriculeInput()"
          />
          <p class="hint" [class.error]="matriculeInvalid">{{ matriculeHint }}</p>

          <label class="field-label" for="password">Mot de passe</label>
          <input
            id="password"
            data-cy="password"
            class="input"
            type="password"
            formControlName="password"
            autocomplete="current-password"
          />
          <p class="hint error" *ngIf="passwordTouchedAndEmpty">Le mot de passe est obligatoire.</p>

          <div class="sites" *ngIf="sites.length">
            <span class="field-label">Site de travail</span>
            <button
              type="button"
              *ngFor="let site of sites"
              class="site"
              [class.selected]="site.id === selectedSiteId"
              (click)="selectSite(site.id)"
            >
              <span class="site-name">{{ site.name }}</span>
              <span class="site-detail tabular">
                {{ site.courts.length }} terrains · {{ site.openingTime }} – {{ site.closingTime }}
              </span>
            </button>
          </div>

          <p class="error server" *ngIf="serverError" data-cy="login-error">{{ serverError }}</p>

          <button class="btn submit" type="submit" data-cy="submit" [disabled]="form.invalid || loading">
            {{ loading ? 'Connexion…' : 'Acceder au planning' }}
          </button>
        </form>
      </section>
    </div>
  `,
  styles: [
    `
      .page {
        min-height: calc(100vh - 60px);
        display: grid;
        grid-template-columns: 1fr 1fr;
      }
      .pitch {
        padding: 64px 56px;
        display: flex;
        flex-direction: column;
        justify-content: center;
        gap: 30px;
        border-right: 1px solid var(--line);
      }
      h1 { font-size: 68px; line-height: 0.95; font-weight: 700; }
      .mark { background: var(--lime); padding: 0 10px; }
      .pitch p { margin: 0; max-width: 430px; color: var(--ink-60); line-height: 1.6; }
      .rules { display: flex; flex-direction: column; gap: 1px; background: var(--line); border: 1px solid var(--line); }
      .rule {
        display: grid;
        grid-template-columns: 92px 1fr 92px;
        gap: 14px;
        align-items: center;
        padding: 15px 18px;
        background: var(--surface);
      }
      .code { font-family: var(--font-display); font-size: 20px; font-weight: 700; letter-spacing: 0.08em; }
      .text { font-size: 13.5px; color: var(--ink-60); }
      .delay { font-family: var(--font-display); font-size: 17px; font-weight: 600; text-align: right; }
      .form { padding: 64px 56px; display: flex; align-items: center; }
      form { width: 100%; max-width: 440px; display: flex; flex-direction: column; }
      .matricule-input {
        font-family: var(--font-display);
        font-size: 28px;
        font-weight: 600;
        letter-spacing: 0.16em;
        text-transform: uppercase;
        font-variant-numeric: tabular-nums;
      }
      .hint { margin: 8px 0 22px; font-size: 13.5px; color: var(--ink-45); min-height: 18px; }
      .sites { display: flex; flex-direction: column; gap: 8px; margin: 24px 0 4px; }
      .site {
        display: flex;
        flex-direction: column;
        gap: 3px;
        align-items: flex-start;
        text-align: left;
        padding: 14px 18px;
        background: var(--surface);
        border: 1px solid var(--line);
        border-radius: 2px;
        cursor: pointer;
      }
      .site:hover { border-color: var(--ink); }
      .site.selected { background: var(--lime); border-color: var(--ink); }
      .site-name { font-family: var(--font-display); font-size: 20px; font-weight: 600; text-transform: uppercase; }
      .site-detail { font-size: 12.5px; color: var(--ink-60); }
      .server { margin: 18px 0 0; }
      .submit { margin-top: 24px; min-height: 50px; }
      @media (max-width: 900px) {
        .page { grid-template-columns: 1fr; }
        .pitch { border-right: none; border-bottom: 1px solid var(--line); padding: 36px 20px; }
        h1 { font-size: 44px; }
        .form { padding: 32px 20px; }
      }
    `,
  ],
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private siteService = inject(SiteService);
  private errors = inject(ErrorMessageService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly rules = [
    { code: 'Gxxxx', text: 'Membre global, tous les sites', delay: '21 jours' },
    { code: 'Sxxxxx', text: 'Membre de site, son site uniquement', delay: '14 jours' },
    { code: 'Lxxxxx', text: 'Membre libre, tous les sites', delay: '5 jours' },
  ];

  readonly form = this.fb.nonNullable.group({
    matricule: ['', [Validators.required, Validators.pattern(/^[GSLgsl][0-9]{4,5}$/)]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  sites: Site[] = [];
  selectedSiteId: number | null = null;
  serverError = '';
  loading = false;

  constructor() {
    this.siteService.getSites().subscribe({
      next: (sites) => {
        this.sites = sites;
        this.selectedSiteId = sites.length ? sites[0].id : null;
      },
      error: (error) => (this.serverError = this.errors.toMessage(error)),
    });
  }

  get matriculeInvalid(): boolean {
    const control = this.form.controls.matricule;
    return control.touched && control.invalid;
  }

  get matriculeHint(): string {
    const value = this.form.controls.matricule.value.trim().toUpperCase();
    if (!value) {
      return 'Une lettre G, S ou L suivie de 4 ou 5 chiffres.';
    }
    if (this.form.controls.matricule.invalid) {
      return 'Format attendu : G, S ou L suivi de 4 ou 5 chiffres.';
    }
    const category: Record<string, string> = {
      G: 'Membre global · 21 jours a l\'avance',
      S: 'Membre de site · 14 jours a l\'avance',
      L: 'Membre libre · 5 jours a l\'avance',
    };
    return category[value.charAt(0)] ?? '';
  }

  get passwordTouchedAndEmpty(): boolean {
    const control = this.form.controls.password;
    return control.touched && control.invalid;
  }

  onMatriculeInput(): void {
    const control = this.form.controls.matricule;
    control.setValue(control.value.toUpperCase(), { emitEvent: false });
  }

  selectSite(siteId: number): void {
    this.selectedSiteId = siteId;
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    this.serverError = '';
    const { matricule, password } = this.form.getRawValue();

    this.auth.login(matricule, password).subscribe({
      next: () => {
        if (this.selectedSiteId != null) {
          sessionStorage.setItem('padel.site', String(this.selectedSiteId));
        }
        const redirect = this.route.snapshot.queryParamMap.get('redirect') ?? '/planning';
        void this.router.navigateByUrl(redirect);
      },
      error: (error) => {
        this.serverError = this.errors.toMessage(error);
        this.loading = false;
      },
    });
  }

  protected readonly memberTypes: MemberType[] = ['GLOBAL', 'SITE', 'FREE'];
}
