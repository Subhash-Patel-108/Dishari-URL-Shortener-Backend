package com.dishari.in.web.mapper;

import com.dishari.in.domain.entity.*;

import com.dishari.in.domain.enums.WorkspaceMemberRole;
import com.dishari.in.domain.enums.WorkspaceMemberStatus;
import com.dishari.in.web.dto.request.AddCustomDomainRequest;
import com.dishari.in.web.dto.request.CreateWorkspaceRequest;
import com.dishari.in.web.dto.request.InviteMemberRequest;
import com.dishari.in.web.dto.request.UpdateWorkspaceRequest;
import com.dishari.in.web.dto.response.CustomDomainResponse;
import com.dishari.in.web.dto.response.WorkspaceMemberResponse;
import com.dishari.in.web.dto.response.WorkspaceMemberResponse.* ;
import com.dishari.in.web.dto.response.WorkspaceResponse;
import com.dishari.in.web.dto.response.WorkspaceResponse.OwnerInfo ;
import com.dishari.in.web.dto.response.WorkspaceResponse.MemberStats ;
import com.dishari.in.web.dto.response.WorkspaceSummary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class WorkspaceMapper {

    // ========== Workspace Mappings ==========

    public Workspace toEntity(CreateWorkspaceRequest request, User owner) {
        return Workspace.builder()
                .name(request.name())
                .slug(request.slug())
                .description(request.description())
                .owner(owner)
                .personal(request.personal() != null && request.personal())
                .enabled(true)
                .linkCount(0)
                .build();
    }

    public void updateEntity(UpdateWorkspaceRequest request, Workspace workspace) {
        if (request.name() != null) workspace.setName(request.name());
        if (request.description() != null) workspace.setDescription(request.description());
        if (request.logoUrl() != null) workspace.setLogoUrl(request.logoUrl());
        if (request.brandColor() != null) workspace.setBrandColor(request.brandColor());
    }

    public WorkspaceResponse toWorkspaceResponse(Workspace workspace, List<WorkspaceMember> members) {
        long activeCount = members.stream()
                .filter(m -> m.getStatus() == WorkspaceMemberStatus.ACTIVE)
                .count();
        long pendingCount = members.stream()
                .filter(m -> m.getStatus() == WorkspaceMemberStatus.PENDING)
                .count();

        return new WorkspaceResponse(
                workspace.getId().toString(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getDescription(),
                workspace.getLinkCount() ,
                workspace.getPlan(),
                workspace.isPersonal(),
                workspace.isEnabled(),
                workspace.getLogoUrl(),
                workspace.getBrandColor(),
                new OwnerInfo(
                        workspace.getOwner().getId().toString(),
                        workspace.getOwner().getName(),
                        workspace.getOwner().getEmail(),
                        workspace.getOwner().getAvatarUrl()
                ),
                new MemberStats(
                        members.size(),
                        (int) activeCount,
                        (int) pendingCount
                ),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }

    public WorkspaceSummary toWorkspaceSummary(Workspace workspace, WorkspaceMemberRole currentUserRole, int memberCount, int linkCount) {
        return new WorkspaceSummary(
                workspace.getId().toString(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getLogoUrl(),
                workspace.getPlan(),
                workspace.isPersonal(),
                currentUserRole,
                memberCount,
                linkCount,
                workspace.getCreatedAt()
        );
    }

    // ========== WorkspaceMember Mappings ==========

    public WorkspaceMember toMemberEntity(Workspace workspace, User user, InviteMemberRequest request, User invitedBy) {
        return WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .role(request.role())
                .status(WorkspaceMemberStatus.PENDING)
                .invitedBy(invitedBy)
                .build();
    }

    public WorkspaceMemberResponse toMemberResponse(WorkspaceMember member) {
        UserInfo userInfo = member.getUser() != null ?
                new UserInfo(
                        member.getUser().getId().toString(),
                        member.getUser().getName(),
                        member.getUser().getEmail(),
                        member.getUser().getAvatarUrl()
                ) : null;

        InvitedByInfo invitedByInfo = member.getInvitedBy() != null ?
                new InvitedByInfo(
                        member.getInvitedBy().getId().toString(),
                        member.getInvitedBy().getName(),
                        member.getInvitedBy().getEmail()
                ) : null;

        return new WorkspaceMemberResponse(
                member.getId().toString(),
                userInfo,
                member.getRole(),
                member.getStatus(),
                invitedByInfo,
                member.getInvitedAt(),
                member.getJoinedAt(),
                member.getRemovedAt()
        );
    }

    public List<WorkspaceMemberResponse> toMemberResponseList(List<WorkspaceMember> members) {
        return members.stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    // ========== CustomDomain Mappings ==========

    public CustomDomain toDomainEntity(AddCustomDomainRequest request, Workspace workspace) {
        return CustomDomain.builder()
                .domain(request.domain())
                .workspace(workspace)
                .rootRedirect(request.rootRedirect())
                .errorRedirect(request.errorRedirect())
                .verificationType(request.verificationType())
                .status(com.dishari.in.domain.enums.CustomDomainStatus.PENDING)
                .verified(false)
                .sslEnabled(false)
                .build();
    }

    public CustomDomainResponse toDomainResponse(CustomDomain domain) {
        return new CustomDomainResponse(
                domain.getId().toString(),
                domain.getDomain(),
                domain.getRootRedirect(),
                domain.getErrorRedirect(),
                domain.isVerified(),
                domain.getVerificationToken(),
                domain.getVerificationType(),
                domain.getStatus(),
                domain.isSslEnabled(),
                domain.getVerifiedAt(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public List<CustomDomainResponse> toDomainResponseList(List<CustomDomain> domains) {
        return domains.stream()
                .map(this::toDomainResponse)
                .collect(Collectors.toList());
    }
}