package com.dishari.in.web.controller;

import com.dishari.in.application.service.BioPageService;
import com.dishari.in.aspect.ApiResponse;
import com.dishari.in.domain.entity.User;
import com.dishari.in.exception.BioLinkNotFoundException;
import com.dishari.in.web.dto.request.*;
import com.dishari.in.web.dto.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bio")
public class BioPageController {

    private final BioPageService bioPageService;

    //Endpoint to create Bio page
    @PostMapping()
    public ResponseEntity<ApiResponse<BioPageResponse>> createBioPage(
            @Valid @RequestBody CreateBioPageRequest request,
            @AuthenticationPrincipal User principal) {

        BioPageResponse response = bioPageService.createBioPage(request, principal) ;
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bio Page Created Successfully" ,response));
    }

    //Endpoint to get all bio pages of the user
    @GetMapping()
    public ResponseEntity<ApiResponse<List<BioPageResponse>>> getAllMyBioPages(
            @AuthenticationPrincipal User principal) {

        List<BioPageResponse> response = bioPageService.getAllMyBioPages(principal) ;
        return ResponseEntity.ok(ApiResponse.success("Bio Pages Found Successfully",response));
    }

    //Endpoint to get bio page by id
    @GetMapping("/{bioPageId}")
    public ResponseEntity<ApiResponse<BioPageResponse>> getMyBioPage(
            @PathVariable("bioPageId") String bioPageId,
            @AuthenticationPrincipal User principal) {

        BioPageResponse response = bioPageService.getMyBioPage(bioPageId, principal) ;
        return ResponseEntity.ok(ApiResponse.success("Bio Page using " , response));
    }

    //Endpoint to update bio page
    @PutMapping("/{bioPageId}")
    public ResponseEntity<ApiResponse<BioPageResponse>> updateBioPage(
            @PathVariable("bioPageId") String bioPageId,
            @Valid @RequestBody UpdateBioPageRequest request,
            @AuthenticationPrincipal User principal) {

        BioPageResponse response = bioPageService.updateBioPage(bioPageId, request, principal) ;
        return ResponseEntity.ok(ApiResponse.success("Bio Page Updated Successfully." , response));
    }

    //Endpoint to delete bio page
    @DeleteMapping("/{bioPageId}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteBioPage(
            @PathVariable("bioPageId") String bioPageId,
            @AuthenticationPrincipal User principal) {

        MessageResponse response = bioPageService.deleteBioPage(bioPageId, principal);
        return ResponseEntity.ok(ApiResponse.success("Bio Page Deleted Successfully." , response));
    }

    //Endpoint to add bio link in the Bio page
    @PostMapping("/{bioPageId}/links")
    public ResponseEntity<ApiResponse<BioLinkResponse>> addLink(
            @PathVariable("bioPageId") String bioPageId,
            @Valid @RequestBody CreateBioLinkRequest request,
            @AuthenticationPrincipal User principal) {

        BioLinkResponse response = bioPageService.addLink(bioPageId, request, principal) ;

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Bio Link created successfully." , response));
    }

    //Endpoint to update bio link
    @PutMapping("/{bioPageId}/links/{linkId}")
    public ResponseEntity<ApiResponse<BioLinkResponse>> updateLink(
            @PathVariable("bioPageId") String bioPageId,
            @PathVariable("linkId") String linkId,
            @Valid @RequestBody UpdateBioLinkRequest request,
            @AuthenticationPrincipal User principal) {

        BioLinkResponse response = bioPageService.updateLink(bioPageId, linkId, request, principal) ;

        return ResponseEntity.ok(ApiResponse.success("Bio link updated successfully." , response));
    }

    //Endpoint to delete bio link
    @DeleteMapping("/{bioPageId}/links/{linkId}")
    public ResponseEntity<Void> deleteLink(
            @PathVariable("bioPageId") String bioPageId,
            @PathVariable("linkId") String linkId,
            @AuthenticationPrincipal User principal) {

        bioPageService.deleteLink(bioPageId, linkId, principal);
        return ResponseEntity.noContent().build();
    }

    //Endpoint to reorder the bio link
    @PatchMapping("/{bioPageId}/links/reorder")
    public ResponseEntity<ApiResponse<List<BioLinkResponse>>> reorderLinks(
            @PathVariable("bioPageId") String bioPageId,
            @Valid @RequestBody ReorderBioLinksRequest request,
            @AuthenticationPrincipal User principal) {

        List<BioLinkResponse> response = bioPageService.reorderLinks(bioPageId, request, principal) ;
        return ResponseEntity.ok(ApiResponse.success("Bio Links Reorder Successfully." , response));
    }
}