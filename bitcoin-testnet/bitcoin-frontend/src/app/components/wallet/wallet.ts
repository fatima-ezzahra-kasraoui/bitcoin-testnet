import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { BitcoinService } from '../../services/bitcoin';
import QRCode from 'qrcode';

@Component({
  selector: 'app-wallet',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './wallet.html',
  styleUrl: './wallet.css'
})
export class WalletComponent implements OnInit, OnDestroy {
  fromAddress = '';
  toAddress = '';
  amount = 0;
  message = '';
  messageText = '';
  signature = '';
  txResult: any = null;
  contacts: any[] = [];

  showSuggestions = false;
  filteredContacts: any[] = [];

  // Confirmation modal state
  showConfirmationModal = false;
  pendingTxData: any = null;
  countdown = '10:00';
  isCountdownUrgent = false;
  private countdownInterval: any;

  // QR code modal state
  showQrModal = false;
  qrCodeDataUrl = '';
  selectedWalletAddress = '';
  copiedToast = false;
  private copyToastTimer: any;

  constructor(
    private bitcoinService: BitcoinService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      if (params['address']) {
        this.fromAddress = params['address'];
      }
    });
    this.loadContacts();
  }

  ngOnDestroy() {
    clearInterval(this.countdownInterval);
    clearTimeout(this.copyToastTimer);
  }

  @HostListener('document:keydown.escape')
  onEscapeKey() {
    if (this.showQrModal) this.closeQrModal();
    if (this.showConfirmationModal) this.closeConfirmationModal();
  }

  loadContacts() {
    this.bitcoinService.getContacts().subscribe({
      next: (res) => {
        this.contacts = res;
        this.filteredContacts = res;
      },
      error: () => console.log('Erreur chargement contacts')
    });
  }

  filterContacts() {
    if (this.toAddress) {
      this.filteredContacts = this.contacts.filter(contact =>
        contact.label.toLowerCase().includes(this.toAddress.toLowerCase()) ||
        contact.address.toLowerCase().includes(this.toAddress.toLowerCase())
      );
      this.showSuggestions = true;
    } else {
      this.filteredContacts = this.contacts;
      this.showSuggestions = true;
    }
  }

  selectContact(contact: any) {
    this.toAddress = contact.address;
    this.showSuggestions = false;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event) {
    const target = event.target as HTMLElement;
    if (!target.closest('.suggestion-item') && !target.closest('.form-input')) {
      this.showSuggestions = false;
    }
  }

  // ── QR Code ────────────────────────────────────────────────

  generateQrCode(address: string) {
    this.selectedWalletAddress = address;
    QRCode.toDataURL(address, {
      width: 256,
      margin: 2,
      color: { dark: '#000000', light: '#ffffff' }
    }).then(url => {
      this.qrCodeDataUrl = url;
      this.showQrModal = true;
    });
  }

  closeQrModal() {
    this.showQrModal = false;
    this.qrCodeDataUrl = '';
    this.selectedWalletAddress = '';
    this.copiedToast = false;
  }

  downloadQr() {
    const link = document.createElement('a');
    link.download = 'wallet-qr.png';
    link.href = this.qrCodeDataUrl;
    link.click();
  }

  copyAddress(address: string) {
    navigator.clipboard.writeText(address).then(() => {
      this.copiedToast = true;
      clearTimeout(this.copyToastTimer);
      this.copyToastTimer = setTimeout(() => this.copiedToast = false, 2000);
    });
  }

  // ── Send Transaction ────────────────────────────────────────

  sendTransaction() {
    if (!this.fromAddress) {
      this.message = 'Veuillez sélectionner une adresse source';
      return;
    }
    this.bitcoinService.sendTransaction(this.fromAddress, this.toAddress, this.amount).subscribe({
      next: (res) => {
        if (res.status === 'AWAITING_CONFIRMATION') {
          this.pendingTxData = res;
          this.showConfirmationModal = true;
          this.startCountdown(res.expiresAt);
        } else {
          this.txResult = res;
          this.message = 'Transaction envoyée !';
          setTimeout(() => this.message = '', 3000);
        }
      },
      error: () => this.message = 'Erreur lors de la transaction'
    });
  }

  private startCountdown(expiresAtMs: number) {
    clearInterval(this.countdownInterval);
    this.countdownInterval = setInterval(() => {
      const remaining = Math.max(0, expiresAtMs - Date.now());
      const minutes = Math.floor(remaining / 60000);
      const seconds = Math.floor((remaining % 60000) / 1000);
      this.countdown = `${minutes}:${seconds.toString().padStart(2, '0')}`;
      this.isCountdownUrgent = remaining <= 60000;
      if (remaining <= 0) {
        clearInterval(this.countdownInterval);
        this.closeConfirmationModal();
        this.message = 'La fenêtre de confirmation a expiré.';
      }
    }, 1000);
  }

  closeConfirmationModal() {
    this.showConfirmationModal = false;
    this.pendingTxData = null;
    this.isCountdownUrgent = false;
    clearInterval(this.countdownInterval);
  }

  confirmTx() {
    if (!this.pendingTxData) return;
    this.bitcoinService.confirmTransaction(this.pendingTxData.txId).subscribe({
      next: () => {
        this.closeConfirmationModal();
        this.message = 'Transaction confirmée et envoyée !';
        setTimeout(() => this.message = '', 4000);
      },
      error: (err) => {
        this.message = err.error?.error || 'Erreur lors de la confirmation';
        this.closeConfirmationModal();
      }
    });
  }

  cancelTx() {
    if (!this.pendingTxData) return;
    this.bitcoinService.cancelTransaction(this.pendingTxData.txId).subscribe({
      next: () => {
        this.closeConfirmationModal();
        this.message = 'Transaction annulée.';
        setTimeout(() => this.message = '', 3000);
      },
      error: () => {
        this.closeConfirmationModal();
        this.message = 'Transaction annulée.';
      }
    });
  }

  getRiskLevel(score: number): string {
    if (score >= 80) return 'critical';
    if (score >= 60) return 'high';
    return 'medium';
  }

  getRiskLabel(score: number): string {
    if (score >= 80) return 'Critique';
    if (score >= 60) return 'Élevé';
    return 'Moyen';
  }

  signMessage() {
    this.bitcoinService.signMessage(this.fromAddress, this.messageText).subscribe({
      next: (res) => this.signature = res.signature,
      error: () => this.message = 'Erreur lors de la signature'
    });
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}
