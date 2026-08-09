# Tracabilite des consignes

Chaque exigence des consignes du projet, avec l'endroit du depot ou elle est traitee.
Les chemins sont relatifs a la racine `padel-manager/`.

## 1\. Consignes du projet de developpement web — requis

|Exigence|Ou c'est traite|Etat|
|-|-|-|
|Frontend Angular 20+|`frontend/`, Angular 20, standalone components|Fait|
|Backend Java 21 + Spring Boot 3.5+|`backend/pom.xml` (Boot 3.5.0, Java 21)|Fait|
|Depot Git unique contenant frontend et backend|Ce depot : `frontend/` et `backend/` cote a cote|Fait|
|Dossier d'architecture a la racine|`ARCHITECTURE.md`|Fait|
|Document d'exploitation a la racine|`EXPLOITATION.md`|Fait|
|Architecture decrite = architecture du code|`ARCHITECTURE.md` §2 et §3 decrivent l'arborescence reelle|Fait|
|Les parties compilent et demarrent sans erreur|`mvn clean install`, `npm start` — voir EXPLOITATION §3 et §4|A verifier chez toi|
|Authentification des utilisateurs|`auth/AuthController`, `JwtService`, `SecurityConfig`|Fait|
|Roles utilisateurs avec operations specifiques|`members/Role`, `SecurityConfig`, `CurrentMemberService.requireScopeOn`|Fait|
|Frontend communiquant avec le backend via HTTP|`frontend/src/app/core/services/\*.ts` (HttpClient)|Fait|
|Tests automatises pertinents (front + back)|7 classes de tests backend, 4 specs frontend, 3 fichiers Cypress|Fait|
|Jeu de donnees charge automatiquement au demarrage|`config/DataSeeder.java` (`ApplicationRunner`)|Fait|
|Donnees pertinentes, non obsoletes a l'evaluation|Dates relatives au jour du demarrage ; `PADEL\_SEED\_DATE` pour figer une reference|Fait|
|Volume de donnees pertinent|3 sites, 13 terrains, \~30 membres, une semaine de matchs sur les 3 sites|Fait|

## 2\. Consignes du projet de developpement web — bonus

|Exigence|Ou c'est traite|Etat|
|-|-|-|
|Application responsive|`@media` dans `styles.css` et chaque composant (1100 px et 900 px)|Fait|
|Interface agreable et simple|Planning en grille, codes couleur, dialogues de detail|Fait|
|Erreurs gerees proprement|`GlobalExceptionHandler` (backend), `ErrorMessageService` + interceptor (frontend)|Fait|
|Application performante|Index `ix\_match\_start`, requetes JPQL ciblees, lazy loading des routes |Fait|

## 3\. Guidelines — essentiel

### General

|Exigence|Ou c'est traite|
|-|-|
|Cas d'utilisation principaux sans bug en usage normal|Couverts par `MatchControllerIT` (happy flow) et les tests Cypress|
|Tests correctement ecrits, pattern 3A, isoles|`@Nested` + `@DisplayName` partout ; mocks Mockito ; H2 en memoire|

### Frontend

|Exigence|Ou c'est traite|
|-|-|
|Code bien structure, dossiers pertinents|`core/` (etat et donnees), `features/` (une par fonctionnalite), `shared/` (composants reutilises)|
|Composants Angular pour presenter l'information|`planning`, `my-matches`, `public-matches`, `admin-dashboard`|
|Composants pour eviter la duplication|`match-dialog`, `create-match-dialog`, `balance-card` reutilises par 3 ecrans|
|Composants pour un layout coherent|`app.component.ts` : barre de navigation + `router-outlet`|
|Services pour l'etat de l'application|`AuthService` : `BehaviorSubject` + `currentUser$`, `isAdmin$`, `isGlobalAdmin$`|
|Services pour gerer les donnees|`site`, `match`, `payment`, `member`, `stats` services|
|Experience utilisateur prise en compte|Etats de chargement, messages d'erreur cibles, raisons de blocage affichees avant l'action|
|Routing Angular dans la SPA|`app.routes.ts` avec `loadComponent` et guards|
|Application responsive|Media queries dans chaque composant|
|Formulaires valides|Reactive Forms : matricule contraint par regex, mot de passe requis, `FormArray` des invites|
|Tests unitaires sur composants et services cles|`auth.service.spec.ts`, `error-message.service.spec.ts`, specs des deux pipes|
|Tests Cypress sur les cas principaux|`login.cy.ts`, `booking.cy.ts`, `admin.cy.ts`|

### HTTP

