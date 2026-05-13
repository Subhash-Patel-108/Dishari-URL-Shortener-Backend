package com.dishari.in.application.service;

import com.dishari.in.domain.entity.User;
import com.dishari.in.web.dto.request.CreateWorkspaceRequest;
import com.dishari.in.web.dto.request.UpdateWorkspaceRequest;
import com.dishari.in.web.dto.response.*;

import java.util.UUID;

public interface WorkspaceService {

    WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, UUID ownerId);


    PaginatedResponse<WorkspaceSummary> getUserWorkspaces(int page, int size, String sortBy, String sortDirection, String search, UUID id);

    WorkspaceResponse getWorkspace(String workspaceIdStr, User principal);

    WorkspaceResponse updateWorkspace(String workspaceId, UpdateWorkspaceRequest request, User principal);

    MessageResponse deleteWorkspace(String workspaceId, User principal);

}

