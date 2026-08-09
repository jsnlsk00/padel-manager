# Padel Manager

Gestion de terrains de padel multi-sites : reservation sur un planning terrains x creneaux,
matchs prives et publics, paiements par parts, penalites, et tableau de bord
d'administration par site ou pour l'ensemble du reseau.

Depot unique contenant le frontend, le backend et les documents du projet.

## Demarrage rapide

```bash
docker compose up -d                        # MySQL : schema et users crees automatiquement
cd backend  && ./mvnw spring-boot:run          # API sur :8080, jeu de donnees charge au demarrage
cd frontend && npm install && npm start     # SPA sur :4200
```

Se connecter sur <http://localhost:4200> avec **`G1042`** / **`Padel2026!`**.

Le detail complet des commandes, des variables d'environnement et du depannage est dans
[EXPLOITATION.md](EXPLOITATION.md).

## Structure du depot

```
padel-manager/
├── ARCHITECTURE.md      dossier d'architecture (couches, modele, regles, securite)
├── EXPLOITATION.md      compilation, demarrage, tests, comptes de demonstration
├── TRACABILITE.md       chaque consigne du projet et l'endroit ou elle est traitee
├── docker-compose.yml   MySQL 8.4
├── docker/mysql/init/   users a droits restreints + schema relationnel
├── backend/             Spring Boot 3.5, Java 21, API REST
└── frontend/            Angular 20, SPA standalone
```

## Ce que fait l'application

**Cote joueur** — planning d'une journee par site (grille terrains x creneaux de 1h30 avec
15 min de battement, horaires propres a chaque site, jours de fermeture), creation d'un
match prive ou public, inscription a un match public, paiement de sa part de 15 €,
reglement d'un solde du, historique de ses matchs.

**Cote administration** — chiffre d'affaires, taux d'occupation, part de matchs publics,
impayes et penalites, creneaux les plus demandes, membres par categorie, fermetures. Un
administrateur de site ne voit que son site ; un administrateur global bascule entre les
deux portees.

## Regles metier appliquees

| Regle | Comportement |
|---|---|
| Fenetre de reservation | Membre global 21 jours, membre de site 14 jours, membre libre 5 jours |
| Portee | Un membre de site ne reserve que sur son site de rattachement |
| Match prive | 4 joueurs obligatoires, ajoutes par l'organisateur |
| Bascule a J-1 | Un prive incomplet devient public, les places non payees sont liberees, l'organisateur ecope d'une semaine de delai |
| Match public | Chacun s'inscrit et paie lui-meme : premier paye, premier servi |
| Prix | 60 € par match, 15 € par joueur, payes a l'avance |
| Solde | Le manquant est impute a l'organisateur et bloque ses reservations jusqu'a reglement |

## Tests

```bash
cd backend  && ./mvnw test        # unitaires + integration (H2, Docker inutile)
cd frontend && npm run test:ci # Jasmine / Karma
cd frontend && npm run e2e     # Cypress (backend et frontend demarres)
```

## API

Application demarree : <http://localhost:8080/swagger-ui.html>
