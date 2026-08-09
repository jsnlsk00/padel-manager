import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'planning' },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'planning',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/user/planning/planning.component').then((m) => m.PlanningComponent),
  },
  {
    path: 'mes-matchs',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/user/my-matches/my-matches.component').then((m) => m.MyMatchesComponent),
  },
  {
    path: 'matchs-publics',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/user/public-matches/public-matches.component').then(
        (m) => m.PublicMatchesComponent,
      ),
  },
  {
    path: 'administration',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/admin/dashboard/admin-dashboard.component').then(
        (m) => m.AdminDashboardComponent,
      ),
  },
  { path: '**', redirectTo: 'planning' },
];
