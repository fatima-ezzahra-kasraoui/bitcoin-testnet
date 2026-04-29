import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { QRCodeComponent } from 'angularx-qrcode';
import { BitcoinService } from '../../services/bitcoin';

@Component({
  selector: 'app-mfa-setup',
  standalone: true,
  imports: [CommonModule, FormsModule, QRCodeComponent],
  templateUrl: './mfa-setup.html',
  styleUrl: './mfa-setup.css'
})
export class MfaSetupComponent implements OnInit {
  qrCodeUrl = '';
  secretKey = '';
  confirmCode = '';
  message = '';
  isLoading = true;
  isVerifying = false;

  constructor(
    private bitcoinService: BitcoinService,
    private router: Router
  ) {}

  ngOnInit() {
    // Get the QR code data passed from the register response
    // It was stored in localStorage by the register component
    this.qrCodeUrl = localStorage.getItem('mfaQrCodeUrl') || '';
    this.secretKey = localStorage.getItem('mfaSecret') || '';

    // If no QR data found — user came here directly, redirect to dashboard
    if (!this.qrCodeUrl) {
      this.router.navigate(['/dashboard']);
      return;
    }

    this.isLoading = false;
  }

  confirmSetup() {
    // Validate the code format before sending
    if (!this.confirmCode || this.confirmCode.length !== 6) {
      this.message = 'Please enter the 6-digit code from Google Authenticator';
      return;
    }

    if (!/^\d{6}$/.test(this.confirmCode)) {
      this.message = 'Code must contain only digits';
      return;
    }

    this.isVerifying = true;
    this.message = '';

    // Use the pre-auth token stored during registration
    const preAuthToken = localStorage.getItem('preAuthToken') || '';

    this.bitcoinService.verifyMfa(this.confirmCode, preAuthToken).subscribe({
      next: (res) => {
        this.isVerifying = false;

        // MFA confirmed — store real token, clean up temp data
        localStorage.setItem('token', res.token);
        localStorage.removeItem('preAuthToken');
        localStorage.removeItem('mfaQrCodeUrl');
        localStorage.removeItem('mfaSecret');

        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isVerifying = false;
        this.message = err.error?.message || 'Invalid code. Please check Google Authenticator and try again.';
        this.confirmCode = '';
      }
    });
  }

  skipSetup() {
    // User skips — MFA stays disabled
    // Clean up temp data
    localStorage.removeItem('preAuthToken');
    localStorage.removeItem('mfaQrCodeUrl');
    localStorage.removeItem('mfaSecret');
    this.router.navigate(['/dashboard']);
  }
}