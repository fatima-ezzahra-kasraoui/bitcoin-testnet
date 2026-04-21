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

  // Wallets (avec token)
  createWallet(userId: string, label: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/wallets`, { userId, label }, { headers: this.getHeaders() });
  }

  getWallets(userId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/wallets/${userId}`, { headers: this.getHeaders() });
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
}