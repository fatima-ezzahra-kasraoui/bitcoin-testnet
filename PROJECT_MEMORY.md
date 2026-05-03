# PROJECT_MEMORY — Bitcoin TestNet Wallet

## STACK
| Tech | Version | Role |
|------|---------|------|
| Spring Boot | 3.x | REST backend, security, scheduling |
| Angular | 21 (standalone) | SPA frontend |
| MongoDB | 7.x | Primary database |
| Apache Kafka | — | Async tx event streaming |
| bitcoinj | — | TestNet3 wallet gen, tx signing, SPV peers |
| mempool.space API | — | UTXO fetch, broadcast, confirmations, faucet |
| JJWT | — | JWT token generate / validate |
| Lombok | — | Java boilerplate (@Data, @RequiredArgsConstructor) |
| Chart.js | — | Security insights bar charts |
| jsPDF | 2.x | PDF security report (2 pages) |
| qrcode | — | QR code PNG generation (wallet address) |
| html2canvas | — | Installed, not yet used |

## PORTS & URLS
| Service | Port | Base URL |
|---------|------|----------|
| Spring Boot | 8081 | http://localhost:8081/api |
| Angular dev | 4200 | http://localhost:4200 |
| MongoDB | 27017 | localhost/bitcoin_testnet |
| Kafka | 9092 | localhost:9092 |
| Bitcoin TestNet | 443 | https://mempool.space/testnet/api |

## PROJECT STRUCTURE
```
bitcoin/
├── bitcoin-testnet/src/main/java/com/bitcoin/bitcoin_testnet/
│   ├── config/         SecurityConfig (CORS 4200, JWT filter, public: /api/auth/**, /api/status)
│   ├── controller/     Auth, Wallet, Transaction, Message, Notification, Security
│   ├── dto/            TransactionRequest, AuthRequest, WalletRequest
│   ├── exception/      GlobalExceptionHandler
│   ├── filter/         JwtFilter (OncePerRequestFilter, sets username as principal)
│   ├── model/          User, Wallet, Transaction, Notification
│   ├── repository/     Mongo repositories (Spring Data)
│   └── service/        Auth, Wallet, Transaction, AnomalyDetection, Kafka*, Bitcoin, Encryption, Security, Jwt
├── bitcoin-frontend/src/app/
│   ├── components/     login, dashboard, wallet, security, profile, contacts
│   └── services/       bitcoin.ts  (all HTTP, getHeaders() from localStorage token)
├── PROJECT_MEMORY.md
└── memory/             Claude session memories
```

## MONGO COLLECTIONS
| Collection | Key Fields |
|------------|------------|
| users | id, username, password (BCrypt), role |
| wallets | id, userId, address, publicKey, encryptedPrivateKey, label, createdAt |
| transactions | id, txId (blockchain), userId, fromAddress, toAddress, amount (Long sat), status, confirmations, createdAt, confirmedAt, requiresConfirmation (bool), confirmationExpiresAt (LocalDateTime), riskScore (Integer), triggeredRules (List\<String\>) |
| notifications | id, userId, message, txId, read (bool), createdAt, type (TX_UPDATE\|SECURITY_ALERT), riskScore, triggeredRules |

## TRANSACTION STATUSES
| Status | Trigger |
|--------|---------|
| PENDING | Broadcast to mempool / awaiting confirmations |
| CONFIRMED | >= 3 blockchain confirmations (polled every 30s) |
| FAILED | 404 from mempool.space OR user cancelled |
| AWAITING_CONFIRMATION | analyzeSync score >= 50 — held before broadcast |

## API ENDPOINTS

### AuthController — public
| Method | URL | Description |
|--------|-----|-------------|
| POST | /api/auth/register | Register → JWT |
| POST | /api/auth/login | Login → JWT |
| POST | /api/auth/change-password | Change password (JWT) |
| GET | /api/status | TestNet peer connection info |

### WalletController — JWT
| Method | URL | Description |
|--------|-----|-------------|
| POST | /api/wallets | Create P2PKH wallet (bitcoinj TestNet3) |
| GET | /api/wallets/{userId} | List user wallets |
| GET | /api/wallets/{address}/balance | Balance from mempool.space |
| POST | /api/wallets/{address}/faucet | Request 10000 sat |
| PATCH | /api/wallets/{address}/label | Rename wallet |
| DELETE | /api/wallets/{address} | Delete wallet |

