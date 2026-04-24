// src/app/components/profile/profile.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BitcoinService } from '../../services/bitcoin';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class ProfileComponent implements OnInit {

  userId = '';
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

  constructor(
    private bitcoinService: BitcoinService,
    private router: Router
  ) {}

  ngOnInit() {
    this.userId = localStorage.getItem('userId') || '';
    if (!this.userId) {
      this.router.navigate(['/login']);
      return;
    }
    this.loadData();
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
  toggleCurrentPassword() {
    this.showCurrentPassword = !this.showCurrentPassword;
  }

  toggleNewPassword() {
    this.showNewPassword = !this.showNewPassword;
  }

  toggleConfirmPassword() {
    this.showConfirmPassword = !this.showConfirmPassword;
  }
}
