package com.dishari.in.application.service;

import com.dishari.in.web.dto.request.InviteMemberRequest;
import com.dishari.in.web.dto.request.UpdateMemberRoleRequest;
import com.dishari.in.web.dto.response.MessageResponse;
import com.dishari.in.web.dto.response.PaginatedResponse;
import com.dishari.in.web.dto.response.WorkspaceMemberResponse;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface WorkspaceMemberService {

    PaginatedResponse<WorkspaceMemberResponse> getMembers(int page, int size, String sortBy, String sortDirection, String search, String workspaceId, UUID userId);

    WorkspaceMemberResponse inviteMember(String workspaceId, @Valid InviteMemberRequest request, UUID userId);

    WorkspaceMemberResponse updateMemberRole(String workspaceId, String memberId, UpdateMemberRoleRequest request, UUID updatedById);

    void removeMember(String workspaceId, String memberId, UUID removedById);

    MessageResponse verifyWorkspaceMember(String memberId);
}
