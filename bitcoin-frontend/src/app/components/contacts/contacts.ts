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

  constructor(
    private bitcoinService: BitcoinService,
    private router: Router
  ) {}

  ngOnInit() {
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
}
