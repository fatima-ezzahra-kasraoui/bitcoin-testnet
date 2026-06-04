# ₿ Bitcoin Testnet Wallet

Une application web full-stack de portefeuille Bitcoin sur le réseau **TestNet3**, avec détection d'anomalies en temps réel, streaming Kafka et interface Angular moderne.

---

## Table des matières

- [Aperçu](#aperçu)
- [Stack technique](#stack-technique)
- [Architecture du projet](#architecture-du-projet)
- [Prérequis](#prérequis)
- [Installation & Démarrage](#installation--démarrage)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Fonctionnalités](#fonctionnalités)
- [Sécurité & Détection d'anomalies](#sécurité--détection-danomalies)
- [Base de données](#base-de-données)
- [Kafka](#kafka)
- [Frontend Angular](#frontend-angular)


---

## Aperçu

Bitcoin Testnet Wallet est une application de démonstration permettant de :

- Créer et gérer des portefeuilles Bitcoin P2PKH sur le réseau **TestNet3**
- Envoyer et recevoir des transactions via l'API **mempool.space**
- Détecter automatiquement les transactions suspectes grâce à un moteur de règles (score de risque)
- Visualiser les insights de sécurité et exporter des rapports PDF
- Recevoir des notifications en temps réel sur les mises à jour de transactions

> ⚠️ Ce projet utilise exclusivement le réseau **Bitcoin TestNet3**. Aucun vrai bitcoin n'est utilisé.

---

## Stack technique

| Couche | Technologie | Version | Rôle |
|--------|-------------|---------|------|
| Backend | Spring Boot | 3.5.x | API REST, sécurité, scheduling |
| Frontend | Angular | 21 (standalone) | SPA |
| Base de données | MongoDB | 7.x | Persistance principale |
| Messaging | Apache Kafka | — | Streaming async des transactions |
| Bitcoin | bitcoinj | 0.17-alpha5 | Génération de wallet, signature, SPV |
| Bitcoin API | mempool.space | — | UTXO, broadcast, confirmations, faucet |
| Auth | JJWT | 0.12.3 | JWT (TTL 24h) |
| OTP | java-otp | 0.4.0 | MFA TOTP |
| Charts | Chart.js | 4.x | Graphiques de sécurité |
| PDF | jsPDF | 4.x | Rapport de sécurité (2 pages) |
| QR Code | qrcode / angularx-qrcode | — | Génération QR des adresses |
| 3D | Three.js | 0.184 | Coin 3D animé |
| Animations | GSAP + tsParticles | — | Effets visuels landing page |

---

## Architecture du projet

```
bitcoin-testnet-aya-branch/
├── bitcoin-testnet/
│   ├── bitcoin-testnet/          # Backend Spring Boot
│   │   ├── src/main/java/com/bitcoin/bitcoin_testnet/
│   │   │   ├── config/           # SecurityConfig (CORS, JWT filter)
│   │   │   ├── controller/       # Auth, Wallet, Transaction, Security, Message, Notification, Contact
│   │   │   ├── dto/              # AuthRequest, TransactionRequest, WalletRequest, MFA...
│   │   │   ├── exception/        # GlobalExceptionHandler
│   │   │   ├── filter/           # JwtFilter (OncePerRequestFilter)
│   │   │   ├── model/            # User, Wallet, Transaction, Notification, Contact, Message
│   │   │   ├── repository/       # Spring Data MongoDB repositories
│   │   │   └── service/          # Auth, Wallet, Transaction, AnomalyDetection, Kafka, Bitcoin...
│   │   └── src/main/resources/
│   │       └── application.properties
│   │
│   └── bitcoin-frontend/         # Frontend Angular 21
│       └── src/app/
│           ├── components/       # login, dashboard, wallet, security, profile, contacts, mfa-setup
│           ├── services/         # bitcoin.ts (tous les appels HTTP)
│           └── interceptors/     # auth.interceptor.ts (injection JWT)
│
└── PROJECT_MEMORY.md
```

---

## Prérequis

- **Java 25** (JDK)
- **Maven 3.9+**
- **Node.js 20+** et **npm 10+**
- **MongoDB 7.x** (instance locale ou Docker)
- **Apache Kafka** avec Zookeeper (ou KRaft)
- Connexion internet (pour l'API mempool.space TestNet)

---

## Installation & Démarrage

### 1. Cloner le projet

```bash
git clone <url-du-repo>
cd bitcoin-testnet-aya-branch
```

### 2. Démarrer MongoDB

```bash
# Avec Docker
docker run -d -p 27017:27017 --name mongodb mongo:7

# Ou démarrer le service local
mongod --dbpath /data/db
```

### 3. Démarrer Kafka

```bash
# Avec Docker Compose (exemple)
docker-compose up -d zookeeper kafka

# Ou manuellement
bin/zookeeper-server-start.sh config/zookeeper.properties &
bin/kafka-server-start.sh config/server.properties &
```

Le topic `blockchain-transactions` est créé automatiquement au démarrage.

### 4. Démarrer le backend

```bash
cd bitcoin-testnet/bitcoin-testnet
./mvnw spring-boot:run
```

Le serveur démarre sur **http://localhost:8081**

### 5. Démarrer le frontend

```bash
cd bitcoin-testnet/bitcoin-frontend
npm install
npm start
```

L'application est disponible sur **http://localhost:4200**

---

## Configuration

Le fichier `application.properties` se trouve dans `bitcoin-testnet/src/main/resources/` :

```properties
server.port=8081

# MongoDB
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=bitcoin_testnet

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=bitcoin-group

# Sérialisation des dates
spring.jackson.serialization.write-dates-as-timestamps=false
```

### Ports & URLs de services

| Service | Port | URL |
|---------|------|-----|
| Backend Spring Boot | 8081 | http://localhost:8081/api |
| Frontend Angular | 4200 | http://localhost:4200 |
| MongoDB | 27017 | localhost/bitcoin_testnet |
| Kafka | 9092 | localhost:9092 |
| Bitcoin TestNet API | 443 | https://mempool.space/testnet/api |

---

## API Reference

### Authentification — Public

| Méthode | URL | Description |
|---------|-----|-------------|
| `POST` | `/api/auth/register` | Créer un compte → retourne JWT |
| `POST` | `/api/auth/login` | Connexion → retourne JWT |
| `POST` | `/api/auth/change-password` | Changer le mot de passe (JWT requis) |
| `GET` | `/api/status` | Infos de connexion aux peers TestNet |

### Wallets — JWT requis

| Méthode | URL | Description |
|---------|-----|-------------|
| `POST` | `/api/wallets` | Créer un wallet P2PKH (bitcoinj TestNet3) |
| `GET` | `/api/wallets/{userId}` | Lister les wallets d'un utilisateur |
| `GET` | `/api/wallets/{address}/balance` | Solde via mempool.space |
| `POST` | `/api/wallets/{address}/faucet` | Demander 10 000 sat depuis le faucet |
| `PATCH` | `/api/wallets/{address}/label` | Renommer un wallet |
| `DELETE` | `/api/wallets/{address}` | Supprimer un wallet |

### Transactions — JWT requis

| Méthode | URL | Description |
|---------|-----|-------------|
| `POST` | `/api/transactions` | Envoyer une transaction (peut retourner `AWAITING_CONFIRMATION`) |
| `GET` | `/api/transactions/pending-confirmation` | Transactions bloquées en attente de validation |
| `GET` | `/api/transactions/{address}` | Historique (filtres : status, from, to, sort) |
| `GET` | `/api/transactions/{address}/export` | Export CSV |
| `POST` | `/api/transactions/{txId}/confirm` | Confirmer et broadcaster une transaction bloquée |
| `POST` | `/api/transactions/{txId}/cancel` | Annuler une transaction bloquée |

> **Note :** `{txId}` dans `/confirm` et `/cancel` est l'`_id` MongoDB, **pas** le txId blockchain.

### Autres endpoints — JWT requis

| Méthode | URL | Description |
|---------|-----|-------------|
| `POST` | `/api/messages/sign` | Signer un message avec ECKey bitcoinj |
| `GET` | `/api/notifications` | Notifications non lues |
| `PATCH` | `/api/notifications/{id}/read` | Marquer une notification comme lue |
| `GET` | `/api/security/insights` | Données de risque agrégées sur 30 jours |
| `GET` | `/api/security/score-history` | Top 20 alertes de sécurité |
| `GET` | `/api/contacts` | Liste des contacts sauvegardés |
| `POST` | `/api/contacts` | Ajouter un contact `{label, address}` |
| `DELETE` | `/api/contacts/{id}` | Supprimer un contact |

---

## Fonctionnalités

### ✅ Implémentées

1. **Authentification JWT** — register, login, changement de mot de passe (TTL 24h)
2. **Gestion de wallets** — création P2PKH, suppression, renommage, solde, faucet TestNet
3. **Envoi de transactions** — signature bitcoinj + broadcast mempool.space
4. **Historique des transactions** — filtres statut/date/tri, export CSV
5. **Détection d'anomalies asynchrone** — 5 règles, `@Async`, notification `SECURITY_ALERT`
6. **Système de notifications** — `TX_UPDATE` + `SECURITY_ALERT`, badge non-lus, dropdown
7. **Dashboard de sécurité** — ring de risque, graphiques Chart.js, règles déclenchées, conseils
8. **Blocage de transactions** — gate `AWAITING_CONFIRMATION` (score ≥ 50)
9. **Modal de confirmation** — countdown (epoch ms backend), badge de risque, chips de règles
10. **Confirm / Cancel** — broadcast ou passage en `FAILED`
11. **Scheduler d'auto-annulation** — `AWAITING_CONFIRMATION` expirées → `FAILED` (toutes les 5 min)
12. **Badge de review** — badge orange navbar + scroll vers les transactions suspectes
13. **QR Code** — PNG 256px, copie d'adresse, téléchargement, fermeture Escape
14. **Rapport PDF** — 2 pages jsPDF : overview + conseils, nom `security-report-YYYY-MM-DD.pdf`
15. **Signature de message** — via bitcoinj ECKey
16. **Gestion de contacts** — label + adresse, autocomplétion dans le formulaire d'envoi
17. **Streaming Kafka** — publication à l'envoi/confirmation, consumer met à jour les confirmations blockchain
18. **Polling de confirmations blockchain** — mempool.space toutes les 30s, `CONFIRMED` à ≥ 3 confs
19. **Historique des scores de sécurité** — top 20 alertes avec txId, score, règles, date
20. **Setup MFA (TOTP)** — configuration via `java-otp`

---

## Sécurité & Détection d'anomalies

### Statuts des transactions

| Statut | Déclencheur |
|--------|-------------|
| `PENDING` | Transaction broadcastée, en attente de confirmations |
| `CONFIRMED` | ≥ 3 confirmations blockchain (polling toutes les 30s) |
| `FAILED` | 404 mempool.space OU annulée par l'utilisateur |
| `AWAITING_CONFIRMATION` | Score d'anomalie ≥ 50 — bloquée avant broadcast |

### Règles d'anomalie

| Règle | Points | Condition |
|-------|--------|-----------|
| `HIGH_AMOUNT` | +30 | Montant > 3× la moyenne des 10 dernières transactions confirmées |
| `NEW_DESTINATION` | +25 | Première transaction vers cette adresse |
| `RAPID_FIRE` | +20 | ≥ 3 transactions depuis la même adresse en moins de 2 minutes |
| `OFF_HOURS` | +15 | Heure actuelle écarte de > 3h du créneau habituel (20 dernières tx) |
| `ROUND_AMOUNT` | +10 | Montant multiple de 1000 sat ET ≥ 10 000 sat |

### Logique de traitement

- **Score ≥ 50** → Transaction bloquée (`AWAITING_CONFIRMATION`), non broadcastée. Expiration dans **10 minutes**.
- **Score ≥ 30** → Notification `SECURITY_ALERT` créée de façon asynchrone (`@Async`).
- `analyzeSync` est appelé **de façon synchrone avant** le broadcast.
- `analyze` est appelé **de façon asynchrone après** le broadcast ou la confirmation.
- Les clés privées des wallets sont stockées chiffrées en **AES** dans MongoDB (déchiffrées uniquement à la signature).

---

## Base de données

### Collections MongoDB

| Collection | Champs clés |
|------------|-------------|
| `users` | `id`, `username`, `password` (BCrypt), `role` |
| `wallets` | `id`, `userId`, `address`, `publicKey`, `encryptedPrivateKey`, `label`, `createdAt` |
| `transactions` | `id`, `txId` (blockchain), `userId`, `fromAddress`, `toAddress`, `amount` (Long sat), `status`, `confirmations`, `createdAt`, `confirmedAt`, `requiresConfirmation`, `confirmationExpiresAt`, `riskScore`, `triggeredRules` |
| `notifications` | `id`, `userId`, `message`, `txId`, `read`, `createdAt`, `type` (`TX_UPDATE`\|`SECURITY_ALERT`), `riskScore`, `triggeredRules` |
| `contacts` | `id`, `userId`, `label`, `address` |

---

## Kafka

- **Topic** : `blockchain-transactions`
- **Group** : `bitcoin-group`
- **Bootstrap server** : `localhost:9092`

| Service | Rôle |
|---------|------|
| `KafkaProducerService` | Publie le `txId` lors d'un envoi ou d'une confirmation |
| `KafkaConsumerService` | Consomme les événements + schedulers : polling blockchain (30s) et auto-annulation AWAITING_CONFIRMATION (5 min) |

---

## Frontend Angular

### Composants principaux

| Composant | Route | Description |
|-----------|-------|-------------|
| `LandingComponent` | `/` | Page d'accueil animée (GSAP, tsParticles, coin 3D Three.js) |
| `LoginComponent` | `/login` | Connexion / inscription |
| `DashboardComponent` | `/dashboard` | Vue globale : wallets, soldes, transactions, badge pending (polling 15s) |
| `WalletComponent` | `/wallet?address=` | Envoi, confirmation/annulation, QR code, signature de message |
| `SecurityComponent` | `/security` | Insights, graphiques Chart.js, génération rapport PDF |
| `ProfileComponent` | `/profile` | Stats portfolio, changement de mot de passe |
| `ContactsComponent` | `/contacts` | CRUD contacts, utilisés en autocomplétion |
| `MfaSetupComponent` | `/mfa-setup` | Configuration MFA TOTP |

Tous les composants sont **standalone** (sans NgModule). Le token JWT est stocké dans `localStorage` et injecté automatiquement via `auth.interceptor.ts`.

---

