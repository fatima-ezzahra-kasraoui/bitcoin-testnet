import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { BitcoinService } from '../../services/bitcoin';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class RegisterComponent {
  username = '';
  password = '';
  confirmPassword = '';
  message = '';
  isLoading = false;

  usernameAvailable: boolean | null = null;
  checkingUsername = false;

  constructor(private bitcoinService: BitcoinService, private router: Router) {}

  checkUsername() {
    if (this.username.length < 3) {
      this.usernameAvailable = null;
      return;
    }
    this.checkingUsername = true;
    this.bitcoinService.checkUsername(this.username).subscribe({
      next: (res) => {
        this.usernameAvailable = res.available;
        this.checkingUsername = false;
      },
      error: () => {
        this.usernameAvailable = null;
        this.checkingUsername = false;
      }
    });
  }

  hasUppercase(): boolean { return /[A-Z]/.test(this.password); }
  hasNumber(): boolean { return /[0-9]/.test(this.password); }
  hasSpecial(): boolean { return /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(this.password); }

  getStrengthScore(): number {
    let score = 0;
    if (this.password.length >= 8) score++;
    if (this.hasUppercase()) score++;
    if (this.hasNumber()) score++;
    if (this.hasSpecial()) score++;
    return score;
  }

  getStrengthLabel(): string {
    const score = this.getStrengthScore();
    if (score <= 1) return 'Faible';
    if (score === 2) return 'Passable';
    if (score === 3) return 'Bon';
    return 'Fort';
  }

  getStrengthClass(): string {
    const score = this.getStrengthScore();
    if (score <= 1) return 'weak';
    if (score === 2) return 'fair';
    if (score === 3) return 'good';
    return 'strong';
  }

  getStrengthWidth(): string {
    return (this.getStrengthScore() / 4 * 100) + '%';
  }

  register() {
    this.message = '';

    if (!this.username || !this.password || !this.confirmPassword) {
      this.message = 'Veuillez remplir tous les champs';
      return;
    }
    if (this.username.length < 3) {
      this.message = 'Le nom d\'utilisateur doit avoir au moins 3 caractères';
      return;
    }
    if (this.usernameAvailable === false) {
      this.message = 'Ce nom d\'utilisateur est déjà pris';
      return;
    }
    if (this.password.length < 8) {
      this.message = 'Le mot de passe doit avoir au moins 8 caractères';
      return;
    }
    if (!this.hasUppercase()) {
      this.message = 'Le mot de passe doit contenir au moins une majuscule';
      return;
    }
    if (!this.hasNumber()) {
      this.message = 'Le mot de passe doit contenir au moins un chiffre';
      return;
    }
    if (!this.hasSpecial()) {
      this.message = 'Le mot de passe doit contenir au moins un caractère spécial (!@#$...)';
      return;
    }
    if (this.password !== this.confirmPassword) {
      this.message = 'Les mots de passe ne correspondent pas';
      return;
    }

    this.isLoading = true;

    this.bitcoinService.register(this.username, this.password).subscribe({
      next: (res) => {
        this.isLoading = false;
        localStorage.setItem('userId', this.username);
        localStorage.setItem('token', res.token);

        if (res.qrCodeUrl) {
          localStorage.setItem('mfaQrCodeUrl', res.qrCodeUrl);
          localStorage.setItem('mfaSecret', res.secret);
          localStorage.setItem('preAuthToken', res.token);
          this.router.navigate(['/mfa-setup']);
        } else {
          this.router.navigate(['/dashboard']);
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.message = err.error?.message || 'Inscription échouée. Réessayez.';
      }
    });
  }

  goToLogin() {
    this.router.navigate(['/login']);
  }
}
