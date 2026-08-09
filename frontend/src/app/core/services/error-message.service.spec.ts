import { HttpErrorResponse } from '@angular/common/http';
import { ErrorMessageService } from './error-message.service';

describe('ErrorMessageService', () => {
  const service = new ErrorMessageService();

  it('remonte le message metier d un 409', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { message: 'Ce terrain est deja reserve sur ce creneau' },
    });

    expect(service.toMessage(error)).toBe('Ce terrain est deja reserve sur ce creneau');
  });

  it('concatene les erreurs de champs d un 400', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { fieldErrors: { matricule: 'Format attendu : G, S ou L.' } },
    });

    expect(service.toMessage(error)).toContain('Format attendu');
  });

  it('explique un serveur injoignable', () => {
    expect(service.toMessage(new HttpErrorResponse({ status: 0 }))).toContain('injoignable');
  });

  it('neutralise une erreur inconnue', () => {
    expect(service.toMessage('boom')).toBe('Une erreur inattendue est survenue.');
  });
});
