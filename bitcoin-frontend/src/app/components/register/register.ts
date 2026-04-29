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

  // Username availability check
  usernameAvailable: boolean | null = null;
  checkingUsername = false;

  constructor(private bitcoinService: BitcoinService, private router: Router) {}

  // Called when user leaves the username field (blur event)
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

  // Password strength helpers
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
    if (score <= 1) return 'Weak';
    if (score === 2) return 'Fair';
    if (score === 3) return 'Good';
    return 'Strong';
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
      this.message = 'Please fill in all fields';
      return;
    }
    if (this.username.length < 3) {
      this.message = 'Username must be at least 3 characters';
      return;
    }
    if (this.usernameAvailable === false) {
      this.message = 'This username is already taken';
      return;
    }
    if (this.password.length < 8) {
      this.message = 'Password must be at least 8 characters';
      return;
    }
    if (!this.hasUppercase()) {
      this.message = 'Password must contain at least one uppercase letter';
      return;
    }
    if (!this.hasNumber()) {
      this.message = 'Password must contain at least one number';
      return;
    }
    if (!this.hasSpecial()) {
      this.message = 'Password must contain at least one special character (!@#$...)';
      return;
    }
    if (this.password !== this.confirmPassword) {
      this.message = 'Passwords do not match';
      return;
    }

    this.isLoading = true;

    this.bitcoinService.register(this.username, this.password).subscribe({
      next: (res) => {
        this.isLoading = false;
        localStorage.setItem('token', res.token);
        localStorage.setItem('userId', this.username);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading = false;
        // Show real backend message on register — safe to do here
        this.message = err.error?.message || 'Registration failed. Please try again.';
      }
    });
  }

  goToLogin() {
    this.router.navigate(['/login']);
  }
}