import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class BitcoinService {
  private apiUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }

  // Auth (sans token)
  register(username: string, password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/register`, { username, password });
  }

  login(username: string, password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/login`, { username, password });
  }

  checkUsername(username: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/auth/check-username?username=${username}`);
  }

  // MFA endpoints
  skipMfaSetup(): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/skip-mfa`, {}, { headers: this.getHeaders() });
  }

  verifyMfa(code: string, preAuthToken: string): Observable<any> {
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${preAuthToken}` });
    return this.http.post(`${this.apiUrl}/auth/mfa/verify`, { code }, { headers });
  }

  enableMfa(password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/mfa/enable`, { password }, { headers: this.getHeaders() });
  }

  disableMfa(password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/mfa/disable`, { password }, { headers: this.getHeaders() });
  }

  getMfaStatus(): Observable<any> {
    return this.http.get(`${this.apiUrl}/auth/mfa/status`, { headers: this.getHeaders() });
  }

  // Status (public)
  getStatus(): Observable<any> {
    return this.http.get(`${this.apiUrl}/status`);
  }

  // Wallets (avec token)
  createWallet(userId: string, label: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/wallets`, { userId, label }, { headers: this.getHeaders() });
  }

  getWallets(userId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/wallets/${userId}`, { headers: this.getHeaders() });
  }

  getBalance(address: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/wallets/${address}/balance`, { headers: this.getHeaders() });
  }

  requestFaucet(address: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/wallets/${address}/faucet`, {}, { headers: this.getHeaders() });
  }

  // Transactions (avec token)
  sendTransaction(fromAddress: string, toAddress: string, amount: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/transactions`, { fromAddress, toAddress, amount }, { headers: this.getHeaders() });
  }

  getTransactions(address: string, filters?: { status?: string; from?: string; to?: string; sort?: string }): Observable<any> {
    let params = new HttpParams();
    if (filters?.status) params = params.set('status', filters.status);
    if (filters?.from)   params = params.set('from', filters.from);
    if (filters?.to)     params = params.set('to', filters.to);
    if (filters?.sort)   params = params.set('sort', filters.sort);
    return this.http.get(`${this.apiUrl}/transactions/${address}`, { headers: this.getHeaders(), params });
  }

  exportTransactions(address: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/transactions/${address}/export`, {
      headers: this.getHeaders(),
      responseType: 'blob'
    });
  }

  confirmTransaction(txId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/transactions/${txId}/confirm`, {}, { headers: this.getHeaders() });
  }

  cancelTransaction(txId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/transactions/${txId}/cancel`, {}, { headers: this.getHeaders() });
  }

  getPendingConfirmations(): Observable<any> {
    return this.http.get(`${this.apiUrl}/transactions/pending-confirmation`, { headers: this.getHeaders() });
  }

  // Notifications
  getNotifications(): Observable<any> {
    return this.http.get(`${this.apiUrl}/notifications`, { headers: this.getHeaders() });
  }

  markNotificationRead(id: string): Observable<any> {
    return this.http.patch(`${this.apiUrl}/notifications/${id}/read`, {}, { headers: this.getHeaders() });
  }

  // Messages (avec token)
  signMessage(address: string, message: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/messages/sign`, { address, message }, { headers: this.getHeaders() });
  }

  changePassword(userId: string, currentPassword: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/change-password`,
      { userId, currentPassword, newPassword },
      { headers: this.getHeaders() });
  }

  deleteWallet(address: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/wallets/${address}`, {
      headers: this.getHeaders()
    });
  }

  updateWalletLabel(address: string, label: string): Observable<any> {
    return this.http.patch(`${this.apiUrl}/wallets/${address}/label`, { label }, {
      headers: this.getHeaders()
    });
  }

  // Security
  getSecurityInsights(): Observable<any> {
    return this.http.get(`${this.apiUrl}/security/insights`, { headers: this.getHeaders() });
  }

  getScoreHistory(): Observable<any> {
    return this.http.get(`${this.apiUrl}/security/score-history`, { headers: this.getHeaders() });
  }

  // Contacts
  getContacts(): Observable<any> {
    return this.http.get(`${this.apiUrl}/contacts`, {
      headers: this.getHeaders()
    });
  }

  addContact(label: string, address: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/contacts`, { label, address }, {
      headers: this.getHeaders()
    });
  }

  deleteContact(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/contacts/${id}`, {
      headers: this.getHeaders()
    });
  }
}
