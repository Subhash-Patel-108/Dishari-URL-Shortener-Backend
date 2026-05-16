package com.dishari.in.application.serviceImpl;

import com.dishari.in.application.service.WorkspaceService;
import com.dishari.in.domain.entity.User;
import com.dishari.in.domain.entity.Workspace;
import com.dishari.in.domain.entity.WorkspaceMember;
import com.dishari.in.domain.enums.WorkspaceMemberRole;
import com.dishari.in.domain.enums.WorkspaceMemberStatus;
import com.dishari.in.domain.repository.UserRepository;
import com.dishari.in.domain.repository.WorkspaceMemberRepository;
import com.dishari.in.domain.repository.WorkspaceRepository;
import com.dishari.in.exception.SlugAlreadyTakenException;
import com.dishari.in.exception.UserNotFoundException;
import com.dishari.in.exception.WorkspaceAccessDeniedException;
import com.dishari.in.exception.WorkspaceNotFoundException;
import com.dishari.in.utils.UuidUtils;
import com.dishari.in.web.dto.request.CreateWorkspaceRequest;
import com.dishari.in.web.dto.request.UpdateWorkspaceRequest;
import com.dishari.in.web.dto.response.*;
import com.dishari.in.web.mapper.WorkspaceMapper;

import com.dishari.in.web.projection.WorkspaceSummaryProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspaceMapper workspaceMapper;

    /**
     * Create a new workspace
     */
    @Override
    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, UUID ownerId) {
        log.info("Creating workspace '{}' for user {}", request.name(), ownerId);

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + ownerId));

        // Check slug uniqueness
        if (workspaceRepository.existsBySlug(request.slug())) {
            throw new SlugAlreadyTakenException("Slug '" + request.slug() + "' is already taken");
        }

        Workspace workspace = workspaceMapper.toEntity(request, owner);
        workspace = workspaceRepository.save(workspace);

        // Add owner as workspace member with OWNER role
        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspace(workspace)
                .user(owner)
                .role(WorkspaceMemberRole.OWNER)
                .status(WorkspaceMemberStatus.ACTIVE)
                .build();

        WorkspaceMember savedworkspaceMember = memberRepository.save(ownerMember);

        List<WorkspaceMember> members = memberRepository.findActiveByWorkspaceId(workspace.getId());
        return workspaceMapper.toWorkspaceResponse(workspace, members);
    }

    /**
     * List workspaces for a user
     */
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<WorkspaceSummary> getUserWorkspaces(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            String search,
            UUID userId) {
        log.info("Fetching workspaces for user {}", userId);

        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page , size , sort) ;

        // ONE QUERY to rule them all
        Page<WorkspaceSummaryProjection> pageResponse = workspaceRepository.findSummariesByUserId(userId, search, pageable);

        List<WorkspaceSummary> workspaceSummaries = pageResponse.getContent().stream()
                .map(p -> new WorkspaceSummary(
                        p.getId().toString(),
                        p.getName(),
                        p.getSlug(),
                        p.getLogoUrl(),
                        p.getPlan(),
                        p.isPersonal(),
                        p.getCurrentUserRole(),
                        p.getMemberCount(),
                        p.getLinkCount().intValue(),
                        p.getCreatedAt()
                ))
                .toList();

        return new PaginatedResponse<>(
                workspaceSummaries,
                pageResponse.getNumber(),
                pageResponse.getSize(),
                pageResponse.getTotalElements(),
                pageResponse.getTotalPages() ,
                pageResponse.isFirst() ,
                pageResponse.isLast() ,
                pageResponse.hasNext() ,
                pageResponse.hasPrevious()
        ) ;
    }

    /**
     * Get workspace details
     */
    @Transactional(readOnly = true)
    @Override
    public WorkspaceResponse getWorkspace(String workspaceIdStr, User principal) {

        UUID workspaceId = UuidUtils.parse(workspaceIdStr) ;
        UUID userId = principal.getId() ;

        log.info("Fetching workspace {} for user {}", workspaceId, userId);

        Workspace workspace = workspaceRepository.findActiveById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId.toString() , true));

        // Check if user is member
        memberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new WorkspaceAccessDeniedException(principal.getName(), "get info of" ,workspaceId.toString()));

        List<WorkspaceMember> members = memberRepository.findActiveByWorkspaceId(workspaceId);
        return workspaceMapper.toWorkspaceResponse(workspace, members);
    }

    /**
     * Update workspace
     */
    @Override
    public WorkspaceResponse updateWorkspace(String workspaceIdStr, UpdateWorkspaceRequest request, User principal) {

        UUID workspaceId = UuidUtils.parse(workspaceIdStr) ;
        UUID userId = principal.getId() ;

        log.info("Updating workspace {} by user {}", workspaceId, principal.getId());

        Workspace workspace = workspaceRepository.findActiveById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId.toString() , true));

        // Check if user has permission (OWNER or ADMIN)
        Workspace finalWorkspace = workspace;
        WorkspaceMember member = memberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new WorkspaceAccessDeniedException(principal.getName() , "update" ,  finalWorkspace.getSlug()));

        if (member.getRole() != WorkspaceMemberRole.OWNER &&
                member.getRole() != WorkspaceMemberRole.ADMIN) {
            throw new WorkspaceAccessDeniedException("Only owners and admins can update workspace");
        }

        workspaceMapper.updateEntity(request, workspace);
        workspace = workspaceRepository.save(workspace);

        List<WorkspaceMember> members = memberRepository.findActiveByWorkspaceId(workspaceId);
        return workspaceMapper.toWorkspaceResponse(workspace, members);
    }

    /**
     * Delete workspace (owner only) - Soft delete via @SoftDelete
     */
    @Override
    public MessageResponse deleteWorkspace(String workspaceIdStr, User principal) {

        UUID workspaceId = UuidUtils.parse(workspaceIdStr) ;
        UUID userId = principal.getId() ;

        log.info("Deleting workspace {} by user {}", workspaceId, principal.getId());

        Workspace workspace = workspaceRepository.findActiveById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId.toString() , true));

        WorkspaceMember member = memberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new WorkspaceAccessDeniedException(principal.getName(), "delete" , workspace.getSlug()));

        if (member.getRole() != WorkspaceMemberRole.OWNER) {
            throw new WorkspaceAccessDeniedException("Only workspace owners can delete the workspace");
        }

        workspaceRepository.softDelete(workspace.getId());
        log.info("Workspace {} soft deleted successfully", workspaceId);

        return MessageResponse.builder()
                .message("Workspace deleted successfully")
                .timestamp(Instant.now())
                .status(200)
                .build();
    }
}