|Exigence|Ou c'est traite|
|-|-|
|OpenAPI genere|`config/OpenApiConfig.java`, springdoc — `/v3/api-docs`, `/swagger-ui.html`|
|CORS correct|`SecurityConfig.corsConfigurationSource` : origine, methodes et en-tetes explicites|
|Nommage des chemins coherent|Pluriel, minuscules — voir `ARCHITECTURE.md` §8|
|Usage coherent des parametres|Identifiants en `path`, filtres en `query`, donnees en `body`|
|Types de retour coherents|Un DTO par ressource, jamais d'entite JPA exposee|
|Methodes HTTP correctes|`GET` lecture, `POST` creation et actions metier|
|Status codes pertinents|200 / 201 / 400 / 401 / 403 / 404 / 409 / 500 — `GlobalExceptionHandler`|

### Backend

|Exigence|Ou c'est traite|
|-|-|
|Code bien structure, dossiers pertinents|Organisation par feature : `auth`, `members`, `sites`, `matches`, `payments`, `stats`|
|Architecture adequate et respectee|Presentation → service → repository, injection par constructeur|
|Tests unitaires sur les cas principaux|`MatchServiceTest`, `MatchTransitionServiceTest`, `PaymentServiceTest`, `MemberTypeTest`, `MatchBookingTest`|
|Tests d'integration sur les cas principaux|`AuthControllerIT`, `MatchControllerIT`, `MemberRepositoryIT`, `SiteClosureRepositoryIT`|

### Securite

|Exigence|Ou c'est traite|
|-|-|
|Mots de passe stockes de maniere securisee|BCrypt (`SecurityConfig.passwordEncoder`) ; `password\_hash` jamais renvoye|
|Une seule authentification par session|JWT en `sessionStorage`, restaure au rechargement (`AuthService.restore`)|
|JWT correctement genere, valide, stocke|`JwtService` (HS256, signature verifiee), `JwtAuthenticationFilter`|
|JWT ne contenant que le necessaire|Claims : `sub`, `mid`, `roles`, `typ` — aucune donnee personnelle|
|Roles definis, attribues, verifies|`Role`, `DataSeeder.seedAdmins`, `SecurityConfig`, `CurrentMemberService.requireScopeOn`|
|Un service Angular gere l'authentification|`core/auth/auth.service.ts`|
|Une guard Angular verifie l'acces aux routes|`core/auth/auth.guard.ts` : `authGuard`, `adminGuard`|
|Un interceptor ajoute le JWT|`core/auth/jwt.interceptor.ts`|

## 4\. Guidelines — bonus

|Exigence|Ou c'est traite|Etat|
|-|-|-|
|Cas limites geres sans bug|Match complet, doublon d'inscription, date passee, creneau non aligne, fermetures, penalite expiree — tous testes|Fait|
|Nommage coherent|Vocabulaire du domaine partout : `MatchBooking`, `MatchParticipation`, `balanceDue`, `bannedUntil`|Fait|
|Methodes courtes, responsabilite unique|Validations decoupees : `validateOrganizerCanBook`, `validateSiteOpenAt`, `validateCourtAvailability`|Fait|
|Observables utilises elegamment|`forkJoin` (chargement parallele), `map` sur `currentUser$`, `catchError` dans l'interceptor|Fait|
|Client HTTP utilise dans le frontend|`HttpClient` dans les six services de `core/services`|Fait|
|Pipes Angular, dont des pipes personnalises|`DatePipe`, `DecimalPipe`, plus `memberCategory` et `slotTime`|Fait|
|package.json propre|Uniquement Angular, RxJS, TypeScript, Jasmine/Karma, Cypress|Fait|
|Erreurs gerees proprement, via RxJS|`catchError` dans l'interceptor, `ErrorMessageService`, messages affiches par ecran|Fait|
|Validations complexes de formulaires|Regex du matricule, `FormArray` d'invites limite a 3, raison de blocage calculee avant soumission|Fait|
|Tests structures avec `@Nested`|Toutes les classes de tests backend|Fait|
|pom.xml propre|Uniquement les dependances utilisees ; H2 en scope `test`|Fait|
|Gestion globale des exceptions|`GlobalExceptionHandler` (`@RestControllerAdvice`, 8 cas)|Fait|
|Base de donnees utilisee|MySQL 8.4 via Docker|Fait|
|JPA utilise|Entites, repositories Spring Data, requetes JPQL|Fait|
|Transactions correctement gerees|`@Transactional` sur les services, `readOnly` sur les lectures|Fait|
|Tests d'integration sur les repositories|`MemberRepositoryIT`, `SiteClosureRepositoryIT`|Fait|
|Politique de mot de passe fort|`Validators.minLength(6)` cote frontend|Partiel — pas de politique complete cote backend|
|L'utilisateur peut se deconnecter|Bouton *Quitter*, `AuthService.logout()`|Fait cote client|
|Mecanisme de refresh du JWT|`JwtService` (token `typ=refresh`), `POST /api/auth/refresh`, `AuthService.refresh()`|Fait|
|Gestion des migrations|Absent — schema cree une fois par le script d'init Docker|**Non fait**, voir ARCHITECTURE §11|

