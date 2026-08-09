import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateMatchRequest, Match } from '../models/models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class MatchService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/matches`;

  getPublicMatches(siteId?: number): Observable<Match[]> {
    let params = new HttpParams();
    if (siteId != null) {
      params = params.set('siteId', siteId);
    }
    return this.http.get<Match[]>(`${this.base}/public`, { params });
  }

  getMyMatches(): Observable<Match[]> {
    return this.http.get<Match[]>(`${this.base}/me`);
  }

  getMatch(id: number): Observable<Match> {
    return this.http.get<Match>(`${this.base}/${id}`);
  }

  createMatch(request: CreateMatchRequest): Observable<Match> {
    return this.http.post<Match>(this.base, request);
  }

  joinMatch(id: number): Observable<Match> {
    return this.http.post<Match>(`${this.base}/${id}/join`, {});
  }
}
