# Bin-Box — 10-Phase Development Plan

**Application ID / Namespace:** `com.inscopelabs.abx.bin-box`

This plan expands the existing Bin Box foundation into a provider-independent terminal execution platform rather than limiting the application to an Oracle VM client. The architecture establishes the VM/session model, REST/WebSocket architecture, terminal emulator, keyboard controls, lifecycle handling, and VM/session navigation as core foundations.

---

## Phase 1 — Project Foundation & Core Architecture

**Objective:** Establish the production Android/Kotlin foundation and provider-independent terminal architecture.

### Deliverables
- **Android Application:**
  - Application ID: `com.inscopelabs.abx.bin-box`
  - Namespace: `com.inscopelabs.abx.bin-box`
- **Clean Architecture Packages:**
  - `ui`
  - `domain`
  - `data`
  - `network`
  - `session`
  - `terminal`
  - `security`
  - `providers`
- **Kotlin Data Models:**
  - `VmStatus`
  - `TerminalSession`
  - `ConnectionProfile`
  - `ShellProfile`
  - `TerminalSessionState`
- Repository interfaces.
- Dependency injection foundation.
- Coroutines and structured concurrency.
- Configuration/environment abstraction.
- Application-wide logging and diagnostics.

### Architectural Principle
> The terminal core must not know whether it is connected to Oracle, Termux, SSH, WebSocket, or another provider.

---

## Phase 2 — Terminal Core & Emulator

**Objective:** Build the actual terminal experience independently of any particular backend.

### Deliverables
- VT100/ANSI-capable terminal emulator.
- Terminal buffer & Scrollback.
- Cursor management.
- ANSI escape-sequence processing.
- Color/style support & dynamic palettes.
- Font configuration & scaling.
- Terminal resizing (`SIGWINCH` propagation).
- Copy/paste, text selection, and search.
- Clear/reset operations.
- Session state restoration.

### Input Subsystem
- Normal keyboard input.
- Enter, Backspace, Tab, Escape.
- Arrow keys, Home/End, PageUp/PageDown.
- `Ctrl` key combinations.
- `Alt` key combinations.
- Function keys (`F1`–`F12`).
- Custom terminal modifier accessory bar (`Ctrl`, `Alt`, `Esc`, `Tab`, directional arrows).

---

## Phase 3 — Session & Transport Framework

**Objective:** Create the abstraction that allows multiple external shells to coexist.

```
TerminalProvider
       │
       ├── LocalProvider
       ├── SshProvider
       ├── WebSocketProvider
       └── FutureProvider
```

### Core Interfaces
- `TerminalProvider`
- `ConnectionProvider`
- `TerminalSession`
- `TerminalChannel`
- `TerminalTransport`

A terminal session exposes standard operations:
- `connect()`
- `write(data: ByteArray)`
- `resize(cols: Int, rows: Int)`
- `read(): Flow<ByteArray>`
- `disconnect()`
- `reconnect()`

### Deliverables
- Session manager & Connection manager.
- Session registry with lifecycle state machine.
- Reconnection strategy & timeout handling.
- Graceful shutdown & persistence across process recreations.
- Multiple simultaneous active sessions.

---

## Phase 4 — Local Android/Termux Shell Provider

**Objective:** Make Bin Box useful without requiring a remote server.

```
Bin Box
   │
   └── Termux Provider
          │
          └── bash / sh
```

### Deliverables
- Termux integration & local shell provider abstraction.
- Termux session & shell discovery.
- Environment variable discovery.
- Local process/session lifecycle & PTY handling.
- Terminal I/O bridge & resize propagation.
- Exit-code handling and disconnect/reconnect handling.

---

## Phase 5 — SSH Provider

**Objective:** Add generic remote-shell capability.

```
Bin Box
   │
   └── SSH Provider
         ├── Linux VPS
         ├── Raspberry Pi
         ├── Development Server
         └── Oracle VM
```

### Deliverables
- SSH connection engine.
- Host profiles (Host/IP, Port, Username).
- SSH-key authentication (RSA / Ed25519) & Password authentication.
- Known-host verification & Host-key management.
- Connection timeout, keepalive, and auto-reconnect.
- Remote PTY allocation & terminal resizing.
- Shell startup commands & clean session termination.

