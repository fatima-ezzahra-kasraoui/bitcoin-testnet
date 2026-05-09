import { Component, AfterViewInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { BitcoinService } from '../../services/bitcoin';
import * as THREE from 'three';
import { gsap } from 'gsap';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent implements AfterViewInit, OnDestroy {
  @ViewChild('particlesCanvas') particlesCanvasRef!: ElementRef<HTMLCanvasElement>;

  username = '';
  password = '';
  totpCode = '';

  step: 'credentials' | 'totp' = 'credentials';
  message = '';
  isLoading = false;

  private preAuthToken = '';
  private threeCleanup: (() => void) | null = null;

  features = [
    'Multi-wallet AES-256 vault',
    'Real-time Kafka transaction stream',
    'ECDSA message signing & verification',
    'Behavioral anomaly detection engine',
    'Google Authenticator MFA'
  ];

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

  login() {
    if (!this.username || !this.password) {
      this.message = 'Veuillez remplir tous les champs';
      return;
    }
    this.isLoading = true;
    this.message = '';

    this.bitcoinService.login(this.username, this.password).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res.mfaRequired) {
          this.preAuthToken = res.preAuthToken;
          this.step = 'totp';
        } else {
          localStorage.setItem('token', res.token);
          localStorage.setItem('userId', this.username);
          this.router.navigate(['/dashboard']);
        }
      },
      error: () => {
        this.isLoading = false;
        this.message = 'Identifiants invalides';
      }
    });
  }

  verifyTotp() {
    if (!this.totpCode || this.totpCode.length !== 6) {
      this.message = 'Entrez le code à 6 chiffres';
      return;
    }
    if (!/^\d{6}$/.test(this.totpCode)) {
      this.message = 'Le code ne doit contenir que des chiffres';
      return;
    }
    this.isLoading = true;
    this.message = '';

    this.bitcoinService.verifyMfa(this.totpCode, this.preAuthToken).subscribe({
      next: (res) => {
        this.isLoading = false;
        localStorage.setItem('token', res.token);
        localStorage.setItem('userId', this.username);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading = false;
        this.message = err.error?.message || 'Code invalide. Réessayez.';
        this.totpCode = '';
      }
    });
  }

  backToCredentials() {
    this.step = 'credentials';
    this.totpCode = '';
    this.message = '';
    this.preAuthToken = '';
  }

  goToRegister() {
    this.router.navigate(['/register']);
  }
}
