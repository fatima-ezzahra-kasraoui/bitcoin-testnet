import { Component, OnDestroy, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as THREE from 'three';

@Component({
  selector: 'app-btc-coin-3d',
  standalone: true,
  imports: [CommonModule],
  template: `
    <canvas #canvas
      style="display:block;width:100%;height:100%;background:transparent;">
    </canvas>
  `
})
export class BtcCoin3DComponent implements AfterViewInit, OnDestroy {
  @ViewChild('canvas') canvasRef!: ElementRef<HTMLCanvasElement>;
  private cleanup: (() => void) | null = null;

  ngAfterViewInit() {
    const canvas = this.canvasRef.nativeElement;
    const parent = canvas.parentElement!;
    const W = parent.offsetWidth || 400;
    const H = parent.offsetHeight || 400;

    const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true });
    renderer.setSize(W, H);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setClearColor(0x000000, 0);
    renderer.shadowMap.enabled = true;

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(45, W / H, 0.1, 100);
    camera.position.set(0, 0.4, 4.5);

    const coinGroup = new THREE.Group();
    coinGroup.rotation.x = 0.3;

    const coinGeo = new THREE.CylinderGeometry(1.4, 1.4, 0.18, 64);
    const coinMat = new THREE.MeshStandardMaterial({
      color: 0xe6b955,
      metalness: 1.0,
      roughness: 0.18,
      emissive: new THREE.Color(0x3a2a08),
      emissiveIntensity: 0.4
    });
    const coin = new THREE.Mesh(coinGeo, coinMat);
    coin.castShadow = true;
    coinGroup.add(coin);

    const rimGeo = new THREE.TorusGeometry(1.32, 0.035, 16, 80);
    const rimMat = new THREE.MeshStandardMaterial({ color: 0xfbe48a, metalness: 1.0, roughness: 0.1 });
    const rimTop = new THREE.Mesh(rimGeo, rimMat);
    rimTop.position.y = 0.095;
    coinGroup.add(rimTop);
    const rimBot = new THREE.Mesh(rimGeo, rimMat);
    rimBot.position.y = -0.095;
    rimBot.rotation.x = Math.PI;
    coinGroup.add(rimBot);

    const symbolRingGeo = new THREE.RingGeometry(0.55, 0.7, 64);
    const symbolMat = new THREE.MeshStandardMaterial({ color: 0x241804, metalness: 0.9, roughness: 0.3, side: THREE.DoubleSide });
    const symbolRing = new THREE.Mesh(symbolRingGeo, symbolMat);
    symbolRing.position.set(0, 0.1, 0);
    symbolRing.rotation.x = -Math.PI / 2;
    coinGroup.add(symbolRing);

    const glyphRingGeo = new THREE.TorusGeometry(0.62, 0.04, 16, 64);
    const glyphMat = new THREE.MeshStandardMaterial({
      color: 0xfff2b8,
      metalness: 1.0,
      roughness: 0.05,
      emissive: new THREE.Color(0xd99a1a),
      emissiveIntensity: 0.6
    });
    const glyphRing = new THREE.Mesh(glyphRingGeo, glyphMat);
    glyphRing.position.set(0, 0.11, 0);
    glyphRing.rotation.x = -Math.PI / 2;
    coinGroup.add(glyphRing);

    scene.add(coinGroup);

    const orbitGroup = new THREE.Group();
    const nodeCount = 6;
    const nodeMat = new THREE.MeshStandardMaterial({
      color: 0xfbe48a,
      emissive: new THREE.Color(0xd99a1a),
      emissiveIntensity: 1.2
    });
    for (let i = 0; i < nodeCount; i++) {
      const angle = (i / nodeCount) * Math.PI * 2;
      const nodeGeo = new THREE.SphereGeometry(0.06, 16, 16);
      const node = new THREE.Mesh(nodeGeo, nodeMat);
      node.position.set(
        Math.cos(angle) * 2.4,
        Math.sin(angle * 1.3) * 0.4,
        Math.sin(angle) * 2.4
      );
      orbitGroup.add(node);
    }
    scene.add(orbitGroup);

    const particleCount = 80;
    const positions = new Float32Array(particleCount * 3);
    for (let i = 0; i < particleCount; i++) {
      const r = 3 + Math.random() * 3;
      const theta = Math.random() * Math.PI * 2;
      const phi = Math.acos(2 * Math.random() - 1);
      positions[i * 3]     = r * Math.sin(phi) * Math.cos(theta);
      positions[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta);
      positions[i * 3 + 2] = r * Math.cos(phi);
    }
    const particleGeo = new THREE.BufferGeometry();
    particleGeo.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    const particleMat = new THREE.PointsMaterial({
      size: 0.04,
      color: 0xf0c85a,
      transparent: true,
      opacity: 0.7,
      sizeAttenuation: true
    });
    const particles = new THREE.Points(particleGeo, particleMat);
    scene.add(particles);

    scene.add(new THREE.AmbientLight(0xffffff, 0.35));
    const dirLight1 = new THREE.DirectionalLight(0xfff2cc, 2.2);
    dirLight1.position.set(4, 5, 3);
    scene.add(dirLight1);
    const dirLight2 = new THREE.DirectionalLight(0xa0660a, 0.8);
    dirLight2.position.set(-3, -2, -2);
    scene.add(dirLight2);
    const pointLight = new THREE.PointLight(0xffcf6b, 1.4, 20);
    pointLight.position.set(0, 0, 3);
    scene.add(pointLight);

    let floatTime = 0;
    let animId: number;
    const clock = new THREE.Clock();

    const animate = () => {
      animId = requestAnimationFrame(animate);
      const delta = clock.getDelta();
      floatTime += delta;
      coinGroup.rotation.y += delta * 0.6;
      coinGroup.position.y = Math.sin(floatTime * 1.4) * 0.12;
      orbitGroup.rotation.y -= delta * 0.25;
      particles.rotation.y += delta * 0.05;
      renderer.render(scene, camera);
    };
    animate();

    const onResize = () => {
      const W2 = parent.offsetWidth;
      const H2 = parent.offsetHeight;
      renderer.setSize(W2, H2);
      camera.aspect = W2 / H2;
      camera.updateProjectionMatrix();
    };
    window.addEventListener('resize', onResize);

    this.cleanup = () => {
      cancelAnimationFrame(animId);
      window.removeEventListener('resize', onResize);
      try {
        renderer.dispose();
        coinGeo.dispose(); coinMat.dispose();
        rimGeo.dispose(); rimMat.dispose();
        particleGeo.dispose(); particleMat.dispose();
      } catch (e) {}
    };
  }

  ngOnDestroy() { this.cleanup?.(); }
}
