import { Component, OnInit } from '@angular/core';
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
export class DashboardComponent implements OnInit {
  wallets: any[] = [];
  transactions: { [address: string]: any[] } = {};
  userId = '';
  selectedWallet: string | null = null;

  constructor(private bitcoinService: BitcoinService, private router: Router) {}

  ngOnInit() {
    this.userId = localStorage.getItem('userId') || '';
    this.loadWallets();
  }

  loadWallets() {
    this.bitcoinService.getWallets(this.userId).subscribe({
      next: (res) => {
        this.wallets = res;
        this.wallets.forEach(w => this.loadTransactions(w.address));
      },
      error: () => console.log('Erreur chargement wallets')
    });
  }

  loadTransactions(address: string) {
    this.bitcoinService.getTransactions(address).subscribe({
      next: (res) => this.transactions[address] = res,
      error: () => this.transactions[address] = []
    });
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
}