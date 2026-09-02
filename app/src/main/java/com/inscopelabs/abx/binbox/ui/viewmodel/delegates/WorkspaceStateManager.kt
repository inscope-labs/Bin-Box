package com.inscopelabs.abx.binbox.ui.viewmodel.delegates

import com.inscopelabs.abx.binbox.domain.model.Workspace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WorkspaceStateManager {
    private val _workspaces = MutableStateFlow<List<Workspace>>(Workspace.PRESETS)
    val workspaces: StateFlow<List<Workspace>> = _workspaces.asStateFlow()

    private val _activeWorkspace = MutableStateFlow<Workspace>(Workspace.PRESETS.first())
    val activeWorkspace: StateFlow<Workspace> = _activeWorkspace.asStateFlow()

    private val _isWorkspaceDialogOpen = MutableStateFlow(false)
    val isWorkspaceDialogOpen: StateFlow<Boolean> = _isWorkspaceDialogOpen.asStateFlow()

    fun selectWorkspace(workspaceId: String): Workspace? {
        val target = _workspaces.value.firstOrNull { it.id == workspaceId }
        if (target != null) {
            _activeWorkspace.value = target
        }
        return target
    }

    fun switchWorkspace(workspace: Workspace) {
        _activeWorkspace.value = workspace
    }

    fun setWorkspaceDialogOpen(isOpen: Boolean) {
        _isWorkspaceDialogOpen.value = isOpen
    }

    fun openWorkspaceDialog() {
        _isWorkspaceDialogOpen.value = true
    }

    fun closeWorkspaceDialog() {
        _isWorkspaceDialogOpen.value = false
    }

    fun createWorkspace(
        name: String,
        description: String = "",
        iconName: String = "Terminal",
        colorHex: String = "#38BDF8",
        hostIds: List<Long> = emptyList()
    ): Workspace {
        val newWs = Workspace(
            name = name.trim(),
            description = description.trim(),
            iconName = iconName,
            colorHex = colorHex,
            hostProfileIds = hostIds
        )
        _workspaces.value = _workspaces.value + newWs
        _activeWorkspace.value = newWs
        _isWorkspaceDialogOpen.value = false
        return newWs
    }

    fun deleteWorkspace(workspaceId: String): Boolean {
        val current = _workspaces.value
        if (current.size <= 1) {
            return false
        }
        val updated = current.filter { it.id != workspaceId }
        _workspaces.value = updated
        if (_activeWorkspace.value.id == workspaceId) {
            _activeWorkspace.value = updated.first()
        }
        return true
    }

    fun removeHostFromWorkspaces(hostId: Long) {
        val currentWorkspaces = _workspaces.value
        val updatedWorkspaces = currentWorkspaces.map { ws ->
            if (ws.hostProfileIds.contains(hostId)) {
                ws.copy(hostProfileIds = ws.hostProfileIds.filter { it != hostId })
            } else {
                ws
            }
        }
        _workspaces.value = updatedWorkspaces
        if (_activeWorkspace.value.hostProfileIds.contains(hostId)) {
            _activeWorkspace.value = _activeWorkspace.value.copy(
                hostProfileIds = _activeWorkspace.value.hostProfileIds.filter { it != hostId }
            )
        }
    }
}
