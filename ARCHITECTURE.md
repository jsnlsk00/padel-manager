# Dossier d'architecture — Padel Manager

Application de gestion de terrains de padel multi-sites. Le projet couvre les exigences
du cours de **developpement web** (architecture en couches, securite, tests) et celles du
cours de **SGBD** (modele relationnel, regles metier, separation des droits en base).

## 1. Vue d'ensemble

```
┌──────────────────────┐      HTTP / JSON       ┌───────────────────────┐
│  Frontend Angular 20 │ ◄────────────────────► │  Backend Spring Boot  │
│  SPA, port 4200      │      JWT Bearer         │  API REST, port 8080  │
└──────────────────────┘                         └───────────┬───────────┘
                                                             │ JPA / Hibernate
                                                             │ user padel_app (DML seul)
                                                             ▼
                                                 ┌───────────────────────┐
                                                 │  MySQL 8.4 (Docker)   │
                                                 │  schema cree a l'init │
                                                 └───────────────────────┘
```

Le frontend ne contient **aucune regle metier** : il affiche ce que l'API calcule et
relaie les intentions de l'utilisateur. L'API REST est l'unique point d'entree.

## 2. Architecture backend

Architecture **en couches**, organisee **par feature** plutot que par type technique :
chaque dossier contient sa presentation, son service, son acces aux donnees et ses DTO.

```
src/main/java/be/ephec/padel/
├── PadelApplication.java              point d'entree, @EnableScheduling
│
├── config/                            infrastructure transversale
│   ├── SecurityConfig.java              chaine de securite, CORS, autorisations
│   ├── JwtAuthenticationFilter.java     lecture du Bearer token
│   ├── OpenApiConfig.java               specification OpenAPI
│   └── DataSeeder.java                  jeu de donnees automatique
│
├── common/
│   ├── dto/ApiError.java              corps d'erreur uniforme
│   └── exceptions/                    BusinessException, ResourceNotFound,
│                                      ForbiddenOperation, GlobalExceptionHandler
│
├── auth/                              authentification
│   ├── AuthController.java              POST /login, POST /refresh
│   ├── JwtService.java                  generation et validation HS256
│   ├── MemberUserDetailsService.java    chargement du membre
│   ├── PadelUserDetails.java            principal Spring Security
│   └── CurrentMemberService.java        membre courant + portee des admins
│
├── members/                           Member, MemberType (G/S/L), Role,
│                                      MemberRepository, MemberService, MemberController
│
├── sites/                             Site, Court, SiteClosure, leurs repositories,
│                                      SiteService (dont le calcul du planning),
│                                      SiteController
│
├── matches/                           coeur metier
│   ├── MatchBooking.java                entite + invariants (duree, parts, places)
│   ├── MatchParticipation.java
│   ├── MatchService.java                regles R1 a R8, R10, R11
│   ├── MatchTransitionService.java      regle R9, testable sans scheduler
│   ├── MatchScheduler.java              declencheur horaire
│   └── MatchRepository, MatchController, dto/
│
├── payments/                          Payment, PaymentService (parts et soldes),
│                                      PaymentController
│
└── stats/                             StatsService, StatsController, dto/
```

### Responsabilite des couches

