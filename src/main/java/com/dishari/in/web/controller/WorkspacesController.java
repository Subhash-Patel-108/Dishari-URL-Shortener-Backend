package com.dishari.in.web.controller;

import com.dishari.in.application.service.CustomDomainService;
import com.dishari.in.application.service.WorkspaceMemberService;
import com.dishari.in.application.service.WorkspaceService;
import com.dishari.in.aspect.ApiResponse;
import com.dishari.in.domain.entity.User;
import com.dishari.in.web.dto.request.*;
import com.dishari.in.web.dto.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspacesController {

    private final WorkspaceService workspaceService;
    private final WorkspaceMemberService memberService;
    private final CustomDomainService domainService;

    // ========== WORKSPACE CRUD ==========

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal User principal) {

        WorkspaceResponse response = workspaceService.createWorkspace(request, principal.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Workspace created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<WorkspaceSummary>>> getUserWorkspaces(
            @RequestParam(value = "page" , required = false , defaultValue = "0") int page ,
            @RequestParam(value = "size" , required = false , defaultValue = "20") int size ,
            @RequestParam(value = "sortBy" , required = false , defaultValue = "createdAt") String sortBy ,
            @RequestParam(value = "sortDir" , required = false , defaultValue = "DESC") String sortDirection,
            @RequestParam(value = "q" , required = false) String search,
            @AuthenticationPrincipal User principal) {

        PaginatedResponse<WorkspaceSummary> response = workspaceService
                .getUserWorkspaces(page,  size , sortBy , sortDirection , search , principal.getId());

        return ResponseEntity.ok(ApiResponse.success("Workspaces fetch successfully." , response));
    }

    @GetMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getWorkspace(
            @PathVariable("workspaceId") String workspaceId,
            @AuthenticationPrincipal User principal) {

        WorkspaceResponse response = workspaceService.getWorkspace(workspaceId,  principal);

        return ResponseEntity.ok(
                ApiResponse.success("Workspace retrieved successfully", response));
    }

    @PutMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> updateWorkspace(
            @PathVariable("workspaceId") String workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request,
            @AuthenticationPrincipal User principal) {

        WorkspaceResponse response = workspaceService.updateWorkspace(workspaceId, request, principal);

        return ResponseEntity.ok(
                ApiResponse.success("Workspace updated successfully", response));
    }


    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteWorkspace(
            @PathVariable("workspaceId") String workspaceId,
            @AuthenticationPrincipal User principal) {

        MessageResponse response = workspaceService.deleteWorkspace(workspaceId, principal);

        return ResponseEntity.ok(
                ApiResponse.success("Workspace deleted successfully", response));
    }

    // ========== MEMBER MANAGEMENT ==========

    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<ApiResponse<PaginatedResponse<WorkspaceMemberResponse>>> getMembers(
            @RequestParam(value = "page" , required = false , defaultValue = "0") int page ,
            @RequestParam(value = "size" , required = false , defaultValue = "20") int size,
            @RequestParam(value = "sortBy" , required = false , defaultValue = "joinedAt") String sortBy,
            @RequestParam(value = "sortDir" , required = false , defaultValue = "DESC") String sortDirection,
            @RequestParam(value = "q" , required = false) String search ,
            @PathVariable("workspaceId") String workspaceId,
            @AuthenticationPrincipal User principal) {

        PaginatedResponse<WorkspaceMemberResponse> members = memberService.getMembers(page , size , sortBy , sortDirection , search , workspaceId, principal.getId());

        return ResponseEntity.ok(
                ApiResponse.success("Members retrieved successfully", members));
    }

    @PostMapping("/{workspaceId}/members")
    public ResponseEntity<ApiResponse<WorkspaceMemberResponse>> inviteMember(
            @PathVariable("workspaceId") String workspaceId,
            @Valid @RequestBody InviteMemberRequest request,
            @AuthenticationPrincipal User principal) {

        WorkspaceMemberResponse response = memberService.inviteMember(workspaceId, request, principal.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Member invited successfully", response));
    }

    @GetMapping("/member/{memberId}/verify")
    public ResponseEntity<ApiResponse<MessageResponse>> verifyWorkspaceMember(
            @PathVariable("memberId") String memberId
    ) {
        MessageResponse response = memberService.verifyWorkspaceMember(memberId);
        return ResponseEntity.ok(ApiResponse.success("Member verified successfully", response));
    }
    @PatchMapping("/{workspaceId}/members/{memberId}")
    public ResponseEntity<ApiResponse<WorkspaceMemberResponse>> updateMemberRole(
            @PathVariable("workspaceId") String workspaceId,
            @PathVariable("memberId") String memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal User principal) {

        WorkspaceMemberResponse response = memberService.updateMemberRole(workspaceId, memberId, request, principal.getId());

        return ResponseEntity.ok(
                ApiResponse.success("Member role updated successfully", response));
    }

    @DeleteMapping("/{workspaceId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable("workspaceId") String workspaceId,
            @PathVariable("memberId") String memberId,
            @AuthenticationPrincipal User principal) {

        memberService.removeMember(workspaceId, memberId, principal.getId());

        return ResponseEntity.ok(
                ApiResponse.success("Member removed successfully", null));
    }



    // ========== CUSTOM DOMAINS ==========

    @GetMapping("/{workspaceId}/domains")
    public ResponseEntity<ApiResponse<List<CustomDomainResponse>>> getDomains(
            @PathVariable("workspaceId") String workspaceId,
            @AuthenticationPrincipal User principal) {


        List<CustomDomainResponse> domains = domainService.getDomains(workspaceId, principal.getId());

        return ResponseEntity.ok(
                ApiResponse.success("Domains retrieved successfully", domains));
    }

    @PostMapping("/{workspaceId}/domains")
    public ResponseEntity<ApiResponse<CustomDomainResponse>> addDomain(
            @PathVariable("workspaceId") String workspaceId ,
            @Valid @RequestBody AddCustomDomainRequest request,
            @AuthenticationPrincipal User principal) {

        CustomDomainResponse response = domainService.addDomain(workspaceId, request, principal.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Domain added successfully", response));
    }


}
