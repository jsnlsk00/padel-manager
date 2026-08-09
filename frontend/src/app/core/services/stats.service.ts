import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Stats } from '../models/models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class StatsService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/stats`;

  global(): Observable<Stats> {
    return this.http.get<Stats>(`${this.base}/global`);
  }

  site(siteId: number): Observable<Stats> {
    return this.http.get<Stats>(`${this.base}/site/${siteId}`);
  }
}
