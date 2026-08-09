import { SlotTimePipe } from './slot-time.pipe';

describe('SlotTimePipe', () => {
  const pipe = new SlotTimePipe();

  it('ajoute 1h30 pour afficher la fin du match', () => {
    const start = new Date(2026, 7, 25, 18, 30).toISOString();
    expect(pipe.transform(start)).toBe('18:30 – 20:00');
  });

  it('affiche l heure seule sur demande', () => {
    const start = new Date(2026, 7, 25, 9, 45).toISOString();
    expect(pipe.transform(start, false)).toBe('09:45');
  });

  it('rend une chaine vide sans valeur', () => {
    expect(pipe.transform('')).toBe('');
  });
});
