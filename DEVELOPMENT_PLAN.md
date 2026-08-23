# Bin-Box — 10-Phase Development Plan

**Application ID / Namespace:** `com.inscopelabs.abx.bin-box`

This plan expands the existing Bin Box foundation into a provider-independent terminal execution platform rather than limiting the application to an Oracle VM client. The architecture establishes the VM/session model, REST/WebSocket architecture, terminal emulator, keyboard controls, lifecycle handling, and VM/session navigation as core foundations.

---

## Phase 1 — Project Foundation & Core Architecture

**Objective:** Establish the production Android/Kotlin foundation and provider-independent terminal architecture.

### Architectural Principle
> The terminal core must not know whether it is connected to Oracle, Termux, Local Shell, SSH, WebSocket, or any other infrastructure provider.

### Functional Integration Steps

#### Step 1.1 — Package Hierarchy & Clean Architecture Stratification
- **Goal:** Structure the codebase into strict Clean Architecture layers under `com.inscopelabs.abx.binbox`.
- **Package Layout:**
  - `core` (dispatchers, logging, common exceptions, Result wrappers)
  - `domain.model` (provider-agnostic entities, enums, value objects)
  - `domain.repository` (abstract interfaces for data and session operations)
  - `data.database` (Room entities, DAOs, migrations)
  - `data.repository` (concrete Room & local repository implementations)
  - `security` (KeyStore encryption, biometric prompt wrappers, secure storage)
  - `session` (session multiplexing, lifecycle, state machines)
  - `terminal` (ANSI parser, buffer, styles, input translation)
  - `network` / `providers` (SSH, Local PTY/Process, WebSocket, Oracle Cloud)
  - `ui` (theme, components, navigation, ViewModels)
- **Deliverables:**
  - `AppResult<T>` sealed class hierarchy (`Success`, `Error`, `Loading`).
  - `AppError` typed failure model (`NetworkError`, `AuthError`, `CryptoError`, `IoError`).
  - `CoroutineDispatchers` abstraction with injectable IO, Default, Main, and Unconfined for testability.

#### Step 1.2 — Domain Entities & Core Contracts
- **Goal:** Formulate all provider-independent models and repository contracts.
- **Entities & Models:**
  - `ConnectionProfile`: Host ID, label, host/IP, port, username, auth type (Password, Key, Agent), terminal theme, keepalive interval.
  - `TerminalSession`: Session UUID, profile reference, state (`DISCONNECTED`, `CONNECTING`, `AUTHENTICATING`, `CONNECTED`, `FAILED`), active buffer, creation timestamp.
  - `ShellProfile`: Shell path (`/bin/bash`, `/bin/zsh`, `/bin/sh`), custom env vars, startup commands, term type (`xterm-256color`).
  - `VmStatus`: State (`RUNNING`, `STOPPED`, `PROVISIONING`, `TERMINATED`), CPU utilization, memory usage, uptime, IP addresses.
- **Repository Contracts:**
  - `IHostRepository`, `IKeyRepository`, `ISnippetRepository`, `IHistoryRepository`.
  - `ISessionRepository` (in-memory & persisted active session descriptors).

#### Step 1.3 — Security Subsystem & KeyStore Integration
- **Goal:** Implement secure credential, passphrase, and SSH key management backed by Android KeyStore.
- **Deliverables:**
  - `SecureStorageService`: AES-256-GCM hardware-backed encryption with MasterKeys.
  - `SshKeyManager`: RSA (2048/4096) and Ed25519 key generation, OpenSSH public key formatting, PKCS#8 export/import.
  - Safe in-memory clearance for sensitive byte arrays and char arrays.

#### Step 1.4 — Transport & Session Lifecycle Abstraction
- **Goal:** Define the unified transport interface that decouples terminal execution from the underlying network protocol.
- **Deliverables:**
  - `ITransport` interface: `connect()`, `disconnect()`, `sendData(data: ByteArray)`, `resize(cols: Int, rows: Int)`, `stateFlow: StateFlow<TransportState>`.
  - `TransportListener`: Event callbacks for data reception, connection closed, and errors.
  - `SessionManager`: Multi-session coordinator tracking active sessions, focus management, auto-reconnect backoff policies, and heartbeat monitors.

#### Step 1.5 — Dependency Injection & Service Container
- **Goal:** Establish a lightweight, testable dependency container and ViewModel factory.
- **Deliverables:**
  - `BinBoxContainer`: Factory/Singleton lifecycle container providing database instances, repositories, security services, and session managers.
  - `ViewModelFactory`: Parameterized Compose ViewModel resolution with clean lifecycle management.

