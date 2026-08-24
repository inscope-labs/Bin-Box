package com.inscopelabs.abx.binbox.domain.model

enum class AuthType {
    PASSWORD,
    PRIVATE_KEY,
    PASSWORDLESS,
    AGENT
}

enum class ProtocolType {
    SSH,
    LOCAL_SHELL,
    DEMO_HOST,
    TELNET,
    CUSTOM_SOCKET,
    WEBSOCKET
}

enum class TerminalSessionState {
    DISCONNECTED,
    CONNECTING,
    AUTHENTICATING,
    CONNECTED,
    FAILED,
    TERMINATED
}

enum class VmState {
    PROVISIONING,
    RUNNING,
    STOPPING,
    STOPPED,
    TERMINATED,
    ERROR
}
