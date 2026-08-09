import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/models';

/**
 * Traduit une erreur HTTP en message affichable. Les regles metier remontent du
 * backend en 409 avec un texte deja explicite ; le reste est neutralise.
 */
@Injectable({ providedIn: 'root' })
export class ErrorMessageService {
  toMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'Une erreur inattendue est survenue.';
    }

    const body = error.error as ApiError | null;

    if (body?.fieldErrors) {
      return Object.values(body.fieldErrors).join(' ');
    }
    if (error.status === 0) {
      return 'Le serveur est injoignable. Verifiez que le backend est demarre.';
    }
    if (error.status === 401) {
      return 'Identifiants invalides.';
    }
    if (error.status === 403) {
      return body?.message ?? "Vous n'avez pas les droits pour cette operation.";
    }
    if (body?.message) {
      return body.message;
    }
    return 'Une erreur inattendue est survenue.';
  }
}
