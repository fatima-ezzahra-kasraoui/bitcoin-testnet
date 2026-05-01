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

  // State management
  // 'credentials' = showing username/password form
  // 'totp' = showing 6-digit code form
  step: 'credentials' | 'totp' = 'credentials';

  message = '';
  isLoading = false;

  // Store pre-auth token between step 1 and step 2
  private preAuthToken = '';

  constructor(private bitcoinService: BitcoinService, private router: Router) {}

  login() {
    if (!this.username || !this.password) {
      this.message = 'Please fill in all fields';
      return;
    }

    this.isLoading = true;
    this.message = '';

    this.bitcoinService.login(this.username, this.password).subscribe({
      next: (res) => {
        this.isLoading = false;

        if (res.mfaRequired) {
          // MFA enabled — store pre-auth token and show TOTP screen
          this.preAuthToken = res.preAuthToken;
          this.step = 'totp';
        } else {
          // MFA not enabled — direct login
          localStorage.setItem('token', res.token);
          localStorage.setItem('userId', this.username);
          this.router.navigate(['/dashboard']);
        }
      },
      error: () => {
        this.isLoading = false;
        this.message = 'Invalid username or password';
      }
    });
  }

  verifyTotp() {
    if (!this.totpCode || this.totpCode.length !== 6) {
      this.message = 'Please enter the 6-digit code';
      return;
    }

    if (!/^\d{6}$/.test(this.totpCode)) {
      this.message = 'Code must contain only digits';
      return;
    }

    this.isLoading = true;
    this.message = '';

    this.bitcoinService.verifyMfa(this.totpCode, this.preAuthToken).subscribe({
      next: (res) => {
        this.isLoading = false;
        // Store real full JWT and go to dashboard
        localStorage.setItem('token', res.token);
        localStorage.setItem('userId', this.username);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading = false;
        this.message = err.error?.message || 'Invalid code. Please try again.';
        this.totpCode = '';
      }
    });
  }

  // Go back to step 1 — user wants to re-enter credentials
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