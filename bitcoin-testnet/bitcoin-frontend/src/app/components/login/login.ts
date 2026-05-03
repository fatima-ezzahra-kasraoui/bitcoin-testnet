import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { BitcoinService } from '../../services/bitcoin';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  // Step 1 fields
  username = '';
  password = '';

  // Step 2 fields
  totpCode = '';

  // 'credentials' = username/password form | 'totp' = 6-digit code form
  step: 'credentials' | 'totp' = 'credentials';

  message = '';
  isLoading = false;

  private preAuthToken = '';

  constructor(private bitcoinService: BitcoinService, private router: Router) {}

  login() {
    if (!this.username || !this.password) {
      this.message = 'Veuillez remplir tous les champs';
      return;
    }
    this.isLoading = true;
    this.message = '';

    this.bitcoinService.login(this.username, this.password).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res.mfaRequired) {
          this.preAuthToken = res.preAuthToken;
          this.step = 'totp';
        } else {
          localStorage.setItem('token', res.token);
          localStorage.setItem('userId', this.username);
          this.router.navigate(['/dashboard']);
        }
      },
      error: () => {
        this.isLoading = false;
        this.message = 'Identifiants invalides';
      }
    });
  }

  verifyTotp() {
    if (!this.totpCode || this.totpCode.length !== 6) {
      this.message = 'Entrez le code à 6 chiffres';
      return;
    }
    if (!/^\d{6}$/.test(this.totpCode)) {
      this.message = 'Le code ne doit contenir que des chiffres';
      return;
    }
    this.isLoading = true;
    this.message = '';

    this.bitcoinService.verifyMfa(this.totpCode, this.preAuthToken).subscribe({
      next: (res) => {
        this.isLoading = false;
        localStorage.setItem('token', res.token);
        localStorage.setItem('userId', this.username);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading = false;
        this.message = err.error?.message || 'Code invalide. Réessayez.';
        this.totpCode = '';
      }
    });
  }

  backToCredentials() {
    this.step = 'credentials';
    this.totpCode = '';
    this.message = '';
    this.preAuthToken = '';
  }

  goToRegister() {
    this.router.navigate(['/register']);
  }
}