### TransactionController — JWT
| Method | URL | Description |
|--------|-----|-------------|
| POST | /api/transactions | Send tx → returns Map (may be AWAITING_CONFIRMATION) |
| GET | /api/transactions/pending-confirmation | AWAITING_CONFIRMATION txs not yet expired |
| GET | /api/transactions/{address} | History (params: status, from, to, sort) |
| GET | /api/transactions/{address}/export | CSV download |
| POST | /api/transactions/{txId}/confirm | Confirm held tx → broadcast |
| POST | /api/transactions/{txId}/cancel | Cancel held tx → FAILED |

> **{txId}** in confirm/cancel = MongoDB document `id`, NOT blockchain txId.
> `GET /pending-confirmation` must stay ABOVE `GET /{address}` — Spring MVC literal paths win.

### Other Controllers — JWT
| Method | URL | Description |
|--------|-----|-------------|
| POST | /api/messages/sign | Sign message with ECKey |
| GET | /api/notifications | Unread notifications for user |
| PATCH | /api/notifications/{id}/read | Mark notification read |
| GET | /api/security/insights | 30-day aggregated risk data |
| GET | /api/security/score-history | Top 20 SECURITY_ALERT history |
| GET | /api/contacts | List saved contacts |
| POST | /api/contacts | Add contact {label, address} |
| DELETE | /api/contacts/{id} | Delete contact |

## SECURITY RULES
| Rule | Points | Condition |
|------|--------|-----------|
| HIGH_AMOUNT | +30 | amount > 3× avg of last 10 CONFIRMED txs from same fromAddress |
| NEW_DESTINATION | +25 | First tx to this toAddress (DB history size <= 1 after save) |
| RAPID_FIRE | +20 | >= 3 txs from same fromAddress in last 2 minutes |
| OFF_HOURS | +15 | Current hour differs > 3 from most common hour in last 20 txs |
| ROUND_AMOUNT | +10 | amount % 1000 == 0 AND amount >= 10000 sat |

