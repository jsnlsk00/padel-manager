import { MemberCategoryPipe } from './member-category.pipe';

describe('MemberCategoryPipe', () => {
  const pipe = new MemberCategoryPipe();

  it('traduit les trois categories', () => {
    expect(pipe.transform('GLOBAL')).toBe('Membre global');
    expect(pipe.transform('SITE')).toBe('Membre de site');
    expect(pipe.transform('FREE')).toBe('Membre libre');
  });

  it('ajoute la fenetre de reservation a la demande', () => {
    expect(pipe.transform('GLOBAL', true)).toBe('Membre global · 21 jours');
    expect(pipe.transform('FREE', true)).toBe('Membre libre · 5 jours');
  });

  it('rend une chaine vide sans valeur', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
  });
});
