import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, map, tap } from 'rxjs';
import { AuthUser, Role } from '../models/models';
import { environment } from '../../environments/environment';

const STORAGE_KEY = 'padel.session';

/**
 * Etat d'authentification de l'application. Le JWT est conserve en sessionStorage :
 * il disparait a la fermeture de l'onglet, et une seule authentification est
 * necessaire par session d'utilisation.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/auth`;

  private currentUserSubject = new BehaviorSubject<AuthUser | null>(this.restore());

  /** Flux de l'utilisateur courant, consomme par les composants. */
  readonly currentUser$: Observable<AuthUser | null> = this.currentUserSubject.asObservable();

  readonly isAdmin$ = this.currentUser$.pipe(
    map((user) => this.hasAnyRole(user, ['ROLE_ADMIN_SITE', 'ROLE_ADMIN_GLOBAL'])),
  );

  readonly isGlobalAdmin$ = this.currentUser$.pipe(
    map((user) => this.hasAnyRole(user, ['ROLE_ADMIN_GLOBAL'])),
  );

  get currentUser(): AuthUser | null {
    return this.currentUserSubject.value;
  }

  get token(): string | null {
    return this.currentUser?.token ?? null;
  }

  login(matricule: string, password: string): Observable<AuthUser> {
    return this.http
      .post<AuthUser>(`${this.base}/login`, { matricule: matricule.toUpperCase(), password })
      .pipe(tap((user) => this.persist(user)));
  }

  refresh(): Observable<AuthUser> {
    const refreshToken = this.currentUser?.refreshToken ?? '';
    return this.http
      .post<AuthUser>(`${this.base}/refresh`, { refreshToken })
      .pipe(tap((user) => this.persist(user)));
  }

  logout(): void {
    sessionStorage.removeItem(STORAGE_KEY);
    this.currentUserSubject.next(null);
  }

  isAuthenticated(): boolean {
    return this.currentUser !== null;
  }

  hasRole(role: Role): boolean {
    return this.currentUser?.roles.includes(role) ?? false;
  }

  isAdmin(): boolean {
    return this.hasRole('ROLE_ADMIN_SITE') || this.hasRole('ROLE_ADMIN_GLOBAL');
  }

  private hasAnyRole(user: AuthUser | null, roles: Role[]): boolean {
    return user !== null && roles.some((role) => user.roles.includes(role));
  }

  private persist(user: AuthUser): void {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  private restore(): AuthUser | null {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}
