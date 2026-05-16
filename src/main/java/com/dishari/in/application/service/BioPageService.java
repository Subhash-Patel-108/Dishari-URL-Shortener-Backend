package com.dishari.in.application.service;

import com.dishari.in.domain.entity.User;
import com.dishari.in.web.dto.request.*;
import com.dishari.in.web.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface BioPageService {

    // ── Bio Page CRUD ────────────────────────────────────────────
    BioPageResponse createBioPage(
            CreateBioPageRequest request,
            User principal);

    BioPageResponse updateBioPage(String bioPageId, UpdateBioPageRequest request, User principal);

    MessageResponse deleteBioPage(String bioPageId, User principal);

    BioPageResponse getMyBioPage(String bioPageId, User principal);

    List<BioPageResponse> getAllMyBioPages(User principal);

    // ── Public endpoint — no auth ────────────────────────────────
    BioPagePublicResponse getPublicBioPage(String handle);

    void recordPageView(String handle);

    // ── Bio Link CRUD ────────────────────────────────────────────
    BioLinkResponse addLink(String bioPageId, CreateBioLinkRequest request, User principal);

    BioLinkResponse updateLink(
            String bioPageId,
            String linkId,
            UpdateBioLinkRequest request,
            User principal);

    void deleteLink(String bioPageId, String linkId, User principal);

    // ── Link ordering ────────────────────────────────────────────
    List<BioLinkResponse> reorderLinks(
            String bioPageId,
            ReorderBioLinksRequest request,
            User principal);
}