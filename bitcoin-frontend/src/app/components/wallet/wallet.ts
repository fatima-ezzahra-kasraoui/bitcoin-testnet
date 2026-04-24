import { Component, OnInit, HostListener } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { BitcoinService } from '../../services/bitcoin';

@Component({
  selector: 'app-wallet',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './wallet.html',
  styleUrl: './wallet.css'
})
export class WalletComponent implements OnInit {
  fromAddress = '';
  toAddress = '';
  amount = 0;
  message = '';
  messageText = '';
  signature = '';
  txResult: any = null;
  contacts: any[] = [];

  // Pour les suggestions personnalisées
  showSuggestions = false;
  filteredContacts: any[] = [];

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
    // Ne ferme pas la liste si on clique sur un élément de suggestion
    if (!target.closest('.suggestion-item') && !target.closest('.form-input')) {
      this.showSuggestions = false;
    }
  }

  sendTransaction() {
    if (!this.fromAddress) {
      this.message = 'Veuillez sélectionner une adresse source';
      return;
    }
    this.bitcoinService.sendTransaction(this.fromAddress, this.toAddress, this.amount).subscribe({
      next: (res) => {
        this.txResult = res;
        this.message = 'Transaction envoyée !';
        setTimeout(() => this.message = '', 3000);
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
