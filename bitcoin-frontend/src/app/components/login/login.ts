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
  isLoading = false;

  constructor(private bitcoinService: BitcoinService, private router: Router) {}

  login() {
    if (!this.username || !this.password) {
      this.message = 'Please fill in all fields';
      return;
    }

    this.isLoading = true;
    this.message = '';

    this.bitcoinService.login(this.username, this.password).subscribe({
      next: (res) => {
        this.isLoading = false;
        localStorage.setItem('token', res.token);
        localStorage.setItem('userId', this.username);
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.isLoading = false;
        this.message = 'Invalid username or password';
      }
    });
  }

  goToRegister() {
    this.router.navigate(['/register']);
  }
}