| Couche | Composants | Responsabilite |
|---|---|---|
| Presentation | `*Controller` | Traduction HTTP : validation des DTO, statuts, serialisation. Aucune regle. |
| Service | `*Service` | Regles metier, orchestration, transactions (`@Transactional`). |
| Acces donnees | `*Repository` | Spring Data JPA, requetes JPQL nommees. |
| Domaine | entites | Invariants proches de la donnee (duree d'un match, calcul du solde, liberation des places). |
| Infrastructure | `config/`, `common/` | Securite, exceptions, OpenAPI, seeding. |

Le sens des dependances est unique : presentation → service → repository. L'injection se
fait **par constructeur**, ce qui rend chaque service testable avec des mocks.

### Choix : les invariants dans l'entite

`MatchBooking` porte la duree du match, la part de 15 €, le calcul du solde restant et
la liberation des places non payees. Les regles qui exigent de consulter d'autres
agregats (fenetre de reservation, fermetures, double reservation) vivent dans
`MatchService`. L'entite reste donc coherente par construction, sans devenir un service.

## 3. Architecture frontend (Angular 20)

```
src/app/
├── app.component.ts          layout : barre de navigation + router-outlet
├── app.config.ts             providers : router, HttpClient, interceptor
├── app.routes.ts             routes avec lazy loading et guards
│
├── core/                     singletons : etat et acces aux donnees
│   ├── auth/
│   │   ├── auth.service.ts     etat d'authentification (BehaviorSubject)
│   │   ├── auth.guard.ts       authGuard, adminGuard
│   │   └── jwt.interceptor.ts  ajout du Bearer, deconnexion sur 401
│   ├── models/models.ts        types partages, alignes sur les DTO du backend
│   ├── pipes/                  memberCategory, slotTime (pipes personnalises)
│   └── services/               site, match, payment, member, stats, error-message
│
├── features/                 une feature = un dossier, charge a la demande
│   ├── auth/login/
│   ├── user/planning/          grille terrains x creneaux
│   ├── user/public-matches/    matchs publics filtres
│   ├── user/my-matches/
│   └── admin/dashboard/        statistiques, portee site ou reseau
│
└── shared/components/        match-dialog, create-match-dialog, balance-card
```

### Concepts Angular utilises

- **Standalone components** partout, sans NgModule.
- **Etat applicatif dans un service** : `AuthService` expose `currentUser$`, `isAdmin$`
  et `isGlobalAdmin$` (BehaviorSubject + `map`), consommes dans les templates par `async`.
- **Routing** avec `loadComponent` (lazy loading) et deux guards fonctionnels.
- **Interceptor fonctionnel** (`HttpInterceptorFn`) pour le JWT et la gestion des 401.
- **Reactive Forms** avec validation : matricule contraint par une expression reguliere,
  mot de passe obligatoire, `FormArray` pour les invites d'un match prive.
- **Pipes personnalises** : `memberCategory` (categorie + fenetre), `slotTime`
  (creneau "18:30 – 20:00" en ajoutant 1h30).
- **RxJS** : `forkJoin` pour charger le planning et le profil en parallele,
  `catchError` dans l'interceptor.
- **Responsive** : grilles a une colonne sous 1100 px, navigation compactee sous 900 px.

### Gestion des erreurs cote client

`ErrorMessageService` traduit chaque `HttpErrorResponse` en message affichable : les
regles metier arrivent en 409 avec un texte deja explicite venant du backend, les erreurs
de validation en 400 avec le detail par champ, le reste est neutralise. Aucune trace
technique n'atteint l'utilisateur.

## 4. Securite applicative

| Aspect | Implementation |
|---|---|
| Mots de passe | BCrypt (`BCryptPasswordEncoder`), jamais en clair, jamais renvoyes par l'API |
| Authentification | Matricule + mot de passe → JWT ; une seule authentification par session |
| JWT | HS256, claims minimaux (`sub`, `mid`, `roles`, `typ`), expiration 60 min |
| Refresh | Token distinct (`typ=refresh`, 480 min), echange via `POST /api/auth/refresh` |
| Stockage cote client | `sessionStorage` : le token disparait a la fermeture de l'onglet |
| Autorisation | Centralisee dans `SecurityConfig` ; portee des admins verifiee dans `CurrentMemberService` |
| Message de login | 401 generique : "matricule inconnu" et "mauvais mot de passe" sont indistinguables |
| CORS | Origine unique autorisee, methodes et en-tetes explicites |
| Session serveur | `STATELESS` : aucune session, donc pas de CSRF a proteger |
| Deconnexion | Cote client (suppression du token) ; le JWT expire de lui-meme |

### Roles

| Role | Portee |
|---|---|
| `ROLE_USER` | Reserver, rejoindre, payer, consulter ses matchs |
| `ROLE_ADMIN_SITE` | Statistiques et fermetures **de son site uniquement** |
| `ROLE_ADMIN_GLOBAL` | Tous les sites |

Un admin de site qui appelle `/api/stats/site/{autreSite}` recoit un 403 : la
verification ne repose pas sur l'URL mais sur le `adminSite` du membre authentifie.

## 5. Securite de la base de donnees (SGBD)

Trois users MySQL crees par `docker/mysql/init/01-users.sql` :

| User | Droits | Usage |
|---|---|---|
| `padel_app` | `SELECT, INSERT, UPDATE, DELETE` | Le **seul** utilise par l'application |
| `padel_readonly` | `SELECT` | Rapports, exports |
| `padel_ddl` | `CREATE, ALTER, DROP, INDEX, REFERENCES` | Migrations, hors execution |

L'application tourne avec `hibernate.ddl-auto: none` : elle ne peut pas modifier le
schema, meme en cas de bug ou de mauvaise annotation JPA.

### Choix : acces direct aux tables, sans vues ni procedures

**Decision** — `padel_app` accede directement aux tables.

**Consequences assumees.** Le code applicatif connait la structure du schema : un
changement de colonne se repercute sur les entites JPA. Il n'existe pas de couche
d'abstraction en base pour absorber une refonte du modele. En contrepartie, JPA reste
utilisable de maniere idiomatique (les procedures stockees s'y integrent mal), les
requetes sont visibles et optimisables depuis le code, et la coherence est garantie par
la couche service, qui est testee. Le risque residuel — une ecriture incorrecte par
l'application — est limite par les contraintes declarees en base (`CHECK`, `UNIQUE`,
cles etrangeres), qui rejettent une donnee invalide meme si le service se trompe.

Une alternative aurait ete d'exposer des vues en lecture et des procedures en ecriture :
plus protecteur du schema, mais au prix d'une logique metier dupliquee en SQL, non
testable avec les outils du projet.

## 6. Modele relationnel

```
SITE                    COURT                   SITE_CLOSURE
 id (PK)                 id (PK)                 id (PK)
 name (UQ)               number                  closed_on
 address                 site_id (FK) ──┐        reason
 opening_time                            │       site_id (FK, NULL = reseau)
 closing_time                            │
 CHECK closing>opening   UQ (site_id, number)

MEMBER                                   MATCH_BOOKING
 id (PK)                                  id (PK)
 matricule (UQ)                           court_id (FK) ─────┐
 first_name, last_name                    organizer_id (FK) ─┼──► MEMBER
 email (UQ)                               start_time         │
 password_hash                            end_time           │
 type (CHECK G/S/L)                       visibility (CHECK) │
 home_site_id (FK) ───────────────────┐   status (CHECK)      │
 admin_site_id (FK) ──────────────────┤   price              │
 balance_due (CHECK >= 0)             │   UQ (court_id, start_time)
 banned_until                         │            ▲
                                      │            │ 1..n
MEMBER_ROLE                           │   MATCH_PARTICIPATION
 member_id (PK, FK) ──► MEMBER        │    id (PK)
 role (PK, CHECK)                     │    match_id (FK) ────┘
                                      │    player_id (FK) ──► MEMBER
PAYMENT                               │    paid
 id (PK)                              │    UQ (match_id, player_id)
 match_id (FK, NULL si solde seul)    │
 payer_id (FK) ──► MEMBER ────────────┘
 amount (CHECK > 0)
 paid_at
 is_balance_payment
```

### Relations

| Relation | Cardinalite | Role |
|---|---|---|
| Site — Court | 1..n | Chaque site a son propre nombre de terrains |
| Site — SiteClosure | 1..n | Fermetures ; `site_id` NULL = fermeture du reseau |
| Site — Member (home_site) | 1..n | Rattachement d'un membre de site |
| Site — Member (admin_site) | 1..n | Site administre |
| Member — Role | 1..n | `@ElementCollection` sur `member_role` |
| Court — MatchBooking | 1..n | Reservations d'un terrain |
| Member — MatchBooking (organizer) | 1..n | Organisateur responsable du match |
| MatchBooking — MatchParticipation — Member | n..n | Les 4 joueurs, avec leur etat de paiement |
| MatchBooking — Payment | 1..n | Versements rattaches au match |
| Member — Payment | 1..n | Historique des paiements |

### Contraintes qui portent des regles metier

| Contrainte | Regle protegee |
|---|---|
| `UQ (court_id, start_time)` sur `match_booking` | Pas de double reservation, meme en cas de requetes concurrentes |
| `UQ (match_id, player_id)` | Un joueur ne peut pas occuper deux places du meme match |
| `CHECK balance_due >= 0` | Un solde ne peut pas devenir negatif |
| `CHECK amount > 0` | Pas de paiement nul ou negatif |
| `CHECK closing_time > opening_time` | Horaires de site coherents |
| `CHECK end_time > start_time` | Periode de match coherente |
| `INDEX ix_match_start` | Performance du planning et du job de bascule |

## 7. Regles metier

| # | Regle | Implementation |
|---|---|---|
| R1 | Match de 1h30, 15 min de battement, grille au pas de 1h45 propre a chaque site | `MatchBooking.DURATION/BREAK/SLOT_STEP`, `MatchService.validateSiteOpenAt`, `slotsOf` |
| R2 | Fenetre de reservation : G 21 j, S 14 j, L 5 j | `MemberType`, `MatchService.validateOrganizerCanBook` |
| R3 | Aucune reservation pendant une penalite d'une semaine | `Member.hasActivePenalty`, `validateOrganizerCanBook` |
| R4 | Aucune reservation si un solde est du | `Member.owesBalance`, `validateOrganizerCanBook` |
| R5 | Jours de fermeture, par site ou globaux | `SiteClosureRepository.countClosuresOn` |
| R6 | Creneau dans les horaires du site | `validateSiteOpenAt` |
| R7 | Pas de double reservation d'un terrain | `validateCourtAvailability` + contrainte d'unicite en base |
| R8 | Membre de site limite a son site de rattachement | `MemberType.canBookOnAnySite`, `validateOrganizerCanBook` |
| R9 | Prive incomplet a J-1 : places non payees liberees, bascule en public, penalite d'une semaine a l'organisateur | `MatchTransitionService.applyTransitions`, declenche chaque heure par `MatchScheduler` |
| R10 | Match public : l'organisateur n'inscrit personne, chacun paie sa place ; premier paye, premier servi | `MatchService.createMatch` (refus des invites) et `joinPublicMatch` |
| R11 | 60 € par match, 15 € par joueur, payes a l'avance ; solde a charge de l'organisateur, encaisse a son prochain paiement | `MatchBooking.SHARE/outstandingAmount`, `PaymentService.payShare/paySolde`, `MatchTransitionService` |

### Detail de R9

Le job horaire (`MatchScheduler`) appelle `MatchTransitionService.applyTransitions(now)`,
qui traite deux fenetres :

1. **matchs commencant dans les 24 h** — les participations non payees sont supprimees
   (la place redevient reservable) ; si le match est prive et n'a plus ses 4 joueurs, il
   passe en `PUBLIC` et son organisateur recoit `bannedUntil = aujourd'hui + 1 semaine` ;
2. **matchs deja commences** — si moins de 4 parts sont payees, le solde manquant
   (`(4 - payes) x 15 €`) est ajoute au `balance_due` de l'organisateur ; le match passe
   en `CONFIRMED`, puis en `PLAYED` une fois termine.

L'heure est un **parametre de methode** et non `LocalDateTime.now()` interne : la regle
est testee sans attendre le declenchement du scheduler
(`MatchTransitionServiceTest`, 6 scenarios).

## 8. API HTTP

Swagger UI : <http://localhost:8080/swagger-ui.html> · Spec : `/v3/api-docs`

| Methode | Chemin | Acces | Description |
|---|---|---|---|
| POST | `/api/auth/login` | public | Authentification, retourne le JWT et le profil |
| POST | `/api/auth/refresh` | public | Echange un refresh token contre un JWT d'acces |
| GET | `/api/sites` | public | Sites et terrains |
| GET | `/api/sites/{id}` | public | Detail d'un site |
| GET | `/api/sites/{id}/closures` | public | Fermetures applicables |
| GET | `/api/sites/{id}/planning?day=` | public | Grille terrains x creneaux d'une journee |
| POST | `/api/sites/{id}/closures` | admin du site | Declare un jour de fermeture |
| GET | `/api/members/me` | membre | Profil courant (solde, penalite, fenetre) |
| GET | `/api/members` | admin | Membres visibles dans la portee |
| GET | `/api/members/{matricule}` | membre | Recherche d'un membre |
| GET | `/api/matches/public?siteId=` | membre | Matchs publics encore ouverts |
| GET | `/api/matches/me` | membre | Mes matchs |
| GET | `/api/matches/{id}` | membre | Detail d'un match |
| POST | `/api/matches` | membre | Cree un match prive ou public |
| POST | `/api/matches/{id}/join` | membre | Rejoint un match public (valide au paiement) |
| POST | `/api/payments/match/{id}` | membre | Paie sa part, solde du inclus |
| POST | `/api/payments/balance` | membre | Regle un solde seul |
| GET | `/api/payments/me` | membre | Historique des paiements |
| GET | `/api/stats/site/{id}` | admin du site | Statistiques d'un site |
| GET | `/api/stats/global` | admin global | Statistiques du reseau |

### Conventions

- Chemins au pluriel, en minuscules ; identifiants en `path`, filtres en `query`.
- `GET` pour lire, `POST` pour creer ou declencher une action metier.
- Statuts : `200` lecture, `201` creation, `400` validation, `401` non authentifie,
  `403` role ou portee insuffisants, `404` ressource inexistante,
  `409` regle metier violee, `500` erreur interne.
- Toute erreur renvoie le meme corps `ApiError` (statut, message, chemin, horodatage,
  et `fieldErrors` sur une erreur de validation).

## 9. Bibliotheques structurantes

### Backend

| Bibliotheque | Version | Role |
|---|---|---|
| Spring Boot | 3.5.0 | Socle applicatif, injection de dependances |
| Spring Web | 6.x | API REST |
| Spring Data JPA | 3.x | Repositories, Hibernate |
| Spring Security | 6.x | Authentification, autorisation |
| jjwt | 0.12.6 | JWT HS256 |
| mysql-connector-j | gere par Boot | Pilote MySQL |
| springdoc-openapi | 2.6.0 | OpenAPI et Swagger UI |
| JUnit 5, Mockito, AssertJ | via spring-boot-starter-test | Tests |
| H2 | scope test | Base en memoire pour les tests |

### Frontend

| Bibliotheque | Version | Role |
|---|---|---|
| Angular | 20.x | SPA, routing, formulaires |
| RxJS | 7.8 | Flux et etat reactif |
| TypeScript | 5.8 | Langage, mode strict |
| Jasmine + Karma | 5.x | Tests unitaires |
| Cypress | 13.x | Tests end-to-end |

Les dependances sont volontairement limitees : ni bibliotheque de composants, ni
utilitaire CSS. Le style tient dans `styles.css` (les tokens) et dans les styles de
chaque composant.

## 10. Tests

| Niveau | Outil | Fichiers | Ce qui est couvert |
|---|---|---|---|
| Unitaire domaine | JUnit 5 | `MemberTypeTest`, `MatchBookingTest` | Fenetres de reservation, duree, parts, liberation des places |
| Unitaire service | JUnit 5 + Mockito | `MatchServiceTest` | R1 a R8, R10 : 20 scenarios de reservation et d'inscription |
| Unitaire service | JUnit 5 + Mockito | `MatchTransitionServiceTest` | R9 : bascule, penalite, liberation, imputation du solde |
| Unitaire service | JUnit 5 + Mockito | `PaymentServiceTest` | R11 : part de 15 €, solde ajoute au paiement, refus des doublons |
| Integration DAL | `@DataJpaTest` | `MemberRepositoryIT`, `SiteClosureRepositoryIT` | Unicite du matricule, visibilite inter-sites, fermetures site et globales |
| Integration API | `@SpringBootTest` + MockMvc | `AuthControllerIT` | Login, 401 generique, protection des routes, refus des stats a un membre |
| Integration API | `@SpringBootTest` + MockMvc | `MatchControllerIT` | Parcours complet : creation, paiement, inscription, planning, conflits |
| Unitaire frontend | Jasmine | `auth.service.spec.ts`, pipes, `error-message.service.spec.ts` | Etat d'authentification, roles, pipes, traduction des erreurs |
| E2E | Cypress | `login.cy.ts`, `booking.cy.ts`, `admin.cy.ts` | Connexion, planning, detail d'un match, creation, portees admin |

Les tests backend suivent le pattern **3A** (Arrange / Act / Assert), avec `@Nested` et
`@DisplayName` pour que le rapport se lise comme une specification. Ils sont isoles :
mocks pour les services, H2 en memoire pour les tests d'integration, aucune dependance a
Docker ni a l'ordre d'execution.

## 11. Limites connues

- **Inscription** : pas d'endpoint public de creation de compte. Les membres viennent du
  `DataSeeder`. En production il faudrait un endpoint d'inscription et une interface
  d'administration des membres.
- **Annulation d'un match** : le statut `CANCELLED` existe dans le modele mais aucun
  endpoint ne l'utilise.
- **Deconnexion cote serveur** : le JWT n'est pas revoque, il expire. Une liste de
  revocation serait necessaire pour une deconnexion immediate.
- **Migrations** : le schema est cree une fois par le script d'init du conteneur. Un
  outil de migration (Flyway, Liquibase) serait le prolongement naturel pour faire
  evoluer le schema sans repartir d'un volume vide.