## ANOMALY LOGIC
- Score **>= 50** → block: set AWAITING_CONFIRMATION, save, return early (don't broadcast)
- Score **>= 30** → async: save SECURITY_ALERT Notification (runs after confirm too)
- Expiry: `LocalDateTime.now().plusMinutes(10)` → also sent as `expiresAt` epoch ms in response
- Auto-cancel scheduler: every **5 min** (fixedDelay=300000) via KafkaConsumerService
- Blockchain poll scheduler: every **30s** (fixedDelay=30000) for PENDING txs only
- `analyzeSync` (sync, returns AnalysisResult{score, triggeredRules}) called BEFORE broadcast
- `analyze` (@Async, saves notification) called AFTER broadcast / after confirm

## KEY SERVICES
| Service | Responsibility |
|---------|----------------|
| AuthService | register/login with BCrypt, JWT generation |
| WalletService | P2PKH keygen (bitcoinj), balance/faucet (mempool.space), encrypted key storage |
| TransactionService | send (save→analyzeSync→maybe hold→broadcast), confirm, cancel, getPendingConfirmations |
| AnomalyDetectionService | `computeScore` (private, 5 rules), `analyzeSync` (sync), `analyze` (@Async) |
| KafkaConsumerService | @Scheduled: poll PENDING (30s), cancel expired AWAITING_CONFIRMATION (5min) |
| KafkaProducerService | publish txId to "blockchain-transactions" topic |
| BitcoinService | bitcoinj SPV PeerGroup (TestNet3, max 4 peers, waits 30s for ≥1) |
| EncryptionService | AES encrypt/decrypt wallet private keys |
| SecurityInsightsService | aggregate 30-day: score, rulesBreakdown, alertsOverTime, topRiskyTransactions, tips |
| JwtService | generateToken(username), extractUsername, isTokenValid — 86400000ms TTL |

## ANGULAR COMPONENTS
| Component | Route | Key State / Methods |
|-----------|-------|---------------------|
| LoginComponent | /login | username, password → login() / register() → localStorage token + userId |
| DashboardComponent | /dashboard | wallets[], balances{}, transactions{}, pendingConfirmationCount — loadPendingConfirmations(), goToPendingReview(), poll every 15s |
| WalletComponent | /wallet?address= | fromAddress (query param), sendTransaction(), confirmTx(), cancelTx(), generateQrCode(), downloadQr(), copyAddress(), signMessage() |
| SecurityComponent | /security | insights, isGeneratingPdf — generatePdfReport() (jsPDF 2-page), initAlertsChart(), initRulesChart() |
| ProfileComponent | /profile | portfolioStats, changePassword() |
| ContactsComponent | /contacts | CRUD — used for address autocomplete in WalletComponent |

All components are **standalone** (no NgModule). HTTP via `provideHttpClient()`. Token in `localStorage['token']`.

## FEATURES IMPLEMENTED
1. JWT auth — register, login, change-password
2. Wallet CRUD — create, delete, label rename, balance, faucet
3. Send transaction — bitcoinj sign + mempool.space broadcast
4. Transaction history — status/date/sort filters, CSV export
5. Async anomaly detection — 5 rules, @Async, SECURITY_ALERT notification if score >= 30
6. Notification system — TX_UPDATE + SECURITY_ALERT, unread badge, dropdown, mark-read
7. Security insights dashboard — risk ring, bar charts (Chart.js), rules breakdown, tips
8. Transaction blocking — AWAITING_CONFIRMATION gate (score >= 50, saved before broadcast)
9. Confirmation modal — countdown timer (epoch ms from backend), risk badge, rule chips
10. Confirm / Cancel endpoints — broadcasts or sets FAILED
11. Auto-cancel scheduler — expired AWAITING_CONFIRMATION → FAILED every 5 min
12. Pending review badge — orange navbar badge + scroll-to + status filter on click
13. QR code modal — 256px PNG, copy address, download PNG, Escape to close
14. PDF security report — 2-page jsPDF: overview + tips, filename `security-report-YYYY-MM-DD.pdf`
15. Message signing — bitcoinj ECKey
16. Contact management — label+address, autocomplete in wallet send form
17. Kafka streaming — publish on send/confirm, consumer updates blockchain confirmations
18. Blockchain confirmation polling — mempool.space every 30s, CONFIRMED at >= 3 confs
19. Security score history — top 20 alerts with txId/riskScore/rules/date

## FEATURES IN PROGRESS / TODO
- `html2canvas` installed — potential use: capture security dashboard section as PDF image
- No refresh token / expiry handling on frontend (token just expires silently)
- No paginated transaction list (loads all for address)
- No per-wallet pending confirmation tracking (only first wallet used for scroll)
- No email / external push notification

## KNOWN BUGS / FIXES APPLIED
- Countdown urgency string compare `"1:00" <= "01:00"` is wrong → replaced with `isCountdownUrgent: boolean` set when `remaining <= 60000`
- `LocalDateTime` serializes as array by default → fixed: `spring.jackson.serialization.write-dates-as-timestamps=false` in application.properties
- Rule 2 (NEW_DESTINATION) needs tx already in DB → tx saved BEFORE `analyzeSync` call
- jsPDF TypeScript spread error → `scoreColor` typed as `[number, number, number]` (not `number[]`)

## NPM PACKAGES ADDED
| Package | Type | Use |
|---------|------|-----|
| chart.js | dep | Security insights charts |
| qrcode | dep | QR code PNG generation |
| @types/qrcode | devDep | TypeScript types |
| jspdf | dep | PDF report |
| html2canvas | dep | Installed, unused |

## IMPORTANT NOTES
- `@EnableAsync` on `BitcoinTestnetApplication` — **required** for `@Async` in AnomalyDetectionService
- `@EnableScheduling` on `KafkaConsumerService` — NOT on main class
- JWT principal = **username** string. `SecurityContextHolder.getContext().getAuthentication().getName()` returns username (used as userId throughout)
- `sendTransaction()` returns `Map<String,Object>`, NOT `Transaction` — changed when confirmation blocking was added; controller is `ResponseEntity<Map<String,Object>>`
- `{txId}` in `/confirm` and `/cancel` = MongoDB document `_id` field, not blockchain txId
- `GET /pending-confirmation` declared BEFORE `GET /{address}` in controller — literal paths take Spring MVC precedence
- Transaction saved to DB (gets MongoDB `id`) **before** `analyzeSync` so pattern queries work
- AWAITING_CONFIRMATION response fields: `status, riskScore, txId (mongoId), message, triggeredRules, toAddress, amount, expiresAt (epoch ms)`
- Wallet private keys stored AES-encrypted in MongoDB — EncryptionService decrypts only at sign time
- Kafka topic: `"blockchain-transactions"` | group: `"bitcoin-group"` | bootstrap: `localhost:9092`
