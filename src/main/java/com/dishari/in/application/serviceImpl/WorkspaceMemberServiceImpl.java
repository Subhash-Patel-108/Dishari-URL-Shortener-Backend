package com.dishari.in.application.serviceImpl;

import com.dishari.in.application.service.WorkspaceMemberService;
import com.dishari.in.domain.entity.User;
import com.dishari.in.domain.entity.Workspace;
import com.dishari.in.domain.entity.WorkspaceMember;
import com.dishari.in.domain.enums.WorkspaceMemberRole;
import com.dishari.in.domain.enums.WorkspaceMemberStatus;
import com.dishari.in.domain.repository.UserRepository;
import com.dishari.in.domain.repository.WorkspaceMemberRepository;
import com.dishari.in.domain.repository.WorkspaceRepository;
import com.dishari.in.exception.*;
import com.dishari.in.infrastructure.email.EmailService;
import com.dishari.in.utils.UuidUtils;
import com.dishari.in.web.dto.request.InviteMemberRequest;
import com.dishari.in.web.dto.request.UpdateMemberRoleRequest;
import com.dishari.in.web.dto.response.MessageResponse;
import com.dishari.in.web.dto.response.PaginatedResponse;
import com.dishari.in.web.dto.response.WorkspaceMemberResponse;