#### Step 1.6 — Diagnostics, Telemetry & Logging Engine
- **Goal:** Build an internal diagnostic and event pipeline for auditing and live performance tracking.
- **Deliverables:**
  - `BinBoxLogger`: Configurable log levels (Debug, Info, Warn, Error) with structured tags and in-memory circular log ring for export.
  - `SystemDiagnosticsCollector`: Runtime OS version, kernel info, CPU architecture, memory footprint, and network status checks.
  - Real-time latency and throughput telemetry trackers for open sessions.

#### Step 1.7 — Baseline Unit & Robolectric Verification Suite
- **Goal:** Validate all domain models, repositories, crypto tools, and session state machines with automated JVM tests.
- **Deliverables:**
  - Unit tests for `SshKeyManager` key generation and validation.
  - Unit tests for `SessionManager` state transitions and multiplexing.
  - Unit tests for `Room` DAO interactions and repository mappings.
  - Execution of `gradle :app:testDebugUnitTest` to guarantee a 100% green baseline.

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

## Phase 11 — Semantic Stream Ingestion & Smart Token-Budget Windowing

**Objective:** Transform raw ANSI terminal byte streams into structured, token-efficient formats optimized for LLMs and autonomous agents without context window exhaustion.

```
Raw ANSI Stream ──▶ ANSI/PTY State Reducer ──▶ Smart Log Folder ──▶ Structured Token Snapshot
```

### Deliverables
- **ANSI & PTY State Reducer:** Filter spinner animations, escape sequences, and screen clearing artifacts into pristine semantic text diffs.
- **Smart Log Folding:** Intelligently collapse high-volume build outputs (Maven, Gradle, Webpack, test dumps) while preserving exit codes and exact error stack traces.
- **Interactive Prompt Detector:** Regex and state-machine heuristic listener for CLI queries (`[y/N]`, `sudo` password prompts, OAuth verification URLs, pagers) surfacing structured callback events.
- **Dynamic Token-Budget Windowing:** Adjustable sliding-window snapshot generator enabling agents to consume terminal context within constrained token budgets.

---

## Phase 12 — Agentic Co-Pilot & "Human-in-the-Loop" (HITL) Safety Gateways

**Objective:** Enable collaborative, supervisory execution between human engineers and autonomous agents on shared terminal sessions.

```
Agent Intent ──▶ Command Policy Filter ──▶ HITL Gatekeeper ──▶ Multiplexed PTY
                       ▲                         ▲
                       │                         │
                  Safety Rules            Biometric / User Tap
```

### Deliverables
- **Two-Way Multiplexed PTY:** Allow human engineers and autonomous agents to simultaneously attach to and inspect the same live terminal session.
- **Granular Command Policy Filter:** Configurable policy engine classifying commands into safe (read-only) vs. mutating/destructive (`rm -rf`, `systemctl`, `iptables`, `dd`).
- **HITL Permission Gates:** Intercept destructive or mutating agent commands, prompting the user for approval via biometric or one-tap UI confirmation.
- **Visual Agent Intent Overlays:** Terminal buffer annotations displaying agent rationale, command previews, and projected impact prior to execution.

---

## Phase 13 — Model Context Protocol (MCP) & Agent Tool Server

**Objective:** Standardize Bin Box as an external execution backend for agent architectures using the open Model Context Protocol.

```
AI Agent / Framework (Claude, Gemini, LangChain)
                      │
                      ▼ [MCP JSON-RPC / SSE]
            Bin Box Embedded MCP Server
                      │
        ┌─────────────┼──────────────┐
        ▼             ▼              ▼
  binbox_exec   binbox_read    binbox_fs
```

### Deliverables
- **Embedded MCP Server Endpoint:** Expose Bin Box terminal sessions via local WebSocket/SSE following Model Context Protocol standards.
- **Core MCP Tool Definitions:**
  - `binbox_exec(sessionId, command, timeout, background)`
  - `binbox_read_tail(sessionId, lines, filter)`
  - `binbox_get_env(sessionId)`
  - `binbox_upload_file(sessionId, remotePath, contentBase64)`
  - `binbox_list_sessions()`
- **Local Daemon & Token Authentication:** Secure, loopback-bound IPC daemon with rotating bearer tokens for safe workstation-to-device and on-device agent communication.

---

## Phase 14 — Session Time-Travel, Telemetry & Differential State Snapping

**Objective:** Provide complete deterministic session playback, environmental diffing, and fine-tuning dataset generation.

### Deliverables
- **Asciinema-Compatible Recording:** Capture timestamped I/O stream packets for complete terminal auditability, replay, and sharing.
- **Environment & State Diffing:** Track working directories (`pwd`), environment variable mutations, Git branch statuses, and active child PIDs across command execution boundaries.
- **Session Time-Travel Scrubbing:** Interactive timeline slider in the UI to scrub back to any historical point in the session buffer.
- **Dataset Export Pipeline:** Clean export of command-prompt-response sequences formatted for agent fine-tuning and evaluation harnesses.

