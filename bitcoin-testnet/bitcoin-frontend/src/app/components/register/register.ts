import { Component, AfterViewInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { BitcoinService } from '../../services/bitcoin';
import * as THREE from 'three';
import { gsap } from 'gsap';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class RegisterComponent implements AfterViewInit, OnDestroy {
  @ViewChild('particlesCanvas') particlesCanvasRef!: ElementRef<HTMLCanvasElement>;

  username = '';
  password = '';
  confirmPassword = '';
  message = '';
  isLoading = false;

  usernameAvailable: boolean | null = null;
  checkingUsername = false;

  private threeCleanup: (() => void) | null = null;

  constructor(private bitcoinService: BitcoinService, private router: Router) {}

  ngAfterViewInit() {
    gsap.fromTo('.cascade-1', { x: -40, opacity: 0 }, { x: 0, opacity: 1, duration: 0.9, ease: 'power3.out' });
    gsap.fromTo('.cascade-2', { x: 40, opacity: 0 }, { x: 0, opacity: 1, duration: 0.9, ease: 'power3.out' });

    setTimeout(() => {
      const canvas = this.particlesCanvasRef?.nativeElement;
      if (!canvas || window.innerWidth < 768) return;
      const parent = canvas.parentElement!;
      const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: false });
      renderer.setSize(parent.offsetWidth, parent.offsetHeight);
      renderer.setClearColor(0x000000, 0);
      const scene = new THREE.Scene();
      const camera = new THREE.PerspectiveCamera(60, parent.offsetWidth / parent.offsetHeight, 0.1, 100);
      camera.position.z = 4;
      const count = 100;
      const positions = new Float32Array(count * 3);
      const speeds = new Float32Array(count);
      for (let i = 0; i < count; i++) {
        positions[i*3] = (Math.random()-0.5)*8;
        positions[i*3+1] = (Math.random()-0.5)*8;
        positions[i*3+2] = (Math.random()-0.5)*3;
        speeds[i] = 0.003 + Math.random()*0.005;
      }
      const geo = new THREE.BufferGeometry();
      geo.setAttribute('position', new THREE.BufferAttribute(positions, 3));
      const mat = new THREE.PointsMaterial({ color: 0xC9A84C, size: 0.03, transparent: true, opacity: 0.5 });
      const particles = new THREE.Points(geo, mat);
      scene.add(particles);
      let pid: number;
      const tick = () => {
        pid = requestAnimationFrame(tick);
        const pos = geo.attributes['position'].array as Float32Array;
        for (let i = 0; i < count; i++) {
          pos[i*3+1] += speeds[i];
          if (pos[i*3+1] > 4) pos[i*3+1] = -4;
        }
        geo.attributes['position'].needsUpdate = true;
        particles.rotation.y += 0.0005;
        renderer.render(scene, camera);
      };
      tick();
      this.threeCleanup = () => { cancelAnimationFrame(pid); try { renderer.dispose(); geo.dispose(); mat.dispose(); } catch(e){} };
    }, 200);
  }

  ngOnDestroy() {
    this.threeCleanup?.();
  }

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
