import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/auth/auth.service';
import { SiteService } from '../../../core/services/site.service';
import { AuthUser, Site } from '../../../core/models/models';

/**
 * Tests unitaires du composant de connexion : validation du formulaire, indication
 * de categorie deduite du matricule, et traitement de la reponse du backend.
 * Les services HTTP sont remplaces par des doublures : on teste le composant seul.
 */
describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let authSpy: jasmine.SpyObj<AuthService>;
  let siteSpy: jasmine.SpyObj<SiteService>;
  let http: HttpTestingController;

  const sites: Site[] = [
    {
      id: 1,
      name: 'Padel Uccle',
      address: 'Avenue Brugmann 210, 1180 Uccle',
      openingTime: '08:00',
      closingTime: '22:00',
      courts: [{ id: 1, number: 1 }],
    },
    {
      id: 2,
      name: 'Padel Namur',
      address: 'Chaussee de Dinant 88, 5000 Namur',
      openingTime: '07:30',
      closingTime: '22:30',
      courts: [{ id: 2, number: 1 }],
    },
  ];

  const authenticated: AuthUser = {
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

  beforeEach(async () => {
    sessionStorage.clear();

    authSpy = jasmine.createSpyObj<AuthService>('AuthService', ['login']);
    siteSpy = jasmine.createSpyObj<SiteService>('SiteService', ['getSites']);
    siteSpy.getSites.and.returnValue(of(sites));

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: authSpy },
        { provide: SiteService, useValue: siteSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('charge les sites et preselectionne le premier', () => {
    expect(siteSpy.getSites).toHaveBeenCalledTimes(1);
    expect(component.sites.length).toBe(2);
    expect(component.selectedSiteId).toBe(1);
  });

  it('refuse un matricule au format invalide', () => {
    component.form.controls.matricule.setValue('X99');
    component.form.controls.password.setValue('Padel2026!');

    expect(component.form.controls.matricule.invalid).toBeTrue();
    expect(component.form.invalid).toBeTrue();
  });

  it('accepte les trois formats de matricule du cahier des charges', () => {
    for (const matricule of ['G1042', 'S12008', 'L7731']) {
      component.form.controls.matricule.setValue(matricule);
      expect(component.form.controls.matricule.valid)
        .withContext(`${matricule} devrait etre accepte`)
        .toBeTrue();
    }
  });

  it('deduit la categorie et le delai de reservation de la premiere lettre', () => {
    component.form.controls.matricule.setValue('S12008');
    expect(component.matriculeHint).toContain('14 jours');

    component.form.controls.matricule.setValue('L7731');
    expect(component.matriculeHint).toContain('5 jours');
  });

  it('met le matricule en majuscules a la saisie', () => {
    component.form.controls.matricule.setValue('g1042');
    component.onMatriculeInput();

    expect(component.form.controls.matricule.value).toBe('G1042');
  });

  it("n'appelle pas le backend quand le formulaire est invalide", () => {
    component.form.controls.matricule.setValue('');
    component.form.controls.password.setValue('');

    component.submit();

    expect(authSpy.login).not.toHaveBeenCalled();
    expect(component.form.controls.matricule.touched).toBeTrue();
  });

  it('memorise le site choisi et redirige apres une connexion reussie', () => {
    const router = TestBed.inject(Router);
    const navigate = spyOn(router, 'navigateByUrl').and.resolveTo(true);
    authSpy.login.and.returnValue(of(authenticated));

    component.selectSite(2);
    component.form.controls.matricule.setValue('G1042');
    component.form.controls.password.setValue('Padel2026!');
    component.submit();

    expect(authSpy.login).toHaveBeenCalledWith('G1042', 'Padel2026!');
    expect(sessionStorage.getItem('padel.site')).toBe('2');
    expect(navigate).toHaveBeenCalledWith('/planning');
  });

  it('affiche un message lisible quand le backend refuse les identifiants', () => {
    authSpy.login.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' })),
    );

    component.form.controls.matricule.setValue('G1042');
    component.form.controls.password.setValue('mauvais');
    component.submit();

    expect(component.serverError).toBe('Identifiants invalides.');
    expect(component.loading).toBeFalse();
  });
});
