/// <reference types="cypress" />

/** Parcours principal : consulter le planning, ouvrir un match, regler son solde. */
describe('Planning et reservation', () => {
  const login = (matricule: string) => {
    cy.visit('/login');
    cy.get('[data-cy=matricule]').type(matricule);
    cy.get('[data-cy=password]').type('Padel2026!');
    cy.get('[data-cy=submit]').click();
    cy.url().should('include', '/planning');
  };

  it('affiche la grille terrains x creneaux du site', () => {
    login('G1042');

    cy.get('[data-cy=planning-grid]').within(() => {
      cy.contains('Terrain 1').should('be.visible');
      cy.contains('Heure').should('be.visible');
    });
    cy.contains('Public a rejoindre').should('be.visible');
  });

  it('ouvre le detail d un match et montre les 4 places', () => {
    login('G1042');

    cy.get('[data-cy=planning-grid]').find('button.cell').not('.free').first().click();
    cy.get('[data-cy=match-dialog]').should('be.visible');
    cy.get('[data-cy=match-dialog]').contains('60 € divises en 4');
  });

  it('propose de creer un match sur un creneau libre', () => {
    login('G1042');

    cy.get('[data-cy=planning-grid]').find('button.cell.free').first().click();
    cy.get('[data-cy=create-dialog]').should('be.visible');
    cy.get('[data-cy=create-dialog]').contains('Prive');
  });

  it('bloque la reservation tant qu un solde est du', () => {
    login('G1042');

    cy.get('[data-cy=balance-card]').should('be.visible');
    cy.get('[data-cy=balance-amount]').invoke('text').then((text) => {
      if (!text.includes('0 €')) {
        cy.get('[data-cy=planning-grid]').find('button.cell.free').first().click();
        cy.get('[data-cy=create-dialog]').contains('solde');
      }
    });
  });

  it('change de jour dans la semaine', () => {
    login('G1042');

    cy.get('.days button').eq(3).click();
    cy.get('.days button').eq(3).should('have.class', 'selected');
  });

  it('un membre de site ne peut pas selectionner un autre site', () => {
    login('S12008');

    cy.get('[data-cy=site-select]').find('option:not([disabled])').should('have.length', 1);
  });
});
