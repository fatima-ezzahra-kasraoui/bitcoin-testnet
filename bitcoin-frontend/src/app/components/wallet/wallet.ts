import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { BitcoinService } from '../../services/bitcoin';

@Component({
  selector: 'app-wallet',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './wallet.html',
  styleUrl: './wallet.css'
})
export class WalletComponent {
  fromAddress = '';
  toAddress = '';
  amount = 0;
  message = '';
  messageText = '';
  signature = '';
  txResult: any = null;

  constructor(private bitcoinService: BitcoinService, private router: Router) {}

  sendTransaction() {
    this.bitcoinService.sendTransaction(this.fromAddress, this.toAddress, this.amount).subscribe({
      next: (res) => {
        this.txResult = res;
        this.message = 'Transaction envoyée !';
      },
      error: () => this.message = 'Erreur lors de la transaction'
    });
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