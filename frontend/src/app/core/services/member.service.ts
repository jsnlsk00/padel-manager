import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Member } from '../models/models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class MemberService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/members`;

  me(): Observable<Member> {
    return this.http.get<Member>(`${this.base}/me`);
  }

  list(): Observable<Member[]> {
    return this.http.get<Member[]>(this.base);
  }

  byMatricule(matricule: string): Observable<Member> {
    return this.http.get<Member>(`${this.base}/${matricule}`);
  }
}
