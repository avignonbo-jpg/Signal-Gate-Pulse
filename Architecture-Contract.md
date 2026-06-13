# SignalGate Pulse – Target Architecture Contract

**Document Title**: SignalGate Pulse Consumer Version – Architecture Contract  
**Version**: 1.0  
**Date**: June 13, 2026  
**Branch**: `consumer-v1`  
**Project**: SignalGate Pulse (consumer edition)  
**Status**: Binding. All future changes, refactors, features, or PRs **must reference this contract** and justify any deviation.

---

## 1. Core Architectural Principles

**SignalGate Pulse** follows a **clean, layered, security-first architecture**.

### Mandatory Constraints (Non-Negotiable)
- **Single Activity Architecture**: Only `MainActivity` as the host. All UI is built with Jetpack Compose.
- **Navigation**: Pure Compose Navigation (`NavHost` + `SignalGateNavGraph`). No Fragments.
- **Dependency Injection**: **Koin** exclusively (modules in `di/` package).
- **Persistence**: **Room** + **SQLCipher** (encrypted database via `SecureDatabase` wrapper).
- **ViewModel Strategy**: **One ViewModel per screen** (or per major feature flow). ViewModels live in `ui/screens/*/viewmodels/`.
- **UI Layer**: Jetpack Compose only. Material3 + custom theming (glassmorphic effects for Shield).
- **Background**: WorkManager + Coroutines + custom Koin Worker Factory.
- **Core Service**: `CallScreeningService` (system-level) for call decisions.

---

## 2. OSI-Inspired Layer Mapping

Lower layers **never** depend on upper layers.

### Layer 1: Physical / Platform
- **Owns**: `MainActivity`, `CallScreeningService`, `CallOverlayService`, Receivers, `MainApplication`, Manifest.
- **Security Boundary**: Hardware/OS sandbox, permission guarding, Play Integrity.

### Layer 2: Data Link / Security & Persistence
- **Owns**: `security/`, `database/`, `data/repositories/`, `data/models/`.
- **Security Boundary**: **Hard boundary** — trusted data core. All inputs sanitized here. SQLCipher encryption at rest.

### Layer 3: Network / Data Sources & Sync
- **Owns**: `logic/DataSyncEngine`, public source parsers, `DataSourceRepository`.
- **Security Boundary**: Isolation of external data. Sanitization + verification required before entering Layer 2.

### Layer 4: Transport / Business Logic
- **Owns**: `logic/CallScreeningEngine`, risk threshold, precedence rules, undo logic.
- **Security Boundary**: Pure logic. No direct I/O. Depends only on Layer 2.

### Layer 5: Session / Presentation (UI)
- **Owns**: `ui/` package, screens, components, `SignalGateNavGraph`, ViewModels, Shield overlay.
- **Security Boundary**: Untrusted user input zone. All inputs routed through Layer 2 sanitization.

### Layer 6: Application (Orchestration)
- **Owns**: Koin modules, `MainApplication`.

---

## 3. Key Patterns & Rules
- Unidirectional Data Flow (UI → ViewModel → Repository → Engine → UI).
- Repository Pattern for all data access.
- Security-First Rule: Every layer must document and respect its security boundary.
- Onboarding Wizard is mandatory on first launch.
- Shield Overlay triggered only from `CallScreeningService`.

---

## 4. Dependencies & Tech Stack (Locked)
- Koin 3.5.6+
- Room 2.6.1 + SQLCipher 4.5.4
- Compose BOM 2024.09.00+
- Kotlin 1.9.24, Coroutines 1.8.1, WorkManager 2.9.1, etc.

---

**This contract is the single source of truth.**  
Every PR and commit **must reference** this document.  
Deviations require explicit approval and contract version bump.

**Approved**: Architecture Audit – June 13, 2026