---

## Phase 6 — Oracle Cloud Always Free Provider

**Objective:** Make the Oracle Always Free VM a first-class managed connection target.

```
OracleVmProvider
       │
       ▼
SshProvider
       │
       ▼
TerminalSession
       │
       ▼
TerminalEmulator
```

### Deliverables
- Oracle connection profile & VM metadata model.
- VM lifecycle state tracking (Running, Stopped, Provisioning).
- Public/private IP handling.
- SSH connection configuration & identification.
- Connection health & reachability checks (ICMP/TCP ping).
- VM status display & direct terminal launch from VM details.

---

## Phase 7 — Backend / WebSocket Provider

**Objective:** Support the existing Bin Box backend/proxy model as an interchangeable transport.

```
                     Bin Box
                        │
              Terminal Provider API
                        │
        ┌───────────────┼────────────────┐
        │               │                │
      Termux           SSH           WebSocket
        │               │                │
      Local        Oracle/VPS       Bin Box Backend
```

### Deliverables
- REST API client for discovery, VM status, and session provisioning.
- WebSocket session transport with coroutine streaming.
- Authentication, session expiration, and heartbeats.
- Structured terminal frames, resize messages, and error telemetry.

---

## Phase 8 — Workspace, Profiles & Multi-Terminal UX

**Objective:** Turn the terminal into a multi-environment workspace.

### Workspaces Example
- **Development:** Termux, Oracle Free VM, Dev VPS
- **ABX:** ABX Server, ABX-STEP, Remote Build Server

### Deliverables
- Connection profiles & Shell profiles.
- Workspace organization & multi-terminal tabs.
- Quick session switching & session renaming.
- Favorites, recent connections, and group tags.
- Persistent terminal preferences (ANSI theme, font size, cursor style).
- Per-profile startup commands and environment variables.
- Searchable terminal scrollback history.

---

## Phase 9 — Security, Reliability & Device Integration

**Objective:** Harden Bin Box for real-world use.

### Security Deliverables
- Android Keystore-backed encrypted credential storage.
- Protected SSH private keys & secure token storage.
- Strict host-key verification & TLS certificate validation.
- Zero plaintext credentials in logs.
- Session-token expiration & secure clipboard handling.

### Reliability Deliverables
- Network-loss recovery & automatic session reconnection.
- Seamless background/foreground lifecycle transitions.
- Process death recovery & state preservation.
- Crash diagnostics & connection telemetry.
- Clean PTY and socket resource teardown.

---

## Phase 10 — Execution Plane & Extensibility Platform

**Objective:** Evolve Bin Box from a terminal application into a general execution platform for the ABX ecosystem.

### Provider SDK Hierarchy
```
TerminalProvider
       │
       ├── TermuxProvider
       ├── SshProvider
       ├── OracleProvider
       ├── WebSocketProvider
       ├── ABXProvider
       └── CustomProvider
```

### Execution Plane Architecture
```
                  AI / User
                     │
                     ▼
                ABX Gateway
                     │
                     ▼
                 Bin Box
                     │
       ┌─────────────┼──────────────┐
       │             │              │
     Termux       Oracle VM       SSH/VPS
       │             │              │
       ▼             ▼              ▼
    Local          Linux          Linux
    shell          shell          shell
```

---

## Summary Matrix

| Phase | Capability | Result |
|---|---|---|
| **1** | Foundation | `com.inscopelabs.abx.bin-box` application architecture |
| **2** | Terminal Core | Real Android terminal emulator & modifier bar |
| **3** | Session / Transport | Provider-independent terminal sessions |
| **4** | Termux | Local Android execution |
| **5** | SSH | Generic remote shells |
| **6** | Oracle VM | Oracle Always Free integration |
| **7** | WebSocket Backend | Bin Box backend/proxy integration |
| **8** | Workspaces | Multiple shells / sessions / workspaces |
| **9** | Security & Reliability | Production-grade terminal security & recovery |
| **10** | ABX Execution Plane | Extensible multi-agent / multi-shell platform |

> **Core Architectural Rule:** *Bin Box is the terminal; providers are execution environments.*