---

## Phase 15 — Resilient Auto-Reconnection & Headless Background Execution

**Objective:** Eliminate session loss caused by mobile network drops, device sleep, or background process hibernation during long-running workflows.

```
Mobile Foreground / Background ──▶ Native tmux / Session Daemon ──▶ Remote Host Shell
                                              │
                                              ▼
                                 Android System Notification
```

### Deliverables
- **Native Remote Session Multiplexing:** Built-in auto-attachment to `tmux` / `screen` sessions on remote hosts and Oracle VMs to survive connection loss.
- **Mobile Lifecycle State Preserver:** Serialize session state to persistent disk cache upon Android activity backgrounding or process termination.
- **Background Task Monitor & Notification Engine:** Monitor long-running tasks in the background and dispatch rich Android system notifications with interactive action buttons upon completion, error, or prompt blocker.
- **Smart Exponential Backoff Reconnection:** Dynamic reconnect strategy with jitter and socket health telemetry.

---

## Phase 16 — Server Ecosystem & One-Click Package/Plugin Hub (cPanel / App Store Model)

**Objective:** Equip Bin Box with an automated package, stack, and plugin manager inspired by cPanel / WHM / Softaculous, allowing engineers to bootstrap fresh servers and Oracle VMs with full development stacks, databases, web servers, AI agent runtimes, and security tooling in one click.

```
                  Bin Box Hub (Catalog)
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
  Dev Stacks (Node,    Web / Services      AI Agent Runtimes
  Python, Rust, Go)   (Nginx, Docker,     (Ollama, MCP Servers,
                       Postgres, Redis)    FastAPI, LangGraph)
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │  Remote OS Package & Service Manager  │
        │  (apt, dnf, pacman, brew, docker, pip) │
        └───────────────────────────────────────┘
                            │
                            ▼
               [Live Provisioning Stream]
```

### Deliverables
- **Curated Server Software Catalog (App Store):**
  - **Runtimes & SDKs:** Node.js (nvm), Python (pyenv/uv), Rust, Go, Java, Bun, Deno.
  - **Web Servers & Proxies:** Nginx, Caddy (auto-HTTPS), Traefik, Apache.
  - **Databases & Caches:** PostgreSQL, Redis, MySQL/MariaDB, MongoDB, SQLite.
  - **Containers & Virtualization:** Docker, Docker Compose, Podman, Kubernetes (k3s).
  - **AI & Agent Stacks:** Ollama (local LLMs), vLLM, HuggingFace CLI, Model Context Protocol (MCP) server runners, LiteLLM.
  - **Security & Ops Tools:** UFW firewall, Fail2ban, Certbot/Let's Encrypt SSL, WireGuard VPN, Netdata, Prometheus/Grafana agent.
- **One-Click Automated Provisioning Engine:**
  - Dynamic OS detection (Ubuntu/Debian, CentOS/RHEL/Oracle Linux, Alpine, Arch, macOS/Darwin).
  - Non-interactive script execution with real-time progress streaming into a dedicated terminal drawer.
  - Automatic dependency resolution and post-install health checks (e.g., verifying `systemctl status` or listening ports).
- **Service & Plugin Lifecycle Management:**
  - Live status cards for installed components (Running / Stopped / Not Installed).
  - One-tap lifecycle controls: Start, Stop, Restart, Enable on Boot, and Uninstall/Purge.
  - Configuration file quick-editor (e.g., quick edit `nginx.conf`, `redis.conf`, `.env`).
  - Port and firewall rule management for newly installed services.
- **Custom Community Recipes & Template Bundles:**
  - Exportable server blueprints / recipes (e.g., "Full-Stack Node + Postgres", "AI Inference Host", "Minimal Hardened VPS").
  - User-defined provisioning scripts with variable parameterization.

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
| **11** | Semantic Stream Ingestion | Token-budget windowing & ANSI state reduction |
| **12** | Agentic Co-Pilot & HITL | Two-way multiplexed PTY & supervisory safety gates |
| **13** | MCP & Agent Tool Server | Model Context Protocol (MCP) tool integration |
| **14** | Session Time-Travel & State Diff | Asciinema recording, state diffing & dataset export |
| **15** | Resilient Headless Execution | Remote `tmux` multiplexing & background task alerts |
| **16** | Server Ecosystem & App Hub | cPanel-style 1-click stack installer & service manager |

> **Core Architectural Rule:** *Bin Box is the terminal; providers are execution environments.*