## 5\. Cahier des charges padel (SGBD)

|Exigence|Ou c'est traite|
|-|-|
|Plusieurs sites, nombre de terrains different par site|`Site`, `Court` ; seed : Uccle 4, LLN 3, Namur 6|
|Horaires propres a chaque site|`Site.openingTime/closingTime` ; grille calculee par `MatchService.slotsOf`|
|Reservations de 1h30 avec 15 min entre les matchs|`MatchBooking.DURATION`, `BREAK`, `SLOT\_STEP` (105 min)|
|Debut et fin de reservation specifiques au site|`validateSiteOpenAt` : refus avant l'ouverture et si la fin depasse la fermeture|
|Jours de fermeture par site et globaux|`SiteClosure` (`site\_id` NULL = reseau), `countClosuresOn`|
|Matchs prives ou publics|`MatchVisibility`|
|Prive : 4 joueurs obligatoires|`REQUIRED\_PLAYERS`, refus au-dela dans `addInvitedPlayers`|
|Prive incomplet la veille : devient public|`MatchTransitionService.applyTransitions` (fenetre 24 h)|
|Penalite d'une semaine a l'organisateur responsable|`Member.applyOneWeekPenalty`, verifiee par `hasActivePenalty`|
|L'organisateur d'un prive ajoute lui-meme les joueurs|`CreateMatchRequest.privatePlayersMatricules`|
|Un match public est visible par tous les membres|`GET /api/matches/public`, ecran *Matchs publics*|
|60 € par match, divises en 4, payes a l'avance|`PRICE`, `SHARE`, `PaymentService.payShare`|
|Non paye la veille : match public et place liberee|`releaseUnpaidPlaces` dans `applyTransitions`|
|Public : validation du joueur des le paiement|`joinPublicMatch` appelle `payShare` dans la meme transaction|
|Public : l'organisateur ne reserve pas pour autrui|Refus explicite dans `createMatch`|
|Premier paye, premier servi|Refus si le match est complet ; place validee au paiement|
|Solde a charge de l'organisateur si le match n'est pas rempli|`outstandingAmount()` ajoute au `balanceDue` a l'heure du match|
|Pas de reservation tant qu'un solde est du|`owesBalance()` dans `validateOrganizerCanBook`|
|Solde ajoute au prochain paiement du joueur|`PaymentService.payShare` : part + solde en un seul versement|
|Membre global Gxxxx, 3 semaines, tous les sites|`MemberType.GLOBAL` (21 jours, `canBookOnAnySite`)|
|Membre de site Sxxxxx, 2 semaines, son site|`MemberType.SITE` (14 jours), verification du `homeSite`|
|Membre libre Lxxxxx, 5 jours, tous les sites|`MemberType.FREE` (5 jours)|
|Membre de site visible partout, reservable sur son site|`MemberRepository.findVisibleFromSite`|
|Participants obligatoirement dans une des 3 categories|`CHECK ck\_member\_type` en base + enum `MemberType`|
|Interface utilisateur : ses reservations, les matchs publics|Ecrans *Planning*, *Mes matchs*, *Matchs publics*|
|Interface admin : etat des matchs et terrains, CA, membres, stats|Ecran *Administration* + `StatsService`|
|Deux types d'admin : global et par site|`ROLE\_ADMIN\_GLOBAL`, `ROLE\_ADMIN\_SITE`, `requireScopeOn`|
|Admin global : voit et gere tous les sites|`GET /api/stats/global`, bascule de portee dans l'interface|
|Admin de site : ne gere que son site|403 si `adminSite` ne correspond pas au site demande|
|Separation frontend / backend, API REST obligatoire|Deux applications distinctes, JSON sur HTTP|
|Couches minimales backend + injection de dependances|Repositories, services, modeles ; injection par constructeur|
|DB relationnelle avec un maximum de relations|8 tables, 11 cles etrangeres — `ARCHITECTURE.md` §6|
|Priorite : reservations, paiements, statistiques|Les trois sont les fonctionnalites les plus developpees et les plus testees|
|Users DB a droits specifiques, pas de user tout-puissant|`docker/mysql/init/01-users.sql` : `padel\_app` (DML), `padel\_readonly` (SELECT), `padel\_ddl` (DDL)|
|Savoir justifier l'acces direct aux tables ou non|`ARCHITECTURE.md` §5 : decision et consequences assumees|
|Git obligatoire avec des issues|Le depot est fourni ; **les issues restent a creer de ton cote**|



