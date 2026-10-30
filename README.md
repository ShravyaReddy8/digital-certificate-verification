# CertiTrust — Digital Certificate Verification System

A complete full-stack application for issuing and verifying digital certificates. Institutions issue
certificates with a SHA-256 fingerprint and an RSA digital signature; anyone can verify a certificate by
uploading the file, entering its certificate ID, or scanning its QR code — no login required.

The result is always one of **VALID**, **TAMPERED**, **REVOKED**, **EXPIRED** or **NOT FOUND**, and every
verification attempt is recorded in an audit log.

---

## Table of contents

1. [Project overview](#project-overview)
2. [Features](#features)
3. [Tech stack](#tech-stack)
4. [Architecture](#architecture)
5. [Project structure](#project-structure)
6. [Setup and run](#setup-and-run)
7. [Default credentials and seed data](#default-credentials-and-seed-data)
8. [Demo flow](#demo-flow)
9. [How verification works](#how-verification-works)
10. [REST API reference](#rest-api-reference)
11. [Configuration](#configuration)
12. [Database: H2 and MySQL](#database-h2-and-mysql)
13. [Tests](#tests)
14. [Security notes](#security-notes)
15. [Troubleshooting](#troubleshooting)

---

## Project overview

CertiTrust solves a simple problem: a PDF certificate is trivial to edit, so a copy of one proves
nothing. CertiTrust stores a cryptographic fingerprint (SHA-256) of every certificate at issue time and
signs that fingerprint with an RSA private key held by the system. Verifiers can then check that a
submitted file is byte-for-byte the file the issuer registered.

Two roles use the system:

- **Issuer (institution)** — registers, waits for administrator approval, then issues certificates either
  by auto-generating a professional PDF or by uploading an existing PDF/JPG/PNG. Issuers can download
  files and QR codes, view/search their certificates and revoke a certificate with a reason.
- **Administrator** — approves or rejects issuer registrations, views all certificates, reviews
  verification logs with pagination, and sees dashboard statistics.

The **public verifier** requires no account at all.

---

## Features

### Authentication & security
- Registration and login with JWT bearer tokens and BCrypt password hashing.
- Role-based authorization: `ADMIN` and `ISSUER` route protection on both the API and the UI.
- Issuer accounts must be approved by an administrator before login is allowed.
- Default administrator seeded automatically on startup.
- Protected routes, persistent token storage, logout, loading states and error toasts in the UI.

### Issuer module
- Issue a certificate with student name, email, course/title, institution, issue date, optional expiry
  date, and grade/remarks.
- **Option A:** auto-generate a landscape-A4 certificate PDF with a QR code embedded.
- **Option B:** upload an existing PDF/JPG/PNG (max 15 MB).
- On issue, the system:
  1. generates a unique ID (`CERT-2026-XXXXXX`),
  2. computes the SHA-256 hash of the exact certificate file,
  3. signs the hash with an RSA private key (`SHA256withRSA`),
  4. stores the metadata, hash, signature and file path,
  5. generates a QR code pointing at `/verify/{certificateId}`.
- Search and filter own certificates by student, course, certificate ID, and status (VALID / REVOKED /
  EXPIRED), with pagination.
- Download the certificate file and QR code PNG.
- Revoke a certificate with a mandatory reason.

### Public verification module
- **Upload file** — the uploaded bytes are hashed and matched against stored fingerprints; signature,
  revocation and expiry are also checked.
- **Certificate ID** — full details and current status are returned.
- **QR code** — the QR points to the frontend verification page and verifies automatically.
- Large result banners: green **VALID**, red **TAMPERED / REVOKED / NOT FOUND**, orange **EXPIRED**.
- Certificate details displayed: holder, course, institution, issuer, dates, grade, SHA-256 fingerprint.
- Every attempt logged with timestamp, IP address, user agent, method and result.

### Admin module
- Dashboard cards for issuers, pending approvals, certificates, verifications, valid/invalid counts,
  revoked and expired certificates.
- Simple CSS comparison chart of valid vs. invalid verification outcomes.
- Approve/reject issuers (rejection requires a reason).
- Paginated views of all certificates (with search and status filter) and verification logs (with result
  filter).

---

## Tech stack

| Layer | Technology |
| --- | --- |
| Backend language | Java 17 |
| Framework | Spring Boot 3.3.5 (Spring Web, Spring Data JPA, Spring Security, Validation) |
| Build tool | Maven |
| Auth | JWT (jjwt 0.12.6), BCrypt |
| Boilerplate | Lombok |
| Database | H2 file-based (default, zero setup) · MySQL 8 configuration included |
| QR codes | ZXing (`core` + `javase` 3.5.3) |
| PDF generation | Apache PDFBox 2.0.31 |
| Frontend | React 18 + Vite 5, React Router 6, Axios, plain CSS |
| Testing | JUnit 5 / Spring Boot Test |

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         Browser (React + Vite)                           │
│                                                                          │
│  Home   Verify (upload / ID / QR)   Login   Register                     │
│  Issuer Dashboard (issue, list, revoke, download)                        │
│  Admin Dashboard (stats, approvals, certificates, logs)                  │
│                                                                          │
│  Axios instance ── Authorization: Bearer <JWT> ──► /api (Vite proxy)     │
└───────────────────────────────────┬──────────────────────────────────────┘
                                    │ HTTP / JSON / multipart
┌───────────────────────────────────▼──────────────────────────────────────┐
│                     Spring Boot REST API (:8080)                         │
│                                                                          │
│  SecurityFilterChain                                                     │
│    ├── JwtAuthenticationFilter  ── validates JWT, loads UserDetails      │
│    ├── /api/auth/**, /api/verify/**, /api/public/**  → public            │
│    ├── /api/issuer/**  → ROLE_ISSUER                                     │
│    └── /api/admin/**   → ROLE_ADMIN                                      │
│                                                                          │
│  Controllers ──► Services ──► Repositories (Spring Data JPA)             │
│                     │                                                    │
│                     ├── CertificateService   issue / list / revoke       │
│                     ├── VerificationService  file / ID / QR checks       │
│                     ├── AuthService          register / login / me       │
│                     ├── AdminService         stats / approvals / logs    │
│                     ├── CryptoKeyService     RSA key pair on first boot  │
│                     └── FileStorageService   local uploads directory     │
│                                                                          │
│  Utilities: HashUtil (SHA-256) · SignatureUtil (SHA256withRSA)           │
│             QrCodeUtil (ZXing) · PdfGenerator (PDFBox)                   │
│                                                                          │
│  GlobalExceptionHandler → consistent JSON error responses                │
└───────────────┬───────────────────────────────────┬──────────────────────┘
                │                                   │
        ┌───────▼────────┐                 ┌────────▼─────────┐
        │ H2 file DB     │                 │ uploads/          │
        │ (or MySQL)     │                 │  certificates/    │
        │ users          │                 │  qrcodes/         │
        │ certificates   │                 │  keys/            │
        │ verification_  │                 │   private.key     │
        │ logs           │                 │   public.key      │
        └────────────────┘                 └───────────────────┘
```

**Verification flow (upload):**

```
File bytes → SHA-256 → lookup Certificate by hash
                            │
              not found ────┴──► NOT_FOUND
                            │
              found ────────┴──► verify RSA signature of stored hash
                                 verify stored file hash
                                       │ mismatch → TAMPERED
                                       │
                                       ├─ revoked → REVOKED
                                       ├─ expiryDate < today → EXPIRED
                                       └─ otherwise → VALID
```

---

## Project structure

```
digital-certificate-verification/
├── README.md
├── .gitignore
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/certverify/
│       │   │   ├── CertificateVerificationApplication.java
│       │   │   ├── config/        SecurityConfig, DataSeeder
│       │   │   ├── controller/    Auth, Verification, Public, Issuer, Admin
│       │   │   ├── dto/           requests, responses, page wrapper
│       │   │   ├── entity/        User, Certificate, VerificationLog + enums
│       │   │   ├── exception/     GlobalExceptionHandler, ErrorResponse
│       │   │   ├── repository/    Spring Data JPA repositories
│       │   │   ├── security/      JwtService, JwtAuthenticationFilter, handlers
│       │   │   ├── service/       Auth, Certificate, Verification, Admin, storage, keys
│       │   │   └── util/          HashUtil, SignatureUtil, QrCodeUtil, PdfGenerator
│       │   └── resources/
│       │       ├── application.properties
│       │       └── application-mysql.properties
│       └── test/java/com/certverify/util/
│           ├── HashUtilTest.java
│           └── SignatureUtilTest.java
└── frontend/
    ├── package.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── api/client.js
        ├── components/    Navbar, Footer, ProtectedRoute, StatusBadge,
        │                  Pagination, Modal, CertificateDetails
        ├── context/       AuthContext, ToastContext
        ├── pages/         Home, Verify, Login, Register,
        │                  IssuerDashboard, AdminDashboard, NotFound
        ├── utils/format.js
        ├── App.jsx
        ├── main.jsx
        └── styles.css
```

Runtime folders are created automatically when the backend starts:

- `backend/data/` — H2 database files
- `backend/uploads/certificates/` — issued certificate files
- `backend/uploads/qrcodes/` — generated QR PNGs
- `backend/uploads/keys/` — RSA key pair (generated on first start)

These folders are git-ignored.

---

## Setup and run

### Prerequisites

| Tool | Version |
| --- | --- |
| JDK | 17 or newer |
| Maven | 3.8+ |
| Node.js | 18 or newer |
| npm | 9 or newer |

No database or external service is required — H2 runs in a local file.

### 1. Start the backend

```bash
cd digital-certificate-verification/backend
mvn spring-boot:run
```

The API starts on **http://localhost:8080**. On first startup it automatically:

- creates the H2 database under `backend/data/`,
- generates the RSA key pair under `backend/uploads/keys/`,
- seeds the default admin, a demo issuer and two demo certificates.

Health check:

```bash
curl http://localhost:8080/api/public/stats
# {"totalCertificates":2,"totalVerifications":0,"totalIssuers":1}
```

### 2. Start the frontend

In a second terminal:

```bash
cd digital-certificate-verification/frontend
npm install
npm run dev
```

Open **http://localhost:5173**.

The Vite dev server proxies `/api` requests to `http://localhost:8080`, so no frontend environment
variables are needed for local development.

To build a production bundle:

```bash
npm run build        # output in frontend/dist
```

If you host the frontend separately from the API, set `VITE_API_URL` (e.g.
`VITE_API_URL=http://localhost:8080/api`) before building and add your frontend origin to
`app.cors.allowed-origins`.

---

## Default credentials and seed data

| Role | Email | Password | Notes |
| --- | --- | --- | --- |
| **Admin** | `admin@cert.com` | `Admin@123` | Seeded automatically |
| **Issuer** | `issuer@cert.com` | `Issuer@123` | Pre-approved demo institution |
| Public | — | — | No login needed to verify |

The demo issuer (**Dr. Sarah Mitchell**, *Springfield Institute of Technology*) owns two seeded
certificates:

| Certificate ID | Student | Course | Grade | Status |
| --- | --- | --- | --- | --- |
| `CERT-2026-DEMO01` | Alice Johnson | B.Tech in Computer Science | A+ | VALID |
| `CERT-2026-DEMO02` | Michael Chen | Advanced Data Science & Machine Learning | A | VALID |

Both were generated through the real issuing pipeline, so their PDFs, hashes, signatures and QR codes are
fully valid and can be downloaded from the issuer dashboard.

---

## Demo flow

A complete walkthrough of every major feature:

### A. Verify a certificate (no login)

1. Open **http://localhost:5173/verify**.
2. **By ID:** choose the *Certificate ID* tab and enter `CERT-2026-DEMO01`.
   → Green **VALID** result with holder, course, issuer, dates, fingerprint and QR code.
3. **By file:** choose the *Upload file* tab and upload one of these:
   - Download the seeded PDF from the issuer dashboard (see step B) and upload it → **VALID**.
   - Open the downloaded PDF in an editor, change one character, save, upload → **TAMPERED /
     NOT AUTHENTIC** (its hash no longer matches any issued certificate).
   - Upload any unrelated file → **TAMPERED / NOT AUTHENTIC**.
4. **By QR:** the QR code on the certificate links to
   `http://localhost:5173/verify/CERT-2026-DEMO01`. Opening the link verifies automatically. On the
   verification page, the *QR link* tab also accepts a pasted verification link.
5. Try a non-existent ID such as `CERT-2026-NOPE00` → red **NOT FOUND**.

### B. Issue and manage certificates (issuer)

1. Sign in with `issuer@cert.com` / `Issuer@123` (or use the demo buttons on the login page).
2. The dashboard shows four stat cards and the two seeded certificates.
3. Click **+ Issue certificate**:
   - Choose **Auto-generate PDF**.
   - Fill in student name, email, course, institution, issue date (today or earlier), optional expiry and
     grade. Submit.
   - The new certificate appears in the table with a generated `CERT-2026-XXXXXX` ID.
4. Use **Download** to save the PDF (contains the QR code) and **QR** to save the PNG.
5. Verify the downloaded PDF through *Upload file* → **VALID**.
6. Choose **Upload existing file** to register a PDF/JPG/PNG you already have; it is hashed and signed
   exactly like a generated certificate.
7. Click **Revoke** on a certificate, enter a reason and confirm. Verify it by ID → **REVOKED** with the
   reason shown.
8. Use the search box and status filter to find certificates.

### C. Administration

1. Sign in with `admin@cert.com` / `Admin@123`.
2. **Overview** shows totals, a valid-vs-invalid chart and the latest verification attempts.
3. **Issuer approvals**:
   - Register a new institution from `/register` (it starts as `PENDING`).
   - Logging in before approval returns HTTP 403 with a clear message.
   - In the admin console, open **Issuer approvals** (the pending count appears as a badge), then
     **Approve**. The institution can now sign in and issue certificates.
   - **Reject** requires a reason and blocks the account.
4. **All certificates** supports search and status filtering across every issuer.
5. **Verification logs** lists every public attempt with method, result, timestamp and IP; filter by
   result.

### D. Expiry

Issue a certificate with an issue date in the past and an expiry date in the past (issue date may be
historical; expiry must be on/after it). Verifying it returns orange **EXPIRED** while still showing the
certificate details.

---

## How verification works

1. **Hash** — `HashUtil` computes the SHA-256 digest of the certificate file (hex, 64 chars).
2. **Registration** — at issue time the digest and file metadata are stored; the digest is signed with
   the RSA private key using `SHA256withRSA` (`SignatureUtil`).
3. **File verification** — the uploaded file is hashed and looked up by fingerprint. A missing fingerprint
   means the file is not an issued certificate (altered, re-saved or unrelated) → `TAMPERED`.
4. **Integrity check** — for any found certificate the stored digest's RSA signature is verified with the
   public key, and the stored file is re-hashed. Either mismatch → `TAMPERED`.
5. **Status checks** — revocation takes precedence, then expiry, otherwise `VALID`.
6. **Audit** — every request writes a `VerificationLog` with method, result, message, timestamp, IP and
   user agent.

The RSA key pair is generated once at first startup and persisted; subsequent restarts reuse it. Deleting
`backend/uploads/keys/` invalidates all previously issued signatures, so treat those files as production
secrets.

Status meanings:

| Status | Meaning |
| --- | --- |
| `VALID` | Authentic, signature intact, not revoked, not expired |
| `TAMPERED` | Record exists but the file/signature failed the integrity check |
| `REVOKED` | Withdrawn by the issuer (reason included) |
| `EXPIRED` | Authentic but past its expiry date |
| `NOT_FOUND` | No certificate exists for the submitted ID |

---

## REST API reference

Base URL: `http://localhost:8080/api`

Authenticated endpoints expect `Authorization: Bearer <token>`.
Errors use consistent JSON:

```json
{
  "timestamp": "2026-10-30T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed. Please check the highlighted fields.",
  "path": "/api/issuer/certificates",
  "fieldErrors": { "studentName": "Student name is required" }
}
```

### Authentication (public)

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/auth/register` | Register an issuer (status `PENDING`) |
| `POST` | `/auth/login` | Log in, returns `{ token, type, user }` |
| `GET` | `/auth/me` | Current user (requires token) |

**Register body**

```json
{
  "name": "Dr. Jane Doe",
  "email": "registrar@college.edu",
  "password": "Passw0rd123",
  "institutionName": "Example College"
}
```

**Login body**

```json
{ "email": "issuer@cert.com", "password": "Issuer@123" }
```

### Public verification & stats

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/verify/file` | Multipart `file` — verify an uploaded certificate |
| `GET` | `/verify/{certificateId}?method=CERTIFICATE_ID` | Verify by ID (method also `QR`) |
| `GET` | `/verify/{certificateId}/qr` | QR code PNG for a certificate |
| `GET` | `/public/stats` | Landing-page counters |

**Verification response**

```json
{
  "status": "VALID",
  "message": "Certificate is authentic, digitally signed and currently valid.",
  "certificate": {
    "certificateId": "CERT-2026-DEMO01",
    "studentName": "Alice Johnson",
    "courseTitle": "B.Tech in Computer Science",
    "institutionName": "Springfield Institute of Technology",
    "issueDate": "2026-02-28",
    "expiryDate": "2028-10-30",
    "grade": "A+",
    "sha256Hash": "5fa6b1b8...",
    "status": "VALID",
    "verificationUrl": "http://localhost:5173/verify/CERT-2026-DEMO01",
    "qrCodeUrl": "/api/verify/CERT-2026-DEMO01/qr"
  },
  "method": "CERTIFICATE_ID",
  "verifiedAt": "2026-10-30T10:15:30Z"
}
```

### Issuer (`ROLE_ISSUER`)

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/issuer/certificates` | Auto-generate a certificate PDF (JSON body) |
| `POST` | `/issuer/certificates/upload` | Issue from an uploaded file (multipart) |
| `GET` | `/issuer/certificates` | List own certificates (`search`, `status`, `page`, `size`) |
| `GET` | `/issuer/certificates/{certificateId}` | One certificate |
| `POST` | `/issuer/certificates/{certificateId}/revoke` | Revoke with `{ "reason": "..." }` |
| `GET` | `/issuer/certificates/{certificateId}/download` | Download certificate file |
| `GET` | `/issuer/certificates/{certificateId}/qr` | Download QR code PNG |
| `GET` | `/issuer/stats` | Own certificate counts |

**Generate body**

```json
{
  "studentName": "Priya Raman",
  "studentEmail": "priya@example.com",
  "courseTitle": "Cloud Computing Fundamentals",
  "institutionName": "Example College",
  "issueDate": "2026-10-01",
  "expiryDate": "2027-10-01",
  "grade": "A"
}
```

**Upload form fields:** `file` (required), `studentName`, `studentEmail`, `courseTitle`,
`institutionName`, `issueDate`, `expiryDate`, `grade`.

### Admin (`ROLE_ADMIN`)

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/admin/dashboard` | Counts, recent verifications, valid/invalid split |
| `GET` | `/admin/issuers` | Issuers (`status`, `page`, `size`) |
| `PATCH` | `/admin/issuers/{issuerId}/status` | `{ "status": "APPROVED" }` or `{ "status": "REJECTED", "reason": "..." }` |
| `GET` | `/admin/certificates` | All certificates (`search`, `status`, `page`, `size`) |
| `GET` | `/admin/logs` | Verification logs (`result`, `page`, `size`) |

Paginated responses:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

HTTP status codes: `200` OK, `201` Created, `400` validation/bad request, `401` unauthenticated,
`403` forbidden/pending approval, `404` not found, `409` conflict, `413` file too large,
`500` unexpected server error. Verification results themselves are `200` responses whose `status` field
carries the outcome.

---

## Configuration

Backend configuration lives in `backend/src/main/resources/application.properties`:

| Property | Default | Purpose |
| --- | --- | --- |
| `server.port` | `8080` | API port |
| `spring.datasource.url` | `jdbc:h2:file:./data/certverify;...` | H2 database location |
| `spring.servlet.multipart.max-file-size` | `15MB` | Upload limit |
| `app.jwt.secret` | demo value | JWT signing key (**change in production**, ≥ 32 chars) |
| `app.jwt.expiration-ms` | `86400000` | Token lifetime (24 hours) |
| `app.upload-dir` | `uploads` | Certificates, QR codes and RSA keys |
| `app.frontend-url` | `http://localhost:5173` | Base URL used in QR codes/verification links |
| `app.cors.allowed-origins` | localhost origins | Comma-separated CORS allow-list |

> **QR codes and `app.frontend-url`:** QR codes encode `<app.frontend-url>/verify/<id>`. The default
> `localhost` URL works on the same machine. When demonstrating from a phone or another machine, set
> `app.frontend-url` (and start Vite with `--host`) to an address the phone can reach.

---

## Database: H2 and MySQL

**Default (H2, zero setup)** — data persists in `backend/data/certverify.mv.db`. Schema is managed by
Hibernate (`ddl-auto=update`).

**MySQL (optional)** — a ready-to-use profile is provided in
`backend/src/main/resources/application-mysql.properties`. It is fully commented so the default app keeps
working; uncomment and adjust the URL/credentials, then:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

The MySQL JDBC driver (`com.mysql:mysql-connector-j`) is already a runtime dependency.

---

## Tests

Unit tests cover the cryptographic utilities:

```bash
cd backend
mvn test
```

| Test class | Coverage |
| --- | --- |
| `HashUtilTest` (6 tests) | Known SHA-256 vectors, streams, hex round-trip, invalid input |
| `SignatureUtilTest` (5 tests) | Sign/verify, tampered hash, wrong key, malformed signature |

All 11 tests pass.

---

## Security notes

This project is designed as a complete, runnable reference application. Before real production use:

- Replace `app.jwt.secret` with a strong, secret value (environment variable recommended).
- Protect `uploads/keys/private.key` — anyone with it can forge signatures. Back it up securely; losing it
  invalidates every stored signature.
- Serve over HTTPS; restrict `app.cors.allowed-origins` to your real frontend origin.
- Consider object storage for certificate files and a managed database instead of local disk/H2.
- Add rate limiting to the public verification endpoints.
- Uploaded files are validated by extension and not executed; they are only read as bytes and served
  back with their recorded content type to authenticated issuers.
- IP addresses in verification logs come from `X-Forwarded-For` (first value) or the socket address.

---

## Troubleshooting

**Port 8080 or 5173 already in use** — stop the other process, or start with
`mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` (then set
`VITE_PROXY_TARGET=http://localhost:8081` and `VITE_API_URL`) and `npm run dev -- --port 5174`.

**Frontend shows "Cannot reach the server"** — make sure the backend is running on port 8080. The Vite
proxy forwards `/api` there.

**Reset all data** — stop the backend, delete `backend/data/` and `backend/uploads/certificates/`,
`backend/uploads/qrcodes/` (keep or delete `uploads/keys/`), then restart. Seed data is recreated
automatically. Deleting the key pair while keeping certificates makes their signatures fail integrity
checks.

**Login returns 403 "pending administrator approval"** — issuer accounts must be approved by an admin.
Use the seeded demo issuer or approve the account from the admin console.

**QR code opens the wrong address** — update `app.frontend-url` to the host/port where the frontend is
reachable and restart the backend. QR codes generated before the change keep the old URL.

**Uploaded certificate verifies as NOT_FOUND** — the file must be exactly the file that was issued
(download it again from the issuer dashboard). Editing, re-saving or converting a PDF changes its bytes
and therefore its hash.

---

Built with Java 17, Spring Boot 3, React and Vite.
