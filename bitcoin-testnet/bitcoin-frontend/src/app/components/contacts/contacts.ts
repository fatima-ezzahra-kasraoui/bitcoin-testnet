import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BitcoinService } from '../../services/bitcoin';
import { Router } from '@angular/router';

@Component({
  selector: 'app-contacts',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './contacts.html',
  styleUrl: './contacts.css'
})
export class ContactsComponent implements OnInit {
  contacts: any[] = [];
  newLabel = '';
  newAddress = '';
  message = '';
  currentPage = 'contacts';

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
    this.loadContacts();
  }

  loadContacts() {
    this.bitcoinService.getContacts().subscribe({
      next: (res) => this.contacts = res,
      error: () => this.message = 'Erreur chargement contacts'
    });
  }

  addContact() {
    if (!this.newLabel || !this.newAddress) {
      this.message = 'Veuillez remplir tous les champs';
      setTimeout(() => this.message = '', 3000);
      return;
    }
    this.bitcoinService.addContact(this.newLabel, this.newAddress).subscribe({
      next: () => {
        this.newLabel = '';
        this.newAddress = '';
        this.message = 'Contact ajouté avec succès !';
        setTimeout(() => this.message = '', 3000);
        this.loadContacts();
      },
      error: () => {
        this.message = 'Erreur lors de l\'ajout';
        setTimeout(() => this.message = '', 3000);
      }
    });
  }

  deleteContact(id: string) {
    if (confirm('Supprimer ce contact ?')) {
      this.bitcoinService.deleteContact(id).subscribe({
        next: () => this.loadContacts(),
        error: () => {
          this.message = 'Erreur lors de la suppression';
          setTimeout(() => this.message = '', 3000);
        }
      });
    }
  }
  goBack() {
    this.router.navigate(['/dashboard']);
  }

  goToDashboard() { this.router.navigate(['/dashboard']); }
  goToWallet(address: any) { this.router.navigate(['/wallet']); }
  goToSecurity() { this.router.navigate(['/security']); }
  goToContacts() { this.router.navigate(['/contacts']); }
  goToProfile() { this.router.navigate(['/profile']); }
  logout() { localStorage.clear(); this.router.navigate(['/login']); }
}
