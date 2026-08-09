import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { AuthUser } from '../models/models';
import { environment } from '../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  const user: AuthUser = {
    token: 'jwt-access',
    refreshToken: 'jwt-refresh',
    matricule: 'G1042',
    firstName: 'Thomas',
    lastName: 'Leroy',
    memberType: 'GLOBAL',
    homeSiteId: null,
    adminSiteId: null,
    roles: ['ROLE_USER'],
  };

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), AuthService],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  describe('login', () => {
    it('envoie le matricule en majuscules et conserve la session', () => {
      // Arrange / Act
      service.login('g1042', 'Padel2026!').subscribe();

      const request = http.expectOne(`${environment.apiUrl}/api/auth/login`);

      // Assert
      expect(request.request.method).toBe('POST');
      expect(request.request.body.matricule).toBe('G1042');

      request.flush(user);

      expect(service.isAuthenticated()).toBeTrue();
      expect(service.token).toBe('jwt-access');
      expect(sessionStorage.getItem('padel.session')).toContain('G1042');
    });

    it('publie l utilisateur courant sur currentUser$', (done) => {
      service.login('G1042', 'Padel2026!').subscribe(() => {
        service.currentUser$.subscribe((current) => {
          expect(current?.matricule).toBe('G1042');
          done();
        });
      });
      http.expectOne(`${environment.apiUrl}/api/auth/login`).flush(user);
    });

    it('ne conserve rien quand le serveur refuse', () => {
      service.login('G1042', 'faux').subscribe({ error: () => undefined });
      http
        .expectOne(`${environment.apiUrl}/api/auth/login`)
        .flush({ message: 'Identifiants invalides' }, { status: 401, statusText: 'Unauthorized' });

      expect(service.isAuthenticated()).toBeFalse();
      expect(sessionStorage.getItem('padel.session')).toBeNull();
    });
  });

  describe('roles', () => {
    it('reconnait un administrateur de site', () => {
      service.login('A1001', 'Padel2026!').subscribe();
      http
        .expectOne(`${environment.apiUrl}/api/auth/login`)
        .flush({ ...user, matricule: 'A1001', roles: ['ROLE_USER', 'ROLE_ADMIN_SITE'] });

      expect(service.isAdmin()).toBeTrue();
      expect(service.hasRole('ROLE_ADMIN_GLOBAL')).toBeFalse();
    });

    it('un simple membre n est pas administrateur', () => {
      service.login('G1042', 'Padel2026!').subscribe();
      http.expectOne(`${environment.apiUrl}/api/auth/login`).flush(user);

      expect(service.isAdmin()).toBeFalse();
    });
  });

  describe('logout', () => {
    it('vide la session', () => {
      service.login('G1042', 'Padel2026!').subscribe();
      http.expectOne(`${environment.apiUrl}/api/auth/login`).flush(user);

      service.logout();

      expect(service.isAuthenticated()).toBeFalse();
      expect(service.currentUser).toBeNull();
      expect(sessionStorage.getItem('padel.session')).toBeNull();
    });
  });
});
