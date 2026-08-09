import { Pipe, PipeTransform } from '@angular/core';

/**
 * Pipe personnalise : affiche un creneau sous la forme "18:30 – 20:00",
 * en ajoutant la duree de 1h30 d'un match.
 */
@Pipe({ name: 'slotTime', standalone: true })
export class SlotTimePipe implements PipeTransform {
  transform(startTime: string | null | undefined, withEnd = true): string {
    if (!startTime) {
      return '';
    }
    const start = new Date(startTime);
    const format = (date: Date) =>
      `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;

    if (!withEnd) {
      return format(start);
    }
    const end = new Date(start.getTime() + 90 * 60 * 1000);
    return `${format(start)} – ${format(end)}`;
  }
}
