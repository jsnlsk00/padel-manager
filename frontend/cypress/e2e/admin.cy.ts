/// <reference types="cypress" />

describe('Administration', () => {
  const login = (matricule: string) => {
    cy.visit('/login');
    cy.get('[data-cy=matricule]').type(matricule);
    cy.get('[data-cy=password]').type('Padel2026!');
    cy.get('[data-cy=submit]').click();
  };

  it('un admin global voit les deux portees et les indicateurs', () => {
    login('A0001');

    cy.contains('Administration').click();
    cy.get('[data-cy=scope-global]').should('be.visible');
    cy.get('[data-cy=scope-site]').should('be.visible');
    cy.contains("Chiffre d'affaires").should('be.visible');
    cy.contains('Occupation').should('be.visible');
    cy.contains('Impayes').should('be.visible');
  });

  it('un admin de site ne voit pas le selecteur de portee', () => {
    login('A1001');

    cy.contains('Administration').click();
    cy.get('[data-cy=scope-global]').should('not.exist');
    cy.contains('Portee limitee a votre site').should('be.visible');
  });

  it('un simple membre est renvoye vers le planning', () => {
    login('G1042');

    cy.visit('/administration');
    cy.url().should('include', '/planning');
  });
});
