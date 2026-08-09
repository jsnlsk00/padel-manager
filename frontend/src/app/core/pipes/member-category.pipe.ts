import { Pipe, PipeTransform } from '@angular/core';
import { MemberType } from '../models/models';

/**
 * Pipe personnalise : rend lisible une categorie de membre, avec sa fenetre
 * de reservation. Utilise dans l'en-tete et le tableau des membres.
 */
@Pipe({ name: 'memberCategory', standalone: true })
export class MemberCategoryPipe implements PipeTransform {
  private readonly labels: Record<MemberType, string> = {
    GLOBAL: 'Membre global',
    SITE: 'Membre de site',
    FREE: 'Membre libre',
  };

  private readonly windows: Record<MemberType, number> = {
    GLOBAL: 21,
    SITE: 14,
    FREE: 5,
  };

  transform(type: MemberType | null | undefined, withWindow = false): string {
    if (!type) {
      return '';
    }
    return withWindow ? `${this.labels[type]} · ${this.windows[type]} jours` : this.labels[type];
  }
}
