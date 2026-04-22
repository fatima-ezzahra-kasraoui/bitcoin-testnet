import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { BitcoinService } from '../../services/bitcoin';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent implements OnInit, OnDestroy {
  wallets: any[] = [];
  transactions: { [address: string]: any[] } = {};
  balances: { [address: string]: string } = {};
  userId = '';
  selectedWallet: string | null = null;

  // D3-F1 — TestNet status
  isConnected = false;
  peerCount = 0;
  private statusInterval: any;

  // Pour le toast
  toastMessage: string = '';
  toastVisible: boolean = false;

  constructor(private bitcoinService: BitcoinService, private router: Router) {}

  ngOnInit() {
    this.userId = localStorage.getItem('userId') || '';
    this.loadWallets();
    this.checkStatus();
    // Poll toutes les 10 secondes
    this.statusInterval = setInterval(() => this.checkStatus(), 10000);
  }

  ngOnDestroy() {
    if (this.statusInterval) clearInterval(this.statusInterval);
  }

  checkStatus() {
    this.bitcoinService.getStatus().subscribe({
      next: (res) => {
        this.isConnected = res.connected;
        this.peerCount = res.peers;
      },
      error: () => {
        this.isConnected = false;
        this.peerCount = 0;
      }
    });
  }

  loadWallets() {
    this.bitcoinService.getWallets(this.userId).subscribe({
      next: (res) => {
        this.wallets = res;
        this.wallets.forEach(w => {
          this.loadTransactions(w.address);
          this.loadBalance(w.address);
        });
      },
      error: () => console.log('Erreur chargement wallets')
    });
  }

  loadBalance(address: string) {
    this.bitcoinService.getBalance(address).subscribe({
      next: (res) => this.balances[address] = res.balance,
      error: () => this.balances[address] = '0 BTC'
    });
  }

  loadTransactions(address: string) {
    this.bitcoinService.getTransactions(address).subscribe({
      next: (res) => this.transactions[address] = res,
      error: () => this.transactions[address] = []
    });
  }

  // NOUVEAU : Ouvre le faucet manuel + copie l'adresse
  openFaucet(address: string) {
    // 1. Copier l'adresse dans le presse-papier
    navigator.clipboard.writeText(address).then(() => {
      // 2. Ouvrir le site du faucet
      window.open('https://coinfaucet.eu/en/btc-testnet/', '_blank');
      // 3. Afficher le toast
      this.showToast(`✅ Adresse copiée : ${address}\n📋 Collez-la sur le site du faucet !`);
    }).catch(() => {
      // Fallback si la copie échoue
      window.open('https://coinfaucet.eu/en/btc-testnet/', '_blank');
      this.showToast(`📋 Copiez manuellement : ${address}`);
    });
  }

  // Affiche un toast temporaire
  showToast(message: string) {
    this.toastMessage = message;
    this.toastVisible = true;
    setTimeout(() => {
      this.toastVisible = false;
    }, 3000);
  }

  toggleTransactions(address: string) {
    this.selectedWallet = this.selectedWallet === address ? null : address;
  }

  createWallet() {
    this.bitcoinService.createWallet(this.userId, 'Mon Wallet').subscribe({
      next: () => this.loadWallets(),
      error: () => console.log('Erreur création wallet')
    });
  }

  goToWallet() {
    this.router.navigate(['/wallet']);
  }

  logout() {
    localStorage.clear();
    this.router.navigate(['/login']);
  }
  goToProfile() {
    this.router.navigate(['/profile']);
  }
}
