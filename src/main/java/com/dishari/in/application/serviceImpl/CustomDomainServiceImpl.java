package com.dishari.in.application.serviceImpl;

import com.dishari.in.application.service.CustomDomainService;
import com.dishari.in.domain.entity.CustomDomain;
import com.dishari.in.domain.entity.Workspace;
import com.dishari.in.domain.entity.WorkspaceMember;
import com.dishari.in.domain.enums.Plan;
import com.dishari.in.domain.enums.WorkspaceMemberRole;
import com.dishari.in.domain.repository.CustomDomainRepository;
import com.dishari.in.domain.repository.WorkspaceMemberRepository;
import com.dishari.in.domain.repository.WorkspaceRepository;
import com.dishari.in.exception.DomainAlreadyExistsException;
import com.dishari.in.exception.PlanUpgradeRequiredException;
import com.dishari.in.exception.WorkspaceAccessDeniedException;
import com.dishari.in.exception.WorkspaceNotFoundException;
import com.dishari.in.web.dto.request.AddCustomDomainRequest;
import com.dishari.in.web.dto.response.CustomDomainResponse;

import com.dishari.in.web.mapper.WorkspaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomDomainServiceImpl implements CustomDomainService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final CustomDomainRepository domainRepository;
    private final WorkspaceMapper workspaceMapper;

    /**
     * List custom domains for a workspace
     */
    @Transactional(readOnly = true)
    @Override
    public List<CustomDomainResponse> getDomains(String workspaceIdStr, UUID userId) {

        UUID workspaceId = UUID.fromString(workspaceIdStr);

        log.info("Fetching domains for workspace {} by user {}", workspaceId, userId);

        // Verify access
        memberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new WorkspaceAccessDeniedException(userId.toString(), workspaceId.toString()));

        List<CustomDomain> domains = domainRepository.findByWorkspaceId(workspaceId);
        return workspaceMapper.toDomainResponseList(domains);
    }

    /**
     * Add custom domain to workspace (PRO feature)
     */
    @Override
    public CustomDomainResponse addDomain(String workspaceIdStr, AddCustomDomainRequest request, UUID userId) {

        UUID workspaceId = UUID.fromString(workspaceIdStr);

        log.info("Adding domain '{}' to workspace {} by user {}", request.domain(), workspaceId, userId);

        Workspace workspace = workspaceRepository.findActiveById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId.toString()));

        // Check if user has permission
        WorkspaceMember member = memberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new WorkspaceAccessDeniedException(userId.toString(), workspaceId.toString()));

        if (member.getRole() != WorkspaceMemberRole.OWNER &&
                member.getRole() != WorkspaceMemberRole.ADMIN) {
            throw new WorkspaceAccessDeniedException("Only owners and admins can add domains");
        }

        // Check if workspace is on PRO plan
        if (workspace.getPlan() == Plan.FREE) {
            throw new PlanUpgradeRequiredException("Custom domains are only available on PRO plan. Please upgrade.");
        }

        // Check domain uniqueness
        if (domainRepository.existsByDomain(request.domain())) {
            throw new DomainAlreadyExistsException(request.domain());
        }

        // Create domain
        CustomDomain domain = workspaceMapper.toDomainEntity(request, workspace);
        domain.setVerificationToken(UUID.randomUUID().toString());
        domain = domainRepository.save(domain);

        // TODO: Initiate domain verification process

        return workspaceMapper.toDomainResponse(domain);
    }
}