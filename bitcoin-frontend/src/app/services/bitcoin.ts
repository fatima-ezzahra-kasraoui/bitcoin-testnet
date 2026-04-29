import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
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

  getTransactions(address: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/transactions/${address}`, { headers: this.getHeaders() });
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
