import { Component, HostListener, OnInit, OnDestroy, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BitcoinService } from '../../services/bitcoin';
import * as THREE from 'three';
import { gsap } from 'gsap';
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent implements OnInit, OnDestroy, AfterViewInit {
  wallets: any[] = [];
  transactions: { [address: string]: any[] } = {};
  balances: { [address: string]: string } = {};
  userId = '';
  selectedWallet: string | null = null;

  isConnected = false;
  peerCount = 0;
  private statusInterval: any;

  toastMessage: string = '';
  toastVisible: boolean = false;

  txFilter = { status: '', from: '', to: '', sort: 'date' };

  notifications: any[] = [];
  unreadCount = 0;
  notifDropdownOpen = false;
  private notifInterval: any;

  pendingConfirmationCount = 0;
  private pendingConfirmationAddress: string | null = null;

  editingWallet: string | null = null;
  editLabelValue = '';

  private coinCleanup: (() => void) | null = null;

  constructor(private bitcoinService: BitcoinService, private router: Router) {}

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
    this.loadWallets();
    this.checkStatus();
    this.statusInterval = setInterval(() => this.checkStatus(), 10000);
    this.loadNotifications();
    this.loadPendingConfirmations();
    this.notifInterval = setInterval(() => {
      this.loadNotifications();
      this.loadPendingConfirmations();
    }, 15000);
  }

  ngAfterViewInit() {
    setTimeout(() => {
      gsap.fromTo('.sidebar', { x: -280, opacity: 0 }, { x: 0, opacity: 1, duration: 0.7, ease: 'power3.out' });
      gsap.fromTo('.topbar', { y: -64, opacity: 0 }, { y: 0, opacity: 1, duration: 0.5, delay: 0.2 });
      gsap.fromTo('[class*="cascade-"]', { y: 30, opacity: 0 }, { y: 0, opacity: 1, stagger: 0.08, duration: 0.6, delay: 0.3, ease: 'power2.out' });
    }, 100);
  }

  ngOnDestroy() {
    if (this.statusInterval) clearInterval(this.statusInterval);
    if (this.notifInterval) clearInterval(this.notifInterval);
    this.coinCleanup?.();
  }

  @HostListener('document:click')
  closeNotifDropdown() {
    this.notifDropdownOpen = false;
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
    const filters: any = {};
    if (this.txFilter.status) filters.status = this.txFilter.status;
    if (this.txFilter.from)   filters.from = this.txFilter.from;
    if (this.txFilter.to)     filters.to = this.txFilter.to;
    if (this.txFilter.sort && this.txFilter.sort !== 'date') filters.sort = this.txFilter.sort;
    this.bitcoinService.getTransactions(address, filters).subscribe({
      next: (res) => this.transactions[address] = res,
      error: () => this.transactions[address] = []
    });
  }

  applyFilters(address: string) {
    this.loadTransactions(address);
  }

  exportCsv(address: string) {
    this.bitcoinService.exportTransactions(address).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'transactions.csv';
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => console.log('Erreur export CSV')
    });
  }

  toggleTransactions(address: string) {
    if (this.selectedWallet === address) {
      this.selectedWallet = null;
    } else {
      this.selectedWallet = address;
      this.txFilter = { status: '', from: '', to: '', sort: 'date' };
      this.loadTransactions(address);
    }
  }

  loadNotifications() {
    this.bitcoinService.getNotifications().subscribe({
      next: (res) => {
        this.notifications = res;
        this.unreadCount = res.length;
      },
      error: () => {}
    });
  }

  toggleNotifDropdown(event: Event) {
    event.stopPropagation();
    this.notifDropdownOpen = !this.notifDropdownOpen;
  }

  markAsRead(notif: any) {
    this.bitcoinService.markNotificationRead(notif.id).subscribe({
      next: () => {
        this.notifications = this.notifications.filter(n => n.id !== notif.id);
        this.unreadCount = this.notifications.length;
      },
      error: () => {}
    });
  }

  loadPendingConfirmations() {
    this.bitcoinService.getPendingConfirmations().subscribe({
      next: (res) => {
        this.pendingConfirmationCount = res.length;
        if (res.length > 0) {
          this.pendingConfirmationAddress = res[0].fromAddress;
        } else {
          this.pendingConfirmationAddress = null;
        }
      },
      error: () => {}
    });
  }

  goToPendingReview() {
    const addr = this.pendingConfirmationAddress;
    if (!addr) return;
    this.selectedWallet = addr;
    this.txFilter = { status: 'AWAITING_CONFIRMATION', from: '', to: '', sort: 'date' };
    this.loadTransactions(addr);
    setTimeout(() => {
      const el = document.getElementById('wallet-' + addr);
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 150);
  }

  openFaucet(address: string) {
    window.open('https://coinfaucet.eu/en/btc-testnet/', '_blank');
    navigator.clipboard.writeText(address).then(() => {
      this.showToast(`Adresse copiée : ${address} — Collez-la sur le site du faucet !`);
    }).catch(() => {
      this.showToast(`Copiez manuellement : ${address}`);
    });
  }

  showToast(message: string) {
    this.toastMessage = message;
    this.toastVisible = true;
    setTimeout(() => { this.toastVisible = false; }, 3000);
  }

  createWallet() {
    this.bitcoinService.createWallet(this.userId, 'Mon Wallet').subscribe({
      next: () => this.loadWallets(),
      error: () => console.log('Erreur création wallet')
    });
  }

  goToWallet(address: string) {
    this.router.navigate(['/wallet'], { queryParams: { address: address } });
  }

  logout() {
    localStorage.clear();
    this.router.navigate(['/login']);
  }

  goToProfile() {
    this.router.navigate(['/profile']);
  }

  goToContacts() {
    this.router.navigate(['/contacts']);
  }

  goToSecurity() {
    this.router.navigate(['/security']);
  }

  confirmDelete(wallet: any) {
    const confirmed = confirm(`Voulez-vous vraiment supprimer le wallet "${wallet.label}" ?\nSolde: ${this.balances[wallet.address] || '0 BTC'}`);
    if (confirmed) {
      this.deleteWallet(wallet.address);
    }
  }

  deleteWallet(address: string) {
    this.bitcoinService.deleteWallet(address).subscribe({
      next: () => {
        this.wallets = this.wallets.filter(w => w.address !== address);
        delete this.balances[address];
        delete this.transactions[address];
        alert('Wallet supprimé avec succès');
      },
      error: (err) => {
        const message = err.error?.message || err.message || 'Erreur lors de la suppression';
        alert(message);
      }
    });
  }

  startEdit(wallet: any) {
    this.editingWallet = wallet.address;
    this.editLabelValue = wallet.label;
  }

  saveLabel(wallet: any) {
    if (this.editLabelValue && this.editLabelValue !== wallet.label) {
      this.bitcoinService.updateWalletLabel(wallet.address, this.editLabelValue).subscribe({
        next: (res) => {
          wallet.label = res.label;
          this.editingWallet = null;
        },
        error: () => alert('Erreur lors de la modification')
      });
    } else {
      this.editingWallet = null;
    }
  }

  cancelEdit() {
    this.editingWallet = null;
  }
}
