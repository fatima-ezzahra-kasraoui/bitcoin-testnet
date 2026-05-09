// src/app/components/profile/profile.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BitcoinService } from '../../services/bitcoin';
import * as QRCode from 'qrcode';
@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class ProfileComponent implements OnInit {

  userId = '';
  currentPage = 'profile';
  wallets: any[] = [];
  balances: { [address: string]: string } = {};
  transactions: { [address: string]: any[] } = {};

  // Stats du portfolio
  portfolioStats = {
    totalBTC: '0',
    totalSats: 0,
    totalWallets: 0,
    totalReceived: '0',
    totalSent: '0',
    totalTransactions: 0
  };

  // Changement de mot de passe
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  passwordMessage = '';
  passwordError = '';

  // Message global
  message = '';
  messageType = ''; // 'success' ou 'error'

  // Loading
  isLoading = true;

  // Pour afficher/masquer les mots de passe
  showCurrentPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;

  // MFA section
  mfaEnabled = false;
  hasTotpSecret = false;
  showMfaDialog = false;
  mfaAction: 'enable' | 'disable' = 'enable';
  mfaConfirmPassword = '';
  mfaError = '';
  mfaMessage = '';
  showMfaQr = false;
  mfaQrCodeUrl = '';
  mfaSecretKey = '';
  mfaSetupCode = '';
  isProcessingMfa = false;

  constructor(
    private bitcoinService: BitcoinService,
    private router: Router
  ) {}

  private isTokenExpired(): boolean {
    const token = localStorage.getItem('token');
    if (!token) return true;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 < Date.now();
    } catch { return true; }
  }

  ngOnInit() {
    if (this.isTokenExpired()) {
      localStorage.clear();
      this.router.navigate(['/login']);
      return;
    }
    this.userId = localStorage.getItem('userId') || '';
    if (!this.userId) {
      this.router.navigate(['/login']);
      return;
    }
    this.loadData();
    this.loadMfaStatus();
  }

  loadData() {
    this.isLoading = true;
    this.bitcoinService.getWallets(this.userId).subscribe({
      next: (wallets) => {
        this.wallets = wallets;
        this.loadBalancesAndTransactions();
      },
      error: () => {
        this.showMessage('Erreur chargement des wallets', 'error');
        this.isLoading = false;
      }
    });
  }

  loadBalancesAndTransactions() {
    let completed = 0;
    const total = this.wallets.length;

    if (total === 0) {
      this.calculateStats();
      this.isLoading = false;
      return;
    }

    this.wallets.forEach(wallet => {
      // Charger balance
      this.bitcoinService.getBalance(wallet.address).subscribe({
        next: (res) => {
          this.balances[wallet.address] = res.balance;
          completed++;
          if (completed === total) this.calculateStats();
        },
        error: () => {
          this.balances[wallet.address] = '0 BTC';
          completed++;
          if (completed === total) this.calculateStats();
        }
      });

      // Charger transactions
      this.bitcoinService.getTransactions(wallet.address).subscribe({
        next: (res) => {
          this.transactions[wallet.address] = res;
        },
        error: () => {
          this.transactions[wallet.address] = [];
        }
      });
    });
  }

  calculateStats() {
    let totalBTC = 0;
    let totalReceived = 0;
    let totalSent = 0;
    let totalTransactions = 0;

    this.wallets.forEach(wallet => {
      // Solde
      const balanceStr = this.balances[wallet.address] || '0 BTC';
      const balanceBTC = parseFloat(balanceStr.replace(' BTC', ''));
      totalBTC += balanceBTC;

      // Transactions
      const txs = this.transactions[wallet.address] || [];
      totalTransactions += txs.length;

      txs.forEach(tx => {
        const amountBTC = tx.amount / 100_000_000;
        if (tx.toAddress === wallet.address) {
          totalReceived += amountBTC;
        } else if (tx.fromAddress === wallet.address) {
          totalSent += amountBTC;
        }
      });
    });

    this.portfolioStats = {
      totalBTC: totalBTC.toFixed(8),
      totalSats: Math.floor(totalBTC * 100_000_000),
      totalWallets: this.wallets.length,
      totalReceived: totalReceived.toFixed(8),
      totalSent: totalSent.toFixed(8),
      totalTransactions: totalTransactions
    };

    this.isLoading = false;
  }

  changePassword() {
    // Réinitialiser les messages
    this.passwordMessage = '';
    this.passwordError = '';

    // Validation
    if (!this.currentPassword || !this.newPassword) {
      this.passwordError = 'Veuillez remplir tous les champs';
      return;
    }

    if (this.newPassword.length < 6) {
      this.passwordError = 'Le nouveau mot de passe doit avoir au moins 6 caractères';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.passwordError = 'Les mots de passe ne correspondent pas';
      return;
    }

    // Appel API (à implémenter dans le backend)
    this.bitcoinService.changePassword(this.userId, this.currentPassword, this.newPassword).subscribe({
      next: () => {
        this.passwordMessage = '✅ Mot de passe changé avec succès !';
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        setTimeout(() => this.passwordMessage = '', 3000);
      },
      error: (err: any) => {
        this.passwordError = err.error?.message || 'Erreur lors du changement de mot de passe';
      }
    });
  }

  showMessage(msg: string, type: string) {
    this.message = msg;
    this.messageType = type;
    setTimeout(() => {
      this.message = '';
    }, 3000);
  }

  goToDashboard() {
    this.router.navigate(['/dashboard']);
  }

  logout() {
    localStorage.clear();
    this.router.navigate(['/login']);
  }

  goToWallet(address: any) { this.router.navigate(['/wallet']); }
  goToSecurity() { this.router.navigate(['/security']); }
  goToContacts() { this.router.navigate(['/contacts']); }
  goToProfile() { this.router.navigate(['/profile']); }
  toggleCurrentPassword() {
    this.showCurrentPassword = !this.showCurrentPassword;
  }

  toggleNewPassword() {
    this.showNewPassword = !this.showNewPassword;
  }

  toggleConfirmPassword() {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  // MFA methods
  loadMfaStatus() {
    this.bitcoinService.getMfaStatus().subscribe({
      next: (res) => {
        this.mfaEnabled = res.mfaEnabled;
        this.hasTotpSecret = res.hasTotpSecret;
      },
      error: () => {}
    });
  }

  openMfaDialog(action: 'enable' | 'disable') {
    this.mfaAction = action;
    this.showMfaDialog = true;
    this.mfaConfirmPassword = '';
    this.mfaError = '';
    this.showMfaQr = false;
    this.mfaSetupCode = '';
    this.mfaQrCodeUrl = '';
    this.mfaSecretKey = '';
  }

  closeMfaDialog() {
    this.showMfaDialog = false;
    this.showMfaQr = false;
    this.mfaConfirmPassword = '';
    this.mfaError = '';
    this.mfaSetupCode = '';
    this.isProcessingMfa = false;
  }

  async confirmMfaAction(): Promise<void> {
    if (!this.mfaConfirmPassword) {
      this.mfaError = 'Veuillez entrer votre mot de passe';
      return;
    }
    this.isProcessingMfa = true;
    this.mfaError = '';

    if (this.mfaAction === 'enable') {
      this.bitcoinService.enableMfa(this.mfaConfirmPassword).subscribe({
        next: async (res) => {
          if (res.requiresSetup) {
            // First-time setup: qrCodeUrl is an otpauth:// URI — render locally to base64 PNG
            const otpauthUri = res.qrCodeUrl || res.otpAuthUrl || res.uri || '';
            this.mfaSecretKey = res.secret || res.secretKey || res.manualEntryKey || '';
            try {
              this.mfaQrCodeUrl = await QRCode.toDataURL(otpauthUri, {
                width: 250,
                margin: 2,
                color: { dark: '#000000', light: '#ffffff' }
              });
            } catch (err) {
              console.error('QR generation error:', err);
              this.mfaQrCodeUrl = '';
            }
            this.showMfaQr = true;
          } else {
            // TOTP secret already existed — backend re-enabled without regenerating QR
            this.mfaEnabled = true;
            this.mfaMessage = 'Two-factor authentication enabled successfully';
            this.closeMfaDialog();
            setTimeout(() => this.mfaMessage = '', 4000);
          }
          this.isProcessingMfa = false;
        },
        error: (err) => {
          this.isProcessingMfa = false;
          this.mfaError = err.error?.message || 'Invalid password';
        }
      });
    } else {
      this.bitcoinService.disableMfa(this.mfaConfirmPassword).subscribe({
        next: () => {
          this.isProcessingMfa = false;
          this.mfaEnabled = false;
          this.mfaMessage = 'Two-factor authentication disabled successfully';
          this.closeMfaDialog();
          setTimeout(() => this.mfaMessage = '', 4000);
        },
        error: (err) => {
          this.isProcessingMfa = false;
          this.mfaError = err.error?.message || 'Invalid password';
        }
      });
    }
  }

  onQrImageError(event: any) {
    console.error('QR image failed to load:', this.mfaQrCodeUrl);
    if (this.mfaQrCodeUrl && !this.mfaQrCodeUrl.startsWith('http') && !this.mfaQrCodeUrl.startsWith('data:')) {
      this.mfaQrCodeUrl = 'https://chart.googleapis.com/chart?cht=qr&chs=250x250&chl=' + encodeURIComponent(this.mfaQrCodeUrl);
    }
  }

  confirmMfaSetupCode() {
    if (!this.mfaSetupCode || this.mfaSetupCode.length !== 6) {
      this.mfaError = 'Entrez le code à 6 chiffres';
      return;
    }
    this.isProcessingMfa = true;
    const token = localStorage.getItem('token') || '';

    this.bitcoinService.verifyMfa(this.mfaSetupCode, token).subscribe({
      next: () => {
        this.isProcessingMfa = false;
        this.mfaEnabled = true;
        this.closeMfaDialog();
        this.mfaMessage = 'Two-factor authentication enabled successfully';
        setTimeout(() => this.mfaMessage = '', 4000);
      },
      error: () => {
        this.isProcessingMfa = false;
        this.mfaError = 'Invalid code. Please check Google Authenticator and try again.';
        this.mfaSetupCode = '';
      }
    });
  }
}
