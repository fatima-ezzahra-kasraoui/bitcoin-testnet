import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { BitcoinService } from '../../services/bitcoin';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  username = '';
  password = '';
  message = '';

  constructor(private bitcoinService: BitcoinService, private router: Router) {}

  login() {
    this.bitcoinService.login(this.username, this.password).subscribe({
      next: (res) => {
        localStorage.setItem('token', res.token);
        localStorage.setItem('userId', this.username);
        this.router.navigate(['/dashboard']);
      },
      error: () => this.message = 'Erreur de connexion'
    });
  }

  register() {
    this.bitcoinService.register(this.username, this.password).subscribe({
      next: (res) => {
        localStorage.setItem('token', res.token);
        localStorage.setItem('userId', this.username);
        this.router.navigate(['/dashboard']);
      },
      error: () => this.message = 'Erreur d\'inscription'
    });
  }
}