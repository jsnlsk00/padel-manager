import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';
import { MemberCategoryPipe } from './core/pipes/member-category.pipe';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, MemberCategoryPipe],
  template: `
    <header class="bar" *ngIf="auth.currentUser$ | async as user">
      <div class="left">
        <a class="brand" routerLink="/planning">
          <span class="dot"></span>
          <span>Padel</span>
        </a>
        <nav>
          <a routerLink="/planning" routerLinkActive="active">Planning</a>
          <a routerLink="/matchs-publics" routerLinkActive="active">Matchs publics</a>
          <a routerLink="/mes-matchs" routerLinkActive="active">Mes matchs</a>
          <a *ngIf="auth.isAdmin$ | async" routerLink="/administration" routerLinkActive="active">
            Administration
          </a>
        </nav>
      </div>
      <div class="right">
        <span class="matricule tabular">{{ user.matricule }}</span>
        <span class="who">{{ user.firstName }} {{ user.lastName }}</span>
        <span class="cat">{{ user.memberType | memberCategory }}</span>
        <button type="button" class="quit" (click)="logout()">Quitter</button>
      </div>
    </header>

    <main>
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [
    `
      .bar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 24px;
        padding: 0 32px;
        background: var(--ink);
        color: var(--bg);
        flex-wrap: wrap;
      }
      .left { display: flex; align-items: center; gap: 28px; }
      .brand {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 18px 0;
        font-family: var(--font-display);
        font-size: 19px;
        font-weight: 700;
        letter-spacing: 0.06em;
        text-transform: uppercase;
        color: var(--bg);
        text-decoration: none;
      }
      .brand:hover { color: var(--lime); }
      .dot { width: 11px; height: 11px; border-radius: 50%; background: var(--lime); }
      nav { display: flex; gap: 2px; }
      nav a {
        padding: 18px 14px;
        font-family: var(--font-display);
        font-size: 15.5px;
        font-weight: 600;
        letter-spacing: 0.1em;
        text-transform: uppercase;
        color: rgba(244, 246, 242, 0.6);
        text-decoration: none;
        border-bottom: 3px solid transparent;
      }
      nav a:hover { color: var(--bg); }
      nav a.active { color: var(--bg); border-bottom-color: var(--lime); }
      .right { display: flex; align-items: center; gap: 14px; font-size: 13px; }
      .matricule {
        font-family: var(--font-display);
        font-size: 14px;
        letter-spacing: 0.16em;
        color: var(--lime);
      }
      .who { opacity: 0.8; }
      .cat { opacity: 0.5; }
      .quit {
        background: none;
        border: 1px solid rgba(244, 246, 242, 0.3);
        color: var(--bg);
        font-size: 12.5px;
        padding: 6px 12px;
        border-radius: 2px;
        cursor: pointer;
      }
      .quit:hover { border-color: var(--lime); color: var(--lime); }
      @media (max-width: 900px) {
        .bar { padding: 0 16px; }
        .left { gap: 12px; flex-wrap: wrap; }
        nav a { padding: 12px 10px; font-size: 14px; }
        .who, .cat { display: none; }
      }
    `,
  ],
})
export class AppComponent {
  readonly auth = inject(AuthService);
  private router = inject(Router);

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