import com.dishari.in.web.mapper.WorkspaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkspaceMemberServiceImpl implements WorkspaceMemberService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspaceMapper workspaceMapper;
    private final EmailService emailService ;

    /**
     * Get workspace members
     */
    @Transactional(readOnly = true)
    @Override
    public PaginatedResponse<WorkspaceMemberResponse> getMembers(
            int page ,
            int size ,
            String sortBy ,
            String sortDirection ,
            String search ,
            String workspaceIdStr ,
            UUID userId) {
        UUID workspaceId = UuidUtils.parse(workspaceIdStr);

        log.info("Fetching members for workspace {}", workspaceId);

        // Verify access
        memberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new WorkspaceAccessDeniedException(userId.toString(), workspaceId.toString()));


        //Fetching the paginated response
        Sort.Direction direction = sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC ;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction , sortBy));

        Page<WorkspaceMember> members = memberRepository.findActiveByWorkspaceId(workspaceId, search , pageable);
        List<WorkspaceMemberResponse> content = workspaceMapper.toMemberResponseList(members.getContent());

        return new PaginatedResponse<>(
                content ,
                page ,
                size ,
                members.getTotalElements() ,
                members.getTotalPages() ,
                members.isFirst() ,
                members.isLast() ,
                members.hasNext() ,
                members.hasPrevious()
        ) ;
    }

    /**
     * Invite member to workspace
     */
    @Override
    public WorkspaceMemberResponse inviteMember(String workspaceIdStr, InviteMemberRequest request, UUID invitedById) {

        UUID workspaceId = UuidUtils.parse(workspaceIdStr);

        log.info("Inviting member {} to workspace {}", request.email(), workspaceId);

        Workspace workspace = workspaceRepository.findActiveById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId.toString()));

        // Check if inviter has permission
        User invitedBy = userRepository.findById(invitedById)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + invitedById));

        WorkspaceMember inviter = memberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, invitedById)
                .orElseThrow(() -> new WorkspaceAccessDeniedException(invitedById.toString(), workspaceId.toString()));

        if (inviter.getRole() != WorkspaceMemberRole.OWNER &&
                inviter.getRole() != WorkspaceMemberRole.ADMIN) {
            throw new WorkspaceAccessDeniedException("Only owners and admins can invite members");
        }

        // Find user by email
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.email()));

        // Check if already a member
        if (memberRepository.existsActiveByWorkspaceIdAndUserId(workspaceId, user.getId())) {
            throw new WorkspaceMemberAlreadyExistsException("User is already a member of this workspace.");
        }
        // Create member with PENDING status
        WorkspaceMember member = workspaceMapper.toMemberEntity(workspace, user, request, invitedBy);
        WorkspaceMember savedMember = memberRepository.save(member);


        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                //after commiting, send an invitation email to user
                emailService.sendInvitationMail(
                        request.email() ,
                        user.getName() ,
                        savedMember.getId().toString(),
                        workspace.getName() ,
                        invitedBy.getName() ,
                        request.role().name());
            }
        });

        return workspaceMapper.toMemberResponse(savedMember);
    }

    /**
     * Change member role
     */
    @Override
    public WorkspaceMemberResponse updateMemberRole(String workspaceIdStr, String memberUserIdStr, UpdateMemberRoleRequest request, UUID updatedById) {

        UUID workspaceId = UuidUtils.parse(workspaceIdStr);
        UUID memberUserId = UuidUtils.parse(memberUserIdStr);

        log.info("Updating member {} role in workspace {} to {}", memberUserId, workspaceId, request.role());

        // Check if updater has permission
        WorkspaceMember updater = memberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, updatedById)
                .orElseThrow(() -> new WorkspaceAccessDeniedException(updatedById.toString(), workspaceId.toString()));

        if (updater.getRole() != WorkspaceMemberRole.OWNER) {
            throw new WorkspaceAccessDeniedException("Only workspace owners can change member roles");
        }

        // Find target member
        WorkspaceMember member = memberRepository.findActiveById(memberUserId)
                .orElseThrow(() -> new MemberNotFoundException(workspaceId.toString(), memberUserId.toString()));

        // Cannot change owner role
        if (member.getRole() == WorkspaceMemberRole.OWNER) {
            throw new RuntimeException("Cannot change the workspace owner's role");
        }

        // Cannot assign OWNER role
        if (request.role() == WorkspaceMemberRole.OWNER) {
            throw new RuntimeException("Cannot assign OWNER role. Use transfer ownership instead");
        }

        member.setRole(request.role());
        member = memberRepository.save(member);

        return workspaceMapper.toMemberResponse(member);
    }

    /**
     * Remove member from workspace - Soft delete
     */
    @Override
    @Transactional
    public void removeMember(String workspaceIdStr, String memberIdStr, UUID removedById) {

        UUID workspaceId = UuidUtils.parse(workspaceIdStr);
        UUID memberId = UuidUtils.parse(memberIdStr);

        log.info("Removing member {} from workspace {}", memberId, workspaceId);

        // Check if remover has permission
        WorkspaceMember remover = memberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, removedById)
                .orElseThrow(() -> new WorkspaceAccessDeniedException(removedById.toString(), workspaceId.toString()));

        WorkspaceMember member = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(workspaceId.toString(), memberId.toString()));

        // Cannot remove owner
        if (member.getRole() == WorkspaceMemberRole.OWNER) {
            throw new IllegalMemberOperationException("Cannot remove the workspace owner");
        }

        // Only owner or admin can remove members
        if (remover.getRole() != WorkspaceMemberRole.OWNER &&
                remover.getRole() != WorkspaceMemberRole.ADMIN) {
            throw new WorkspaceAccessDeniedException("Only owners and admins can remove members");
        }

        // Admin cannot remove other admins
        if (remover.getRole() == WorkspaceMemberRole.ADMIN &&
                member.getRole() == WorkspaceMemberRole.ADMIN) {
            throw new WorkspaceAccessDeniedException("Admins cannot remove other admins");
        }

        memberRepository.softDeleteMember(memberId) ;

        log.info("Member {} soft removed from workspace {}", memberId, workspaceId);
    }

    @Override
    @Transactional
    public MessageResponse verifyWorkspaceMember(String memberIdStr) {

        UUID memberId = UuidUtils.parse(memberIdStr) ;

        WorkspaceMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId.toString()));

        if (member.getStatus() != WorkspaceMemberStatus.PENDING) {
            throw new MemberVerificationNotPendingException("Member already verified." );
        }

        member.setStatus(WorkspaceMemberStatus.ACTIVE);
        member.setJoinedAt(Instant.now());
        memberRepository.save(member);

        return new MessageResponse("Workspace member verified successfully." , 200);
    }
}
