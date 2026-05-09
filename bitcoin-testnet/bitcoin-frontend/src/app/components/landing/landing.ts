import { Component, AfterViewInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import * as THREE from 'three';
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
gsap.registerPlugin(ScrollTrigger);

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './landing.html',
  styleUrl: './landing.css'
})
export class LandingComponent implements AfterViewInit, OnDestroy {
  @ViewChild('coinCanvas') coinCanvasRef!: ElementRef<HTMLCanvasElement>;
  private cleanup: (() => void) | null = null;

  constructor(private router: Router) {}

  goToLogin() { this.router.navigate(['/login']); }
  goToRegister() { this.router.navigate(['/register']); }

  ngAfterViewInit() {
    this.initCoin();
    this.initAnimations();
  }

  private initCoin() {
    const canvas = this.coinCanvasRef?.nativeElement;
    if (!canvas) return;
    const parent = canvas.parentElement!;
    const W = parent.offsetWidth || 500;
    const H = parent.offsetHeight || 500;
    const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true });
    renderer.setSize(W, H);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setClearColor(0x000000, 0);
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(45, W / H, 0.1, 100);
    camera.position.set(0, 0.4, 4.5);
    const coinGroup = new THREE.Group();
    coinGroup.rotation.x = 0.3;
    const coinGeo = new THREE.CylinderGeometry(1.4, 1.4, 0.18, 64);
    const coinMat = new THREE.MeshStandardMaterial({ color: 0xe6b955, metalness: 1.0, roughness: 0.18, emissive: new THREE.Color(0x3a2a08), emissiveIntensity: 0.4 });
    coinGroup.add(new THREE.Mesh(coinGeo, coinMat));
    const rimGeo = new THREE.TorusGeometry(1.32, 0.035, 16, 80);
    const rimMat = new THREE.MeshStandardMaterial({ color: 0xfbe48a, metalness: 1.0, roughness: 0.1 });
    const rimTop = new THREE.Mesh(rimGeo, rimMat); rimTop.position.y = 0.095; coinGroup.add(rimTop);
    const rimBot = new THREE.Mesh(rimGeo, rimMat); rimBot.position.y = -0.095; rimBot.rotation.x = Math.PI; coinGroup.add(rimBot);
    const glyphGeo = new THREE.TorusGeometry(0.62, 0.04, 16, 64);
    const glyphMat = new THREE.MeshStandardMaterial({ color: 0xfff2b8, metalness: 1.0, roughness: 0.05, emissive: new THREE.Color(0xd99a1a), emissiveIntensity: 0.6 });
    const glyph = new THREE.Mesh(glyphGeo, glyphMat); glyph.position.set(0, 0.11, 0); glyph.rotation.x = -Math.PI / 2; coinGroup.add(glyph);
    scene.add(coinGroup);
    const orbitGroup = new THREE.Group();
    const nodeMat = new THREE.MeshStandardMaterial({ color: 0xfbe48a, emissive: new THREE.Color(0xd99a1a), emissiveIntensity: 1.2 });
    for (let i = 0; i < 6; i++) {
      const a = (i / 6) * Math.PI * 2;
      const node = new THREE.Mesh(new THREE.SphereGeometry(0.06, 16, 16), nodeMat);
      node.position.set(Math.cos(a) * 2.4, Math.sin(a * 1.3) * 0.4, Math.sin(a) * 2.4);
      orbitGroup.add(node);
    }
    scene.add(orbitGroup);
    const pCount = 80;
    const pPos = new Float32Array(pCount * 3);
    for (let i = 0; i < pCount; i++) {
      const r = 3 + Math.random() * 3, t = Math.random() * Math.PI * 2, p = Math.acos(2 * Math.random() - 1);
      pPos[i*3] = r*Math.sin(p)*Math.cos(t); pPos[i*3+1] = r*Math.sin(p)*Math.sin(t); pPos[i*3+2] = r*Math.cos(p);
    }
    const pGeo = new THREE.BufferGeometry(); pGeo.setAttribute('position', new THREE.BufferAttribute(pPos, 3));
    const pts = new THREE.Points(pGeo, new THREE.PointsMaterial({ size: 0.04, color: 0xf0c85a, transparent: true, opacity: 0.7 }));
    scene.add(pts);
    scene.add(new THREE.AmbientLight(0xffffff, 0.35));
    const dl1 = new THREE.DirectionalLight(0xfff2cc, 2.2); dl1.position.set(4, 5, 3); scene.add(dl1);
    const dl2 = new THREE.DirectionalLight(0xa0660a, 0.8); dl2.position.set(-3, -2, -2); scene.add(dl2);
    const pl = new THREE.PointLight(0xffcf6b, 1.4, 20); pl.position.set(0, 0, 3); scene.add(pl);
    let ft = 0, id: number;
    const clock = new THREE.Clock();
    const animate = () => {
      id = requestAnimationFrame(animate);
      const d = clock.getDelta(); ft += d;
      coinGroup.rotation.y += d * 0.6;
      coinGroup.position.y = Math.sin(ft * 1.4) * 0.12;
      orbitGroup.rotation.y -= d * 0.25;
      pts.rotation.y += d * 0.05;
      renderer.render(scene, camera);
    };
    animate();
    const onResize = () => { renderer.setSize(parent.offsetWidth, parent.offsetHeight); camera.aspect = parent.offsetWidth / parent.offsetHeight; camera.updateProjectionMatrix(); };
    window.addEventListener('resize', onResize);
    this.cleanup = () => { cancelAnimationFrame(id); window.removeEventListener('resize', onResize); try { renderer.dispose(); } catch(e){} };
  }

  private initAnimations() {
    setTimeout(() => {
      gsap.fromTo('.hero-left', { x: -60, opacity: 0 }, { x: 0, opacity: 1, duration: 1, ease: 'power3.out' });
      gsap.fromTo('.hero-right', { x: 60, opacity: 0 }, { x: 0, opacity: 1, duration: 1, ease: 'power3.out' });
      gsap.fromTo('.stat-item', { y: 30, opacity: 0 }, { y: 0, opacity: 1, stagger: 0.1, duration: 0.6, delay: 0.5 });
      gsap.fromTo('.feat-card', { y: 40, opacity: 0 }, { y: 0, opacity: 1, stagger: 0.08, duration: 0.6, scrollTrigger: { trigger: '.features-section', start: 'top 80%' } });
      gsap.fromTo('.security-item', { x: -30, opacity: 0 }, { x: 0, opacity: 1, stagger: 0.12, duration: 0.6, scrollTrigger: { trigger: '.security-section', start: 'top 80%' } });
      gsap.fromTo('.partner-logo', { y: 20, opacity: 0 }, { y: 0, opacity: 1, stagger: 0.06, duration: 0.5, scrollTrigger: { trigger: '.partners-section', start: 'top 85%' } });
      gsap.fromTo('.review-card', { y: 30, opacity: 0 }, { y: 0, opacity: 1, stagger: 0.1, duration: 0.6, scrollTrigger: { trigger: '.reviews-section', start: 'top 80%' } });
    }, 100);
  }

  ngOnDestroy() { this.cleanup?.(); ScrollTrigger.getAll().forEach(t => t.kill()); }
}
