import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Closure, Planning, Site } from '../models/models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SiteService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/sites`;

  getSites(): Observable<Site[]> {
    return this.http.get<Site[]>(this.base);
  }

  getSite(id: number): Observable<Site> {
    return this.http.get<Site>(`${this.base}/${id}`);
  }

  getClosures(siteId: number): Observable<Closure[]> {
    return this.http.get<Closure[]>(`${this.base}/${siteId}/closures`);
  }

  /** Planning d'une journee : la grille terrains x creneaux calculee par le backend. */
  getPlanning(siteId: number, day: string): Observable<Planning> {
    return this.http.get<Planning>(`${this.base}/${siteId}/planning`, { params: { day } });
  }

  addClosure(siteId: number, closedOn: string, reason: string): Observable<Closure> {
    return this.http.post<Closure>(`${this.base}/${siteId}/closures`, { closedOn, reason });
  }
}
