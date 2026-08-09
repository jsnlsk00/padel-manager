/// <reference types="cypress" />

describe('Connexion par matricule', () => {
  beforeEach(() => {
    cy.visit('/login');
  });

  it('affiche les trois categories de membres', () => {
    cy.contains('Gxxxx').should('be.visible');
    cy.contains('Sxxxxx').should('be.visible');
    cy.contains('Lxxxxx').should('be.visible');
    cy.contains('21 jours').should('be.visible');
  });

  it('deduit la categorie du prefixe du matricule', () => {
    cy.get('[data-cy=matricule]').type('L7731');
    cy.contains('Membre libre').should('be.visible');
  });

  it('refuse un matricule au mauvais format', () => {
    cy.get('[data-cy=matricule]').type('X1');
    cy.get('[data-cy=password]').type('Padel2026!');
    cy.get('[data-cy=submit]').should('be.disabled');
  });

  it('affiche un message clair sur des identifiants invalides', () => {
    cy.get('[data-cy=matricule]').type('G1042');
    cy.get('[data-cy=password]').type('mauvaispass');
    cy.get('[data-cy=submit]').click();
    cy.get('[data-cy=login-error]').should('contain', 'Identifiants invalides');
  });

  it('connecte un membre global et affiche le planning', () => {
    cy.get('[data-cy=matricule]').type('G1042');
    cy.get('[data-cy=password]').type('Padel2026!');
    cy.get('[data-cy=submit]').click();

    cy.url().should('include', '/planning');
    cy.get('[data-cy=planning-grid]').should('be.visible');
    cy.contains('Votre statut').should('be.visible');
  });
});
