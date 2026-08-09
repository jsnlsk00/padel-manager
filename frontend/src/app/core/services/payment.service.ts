import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PaymentResult } from '../models/models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/payments`;

  /** Paie sa part de 15 EUR ; le backend y ajoute le solde du s'il en existe un. */
  payShare(matchId: number): Observable<PaymentResult> {
    return this.http.post<PaymentResult>(`${this.base}/match/${matchId}`, {});
  }

  payBalance(): Observable<PaymentResult> {
    return this.http.post<PaymentResult>(`${this.base}/balance`, {});
  }

  history(): Observable<PaymentResult[]> {
    return this.http.get<PaymentResult[]>(`${this.base}/me`);
  }
}
