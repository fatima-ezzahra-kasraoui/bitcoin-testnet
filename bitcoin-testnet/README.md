# Bitcoin TestNet Web Application

Une application web full-stack permettant d'interagir avec le réseau Bitcoin TestNet via un navigateur web.

---

## 📋 Table des matières

- [Présentation](#présentation)
- [Stack Technique](#stack-technique)
- [Architecture](#architecture)
- [Fonctionnalités](#fonctionnalités)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Lancer le projet](#lancer-le-projet)
- [API Endpoints](#api-endpoints)
- [Sécurité](#sécurité)
- [Structure du projet](#structure-du-projet)

---

## 🎯 Présentation

Bitcoin TestNet Web Application est un **client web blockchain** qui permet aux utilisateurs de :

- Créer des adresses Bitcoin sur le réseau TestNet
- Consulter leurs portefeuilles
- Initier des transactions Bitcoin (argent fictif)
- Signer des messages avec leur clé privée (cryptographie ECDSA)

> ⚠️ Le réseau TestNet utilise de l'argent fictif — c'est un terrain d'entraînement pour développeurs.

---

## 🛠️ Stack Technique

| Couche | Technologie | Version | Rôle |
|--------|------------|---------|------|
| Frontend | Angular | 21 | Interface utilisateur |
| Backend | Spring Boot | 3.5.13 | API REST |
| Langage | Java | 25 | Logique métier |
| Blockchain | BitcoinJ | 0.16.3 | Connexion réseau Bitcoin |
| Base de données | MongoDB | 7 | Stockage des données |
| Messaging | Apache Kafka | 3.9.0 | Événements asynchrones |
| Sécurité | Spring Security + JWT | - | Authentification |
| Chiffrement | AES-256 | - | Protection des clés privées |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│           UTILISATEUR                    │
│         (Navigateur web)                 │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│         FRONTEND (Angular 21)            │
│      http://localhost:4200               │
│   Login | Dashboard | Wallet | Messages  │
└─────────────────┬───────────────────────┘
                  │ API REST + JWT
                  ▼
┌─────────────────────────────────────────┐
│       BACKEND (Spring Boot 3.5.13)       │
│         http://localhost:8081            │
│                                          │
│  ┌──────────┐ ┌───────┐ ┌───────────┐  │
│  │ BitcoinJ │ │ Kafka │ │  MongoDB  │  │
│  └──────────┘ └───────┘ └───────────┘  │
│       Spring Security + JWT              │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│       RÉSEAU BITCOIN TESTNET             │
│         (Blockchain distribuée)          │
└─────────────────────────────────────────┘
```

---

## ✨ Fonctionnalités

### 1. Authentification JWT
- Inscription avec mot de passe chiffré (BCrypt)
- Connexion avec génération de token JWT (24h)
- Protection des routes par token JWT
- Filtre JWT sur toutes les requêtes sécurisées

### 2. Gestion des Wallets
- Génération d'adresses Bitcoin TestNet via BitcoinJ
- Chiffrement AES-256 des clés privées
- Stockage sécurisé dans MongoDB
- Liste des wallets par utilisateur

### 3. Transactions Bitcoin
- Création de transactions entre adresses
- Gestion asynchrone via Apache Kafka
- Statuts : PENDING → CONFIRMED → FAILED
- Historique des transactions

### 4. Signature de Messages
- Signature numérique ECDSA avec clé privée
- Vérification de signature avec adresse publique
- Preuve d'identité cryptographique

### 5. Sécurité
- Filtre JWT sur toutes les routes protégées
- Chiffrement AES-256/CBC des clés privées Bitcoin
- BCrypt pour les mots de passe
- CORS configuré pour Angular
- Validation des données entrantes (DTOs)
- Gestion globale des erreurs

---

## 📦 Prérequis

- **Java** 25+
- **Node.js** 22+
- **MongoDB** 7+ (port 27017)
- **Apache Kafka** 3.9.0 (port 9092)
- **IntelliJ IDEA** 2025+
- **VS Code** (pour Angular)

---

## 🚀 Installation

### 1. Cloner / Ouvrir le projet Backend
```
Chemin : C:\bitcoin-testnet\bitcoin-testnet
```

### 2. Installer les dépendances Backend
Ouvrir dans IntelliJ et Maven télécharge automatiquement les dépendances.

### 3. Installer le Frontend
```cmd
cd C:\bitcoin-frontend
npm install
```

### 4. Installer Kafka
```
Chemin : C:\kafka
```

---

## ▶️ Lancer le projet

### Étape 1 — Lancer Zookeeper
```cmd
cd C:\kafka
bin\windows\zookeeper-server-start.bat config\zookeeper.properties
```

### Étape 2 — Lancer Kafka
```cmd
cd C:\kafka
bin\windows\kafka-server-start.bat config\server.properties
```

### Étape 3 — Lancer le Backend
Ouvrir IntelliJ et lancer `BitcoinTestnetApplication.java` ▶️

### Étape 4 — Lancer le Frontend
```cmd
cd C:\bitcoin-frontend
npx ng serve
```

### Étape 5 — Accéder à l'application
- **Frontend** : http://localhost:4200
- **Backend API** : http://localhost:8081

---

## 🔌 API Endpoints

### Authentification (publique)
| Méthode | URL | Description |
|---------|-----|-------------|
| POST | /api/auth/register | Créer un compte |
| POST | /api/auth/login | Se connecter |

### Wallets (JWT requis)
| Méthode | URL | Description |
|---------|-----|-------------|
| POST | /api/wallets | Créer un wallet |
| GET | /api/wallets/{userId} | Lister les wallets |
| GET | /api/wallets/{address}/balance | Voir le solde |

### Transactions (JWT requis)
| Méthode | URL | Description |
|---------|-----|-------------|
| POST | /api/transactions | Créer une transaction |
| GET | /api/transactions/{address} | Historique |

### Messages (JWT requis)
| Méthode | URL | Description |
|---------|-----|-------------|
| POST | /api/messages/sign | Signer un message |
| GET | /api/messages/{address} | Lister les messages |

### Utilisation avec token JWT
```
Headers:
Authorization: Bearer eyJhbGciOiJIUzM4NCJ9...
```

---

## 🔐 Sécurité

### JWT (JSON Web Token)
```
Header: algorithme HS384
Payload: username + expiration (24h)
Signature: clé secrète HMAC
```

### AES-256 (Clés privées Bitcoin)
```
Format stocké: AES256:{IV_base64}:{ciphertext_base64}
Mode: CBC (Cipher Block Chaining)
Padding: PKCS5
```

### BCrypt (Mots de passe)
```
Les mots de passe ne sont jamais stockés en clair
BCrypt hash: $2a$10$...
```

### Ce que l'application ne fait JAMAIS
- ❌ Stocker une clé privée en clair
- ❌ Envoyer une clé privée au frontend
- ❌ Logger une clé privée
- ❌ Exposer une clé privée dans l'API

---

## 📁 Structure du projet

### Backend
```
com.bitcoin.bitcoin_testnet
├── config/
│   └── SecurityConfig.java          # Configuration Spring Security + CORS
├── controller/
│   ├── AuthController.java          # Routes /api/auth
│   ├── WalletController.java        # Routes /api/wallets
│   ├── TransactionController.java   # Routes /api/transactions
│   └── MessageController.java       # Routes /api/messages
├── service/
│   ├── AuthService.java             # Logique login/register
│   ├── JwtService.java              # Génération/validation JWT
│   ├── WalletService.java           # Logique wallets Bitcoin
│   ├── TransactionService.java      # Logique transactions
│   ├── MessageService.java          # Logique signature ECDSA
│   ├── BitcoinService.java          # Connexion BitcoinJ
│   ├── EncryptionService.java       # Chiffrement AES-256
│   ├── KafkaProducerService.java    # Publication événements Kafka
│   └── KafkaConsumerService.java    # Consommation événements Kafka
├── model/
│   ├── Wallet.java                  # Document MongoDB wallet
│   ├── Transaction.java             # Document MongoDB transaction
│   ├── Message.java                 # Document MongoDB message
│   └── User.java                    # Document MongoDB utilisateur
├── repository/
│   ├── WalletRepository.java        # Accès MongoDB wallets
│   ├── TransactionRepository.java   # Accès MongoDB transactions
│   ├── MessageRepository.java       # Accès MongoDB messages
│   └── UserRepository.java          # Accès MongoDB utilisateurs
├── dto/
│   ├── WalletRequest.java           # Validation création wallet
│   ├── TransactionRequest.java      # Validation création transaction
│   ├── MessageRequest.java          # Validation signature message
│   └── AuthRequest.java             # Validation login/register
├── filter/
│   └── JwtFilter.java               # Filtre JWT sur chaque requête
└── exception/
    └── GlobalExceptionHandler.java  # Gestion globale des erreurs
```

### Frontend
```
src/app/
├── components/
│   ├── login/
│   │   ├── login.ts                 # Logique connexion
│   │   ├── login.html               # Page login
│   │   └── login.css                # Style login
│   ├── dashboard/
│   │   ├── dashboard.ts             # Logique dashboard
│   │   ├── dashboard.html           # Page dashboard
│   │   └── dashboard.css            # Style dashboard
│   └── wallet/
│       ├── wallet.ts                # Logique transactions/messages
│       ├── wallet.html              # Page wallet
│       └── wallet.css               # Style wallet
├── services/
│   └── bitcoin.ts                   # Service HTTP vers Spring Boot
├── app.routes.ts                    # Configuration des routes
├── app.config.ts                    # Configuration Angular
└── app.html                         # Template principal
```

---

## 📊 Modèle de données MongoDB

### Collection: wallets
```json
{
  "_id": "ObjectId",
  "userId": "alice",
  "address": "mnZ1VeE1ZxW99ZfdHkdw76SypHybF4iwRr",
  "publicKey": "0324777f1a64fbac77e3...",
  "encryptedPrivateKey": "AES256:{iv}:{ciphertext}",
  "label": "Mon Wallet",
  "createdAt": "2026-04-20T09:23:02.143Z"
}
```

### Collection: transactions
```json
{
  "_id": "ObjectId",
  "txId": "a1b2c3d4...",
  "fromAddress": "mnZ1VeE1...",
  "toAddress": "tb1qst0y94...",
  "amount": 5000,
  "status": "PENDING | CONFIRMED | FAILED",
  "confirmations": 0,
  "createdAt": "2026-04-20T09:23:02.143Z",
  "confirmedAt": null
}
```

### Collection: messages
```json
{
  "_id": "ObjectId",
  "address": "mnZ1VeE1...",
  "message": "Bonjour Bitcoin!",
  "signature": "IOFAcwf5OCdX1AQDp...",
  "verified": true,
  "createdAt": "2026-04-20T09:23:02.143Z"
}
```

### Collection: users
```json
{
  "_id": "ObjectId",
  "username": "alice",
  "password": "$2a$10$xyz...",
  "role": "USER"
}
```

---

## ⚙️ Configuration (application.properties)

```properties
server.port=8081

# MongoDB
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=bitcoin_testnet

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.consumer.group-id=bitcoin-group
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
```

---

## 🎓 Concepts appris

- **Spring Boot** : Framework Java pour applications web
- **REST API** : Architecture d'échange de données HTTP/JSON
- **MongoDB** : Base de données NoSQL orientée documents
- **Apache Kafka** : Messagerie asynchrone event-driven
- **JWT** : Authentification stateless par token
- **AES-256** : Chiffrement symétrique par bloc (Block Cipher)
- **BCrypt** : Hachage sécurisé des mots de passe
- **BitcoinJ** : Bibliothèque Java pour le réseau Bitcoin
- **ECDSA** : Algorithme de signature elliptique (Bitcoin)
- **Angular** : Framework frontend TypeScript
- **CORS** : Politique de sécurité cross-origin

---

## 👨‍💻 Auteur

Projet réalisé étape par étape — de zéro à une application blockchain complète.
