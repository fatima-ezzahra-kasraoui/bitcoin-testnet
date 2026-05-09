import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BitcoinService } from '../../services/bitcoin';
import * as QRCode from 'qrcode';

@Component({
  selector: 'app-mfa-setup',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mfa-setup.html',
  styleUrl: './mfa-setup.css'
})
export class MfaSetupComponent implements OnInit {
  qrCodeUrl = '';
  qrCodeDataUrl = '';
  secretKey = '';
  confirmCode = '';
  message = '';
  isLoading = true;
  isVerifying = false;

  constructor(
    private bitcoinService: BitcoinService,
    private router: Router
  ) {}

  async ngOnInit() {
    // Failsafe: if isLoading is still true after 5s, show error
    setTimeout(() => {
      if (this.isLoading) {
        this.isLoading = false;
        this.message = 'Unable to load setup. Please go back and try again.';
      }
    }, 5000);

    this.qrCodeUrl = localStorage.getItem('mfaQrCodeUrl') || '';
    this.secretKey = localStorage.getItem('mfaSecret') || '';

    if (!this.qrCodeUrl) {
      // No setup data in storage — show error, let user click Go Back
      this.isLoading = false;
      this.message = 'Setup data not found. Please go back and register again.';
      return;
    }

    try {
      this.qrCodeDataUrl = await QRCode.toDataURL(this.qrCodeUrl, {
        width: 200,
        margin: 2,
        color: { dark: '#000000', light: '#ffffff' }
      });
    } catch (err) {
      console.error('QR generation error:', err);
      this.message = 'Failed to generate QR code. Please go back and try again.';
    }
    this.isLoading = false;
  }

  confirmSetup() {
    if (!this.confirmCode || this.confirmCode.length !== 6) {
      this.message = 'Enter the 6-digit code from Google Authenticator';
      return;
    }
    if (!/^\d{6}$/.test(this.confirmCode)) {
      this.message = 'Code must contain digits only';
      return;
    }

    this.isVerifying = true;
    this.message = '';

    const preAuthToken = localStorage.getItem('preAuthToken') || '';

    this.bitcoinService.verifyMfa(this.confirmCode, preAuthToken).subscribe({
      next: (res) => {
        this.isVerifying = false;
        localStorage.setItem('token', res.token);
        localStorage.removeItem('preAuthToken');
        localStorage.removeItem('mfaQrCodeUrl');
        localStorage.removeItem('mfaSecret');
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isVerifying = false;
        this.message = err.error?.message || 'Invalid code. Check Google Authenticator and try again.';
        this.confirmCode = '';
      }
    });
  }

  skipSetup() {
    this.bitcoinService.skipMfaSetup().subscribe({
      next: (res) => {
        if (res?.token) {
          localStorage.setItem('token', res.token);
          if (res.username) localStorage.setItem('userId', res.username);
        }
        localStorage.removeItem('preAuthToken');
        localStorage.removeItem('mfaQrCodeUrl');
        localStorage.removeItem('mfaSecret');
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        localStorage.removeItem('preAuthToken');
        localStorage.removeItem('mfaQrCodeUrl');
        localStorage.removeItem('mfaSecret');
        this.router.navigate(['/dashboard']);
      }
    });
  }

  goBack() {
    this.router.navigate(['/login']);
  }
}
