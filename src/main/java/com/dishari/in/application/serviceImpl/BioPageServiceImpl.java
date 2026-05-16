package com.dishari.in.application.serviceImpl;

import com.dishari.in.application.service.BioPageService;
import com.dishari.in.domain.entity.*;
import com.dishari.in.domain.repository.*;
import com.dishari.in.exception.*;
import com.dishari.in.utils.UuidUtils;
import com.dishari.in.web.dto.request.*;
import com.dishari.in.web.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BioPageServiceImpl implements BioPageService {

    private final BioPageRepository bioPageRepo;
    private final BioLinkRepository bioLinkRepo;

    private static final int MAX_LINKS_PER_PAGE = 50;

    // ── Create Bio Page ──────────────────────────────────────────

    @Override
    @Transactional
    public BioPageResponse createBioPage(
            CreateBioPageRequest request,
            User principal) {

        // Handle uniqueness
        if (bioPageRepo.existsByHandleAndDeletedAtIsNull(
                request.handle())) {
            throw new BioPageHandleTakenException(
                    "Handle '@" + request.handle() +
                            "' is already taken.");
        }

        BioPage page = BioPage.builder()
                .handle(request.handle())
                .user(principal)
                .displayName(request.displayName() != null
                        ? request.displayName()
                        : principal.getName())
                .bio(request.bio())
                .avatarUrl(request.avatarUrl() != null
                        ? request.avatarUrl()
                        : principal.getAvatarUrl())
                .viewCount(0L)
                .isActive(true)
                .build();

        page = bioPageRepo.save(page);

        // Save initial links if provided
        if (request.links() != null
                && !request.links().isEmpty()) {
            page = saveLinks(page, request.links());
        }

        log.info("Bio page created: handle={} userId={}",
                page.getHandle(), principal.getId());

        return BioPageResponse.from(page);
    }

    // ── Update Bio Page ──────────────────────────────────────────

    @Override
    @Transactional
    public BioPageResponse updateBioPage(String bioPageIdStr, UpdateBioPageRequest request, User principal) {

        UUID bioPageId = UuidUtils.parse(bioPageIdStr) ;

        BioPage page = findPageAndVerifyOwnership(
                bioPageId, principal);

        if (request.displayName() != null) {
            page.setDisplayName(request.displayName());
        }
        if (request.bio() != null) {
            page.setBio(request.bio());
        }
        if (request.avatarUrl() != null) {
            page.setAvatarUrl(request.avatarUrl());
        }
        if (request.isActive() != null) {
            page.setIsActive(request.isActive());
        }

        page = bioPageRepo.save(page);

        log.info("Bio page updated: id={} handle={}",
                bioPageId, page.getHandle());

        return BioPageResponse.from(bioPageRepo.findByIdWithLinks(bioPageId).orElse(page));
    }

    // ── Delete Bio Page ──────────────────────────────────────────

    @Override
    @Transactional
    public MessageResponse deleteBioPage(String bioPageIdStr, User principal) {

        UUID bioPageId = UuidUtils.parse(bioPageIdStr) ;

        BioPage page = findPageAndVerifyOwnership(
                bioPageId, principal);

        page.setDeletedAt(Instant.now());
        page.setIsActive(false);
        bioPageRepo.save(page);

        log.info("Bio page deleted: id={} handle={}", bioPageId, page.getHandle());

        return new MessageResponse("Bio page deleted successfully." , 200);
    }

    // ── Get My Bio Page ──────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BioPageResponse getMyBioPage(String bioPageIdStr, User principal) {

        UUID bioPageId = UuidUtils.parse(bioPageIdStr) ;

        BioPage page = findPageAndVerifyOwnership(
                bioPageId, principal);

        return BioPageResponse.from(
                bioPageRepo.findByIdWithLinks(bioPageId)
                        .orElse(page));
    }

    // ── Get All My Bio Pages ─────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<BioPageResponse> getAllMyBioPages(User principal) {
        return bioPageRepo
                .findByUserIdAndDeletedAtIsNull(principal.getId())
                .stream()
                .map(page -> bioPageRepo
                        .findByIdWithLinks(page.getId())
                        .map(BioPageResponse::from)
                        .orElse(BioPageResponse.from(page)))
                .toList();
    }

    // ── Public Bio Page ──────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BioPagePublicResponse getPublicBioPage(String handle) {

        BioPage page = bioPageRepo
                .findByHandleWithLinks(handle)
                .orElseThrow(() -> new BioPageNotFoundException(
                        "Bio page '@" + handle + "' not found."));

        if (!Boolean.TRUE.equals(page.getIsActive())) {
            throw new BioPageNotFoundException(
                    "Bio page '@" + handle + "' is not available.");
        }

        return BioPagePublicResponse.from(page);
    }

    // ── Record Page View ─────────────────────────────────────────

    @Override
    @Transactional
    public void recordPageView(String handle) {
        bioPageRepo.findByHandleAndDeletedAtIsNull(handle)
                .ifPresent(page ->
                        bioPageRepo.incrementViewCount(page.getId()));
    }

    // ── Add Link ─────────────────────────────────────────────────

    @Override
    @Transactional
    public BioLinkResponse addLink(String bioPageIdStr, CreateBioLinkRequest request, User principal) {

        UUID bioPageId = UuidUtils.parse(bioPageIdStr) ;
        BioPage page = findPageAndVerifyOwnership(
                bioPageId, principal);

        // Max links check
        int currentCount = bioLinkRepo.countByBioPageId(bioPageId);
        if (currentCount >= MAX_LINKS_PER_PAGE) {
            throw new BioPageLimitException("Maximum " + MAX_LINKS_PER_PAGE + " links per bio page.");
        }

        // Resolve position
        int position = request.position() != null
                ? request.position()
                : bioLinkRepo.findMaxPositionByBioPageId(bioPageId) + 1;

        BioLink link = BioLink.builder()
                .bioPage(page)
                .title(request.title())
                .url(request.url())
                .iconType(request.iconType())
                .position(position)
                .clickCount(0L)
                .isActive(true)
                .hasAdvancedConfig(false)
                .build();

        link = bioLinkRepo.save(link);

        log.info("Bio link added: pageId={} title={}", bioPageId, link.getTitle());

        return BioLinkResponse.from(link);
    }

    // ── Update Link ──────────────────────────────────────────────

    @Override
    @Transactional
    public BioLinkResponse updateLink(
            String bioPageIdStr,
            String linkIdStr,
            UpdateBioLinkRequest request,
            User principal) {

        UUID bioPageId = UuidUtils.parse(bioPageIdStr) ;
        UUID linkId = UuidUtils.parse(linkIdStr) ;

        findPageAndVerifyOwnership(bioPageId, principal);

        BioLink link = findLinkOrThrow(linkId, bioPageId);

        if (request.title() != null) {
            link.setTitle(request.title());
        }
        if (request.url() != null) {
            link.setUrl(request.url());
        }
        if (request.iconType() != null) {
            link.setIconType(request.iconType());
        }
        if (request.isActive() != null) {
            link.setActive(request.isActive());
        }

        link = bioLinkRepo.save(link);

        log.info("Bio link updated: linkId={} pageId={}", linkId, bioPageId);

        return BioLinkResponse.from(link);
    }

    // ── Delete Link ──────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteLink(String bioPageIdStr, String linkIdStr, User principal) {

        UUID bioPageId = UuidUtils.parse(bioPageIdStr) ;
        UUID linkId = UuidUtils.parse(linkIdStr) ;

        findPageAndVerifyOwnership(bioPageId, principal);

        BioLink link = findLinkOrThrow(linkId, bioPageId);

        link.setDeletedAt(Instant.now());
        bioLinkRepo.save(link);

        log.info("Bio link deleted: linkId={} pageId={}", linkId, bioPageId);
    }

    // ── Reorder Links ────────────────────────────────────────────

    @Override
    @Transactional
    public List<BioLinkResponse> reorderLinks(
            String bioPageIdStr,
            ReorderBioLinksRequest request,
            User principal) {

        UUID bioPageId = UuidUtils.parse(bioPageIdStr) ;

        findPageAndVerifyOwnership(bioPageId, principal);

        List<BioLink> links = bioLinkRepo
                .findByBioPageIdAndDeletedAtIsNullOrderByPositionAsc(bioPageId);

        // Build a map for fast lookup
        Map<UUID, BioLink> linkMap = links.stream()
                .collect(Collectors.toMap(
                        BioLink::getId,
                        Function.identity()));

        // Validate all provided IDs exist in this page
        for (UUID id : request.orderedLinkIds()) {
            if (!linkMap.containsKey(id)) {
                throw new BioLinkNotFoundException(
                        "Link " + id + " not found in this bio page.");
            }
        }

        // Reassign positions based on order
        List<BioLink> toSave = new ArrayList<>();
        for (int i = 0; i < request.orderedLinkIds().size(); i++) {
            UUID   id   = request.orderedLinkIds().get(i);
            BioLink link = linkMap.get(id);
            link.setPosition(i + 1); // 1-based positions
            toSave.add(link);
        }

        bioLinkRepo.saveAll(toSave);

        log.info("Bio links reordered: pageId={} count={}", bioPageId, toSave.size());

        return bioLinkRepo
                .findByBioPageIdAndDeletedAtIsNullOrderByPositionAsc(
                        bioPageId)
                .stream()
                .map(BioLinkResponse::from)
                .toList();
    }

    // ── Private helpers ──────────────────────────────────────────

    private BioPage findPageAndVerifyOwnership(
            UUID bioPageId, User principal) {

        BioPage page = bioPageRepo
                .findByIdAndDeletedAtIsNull(bioPageId)
                .orElseThrow(() -> new BioPageNotFoundException(
                        "Bio page not found: " + bioPageId));

        if (!page.getUser().getId().equals(principal.getId())) {
            throw new UnauthorizedException(
                    "You don't own this bio page.");
        }

        return page;
    }

    private BioLink findLinkOrThrow(UUID linkId, UUID bioPageId) {
        BioLink link = bioLinkRepo
                .findByIdAndDeletedAtIsNull(linkId)
                .orElseThrow(() -> new BioLinkNotFoundException(
                        "Bio link not found: " + linkId));

        if (!link.getBioPage().getId().equals(bioPageId)) {
            throw new BioLinkNotFoundException(
                    "Link does not belong to this bio page.");
        }

        return link;
    }

    private BioPage saveLinks(
            BioPage page,
            List<CreateBioLinkRequest> linkRequests) {

        List<BioLink> links = new ArrayList<>();

        for (int i = 0; i < linkRequests.size(); i++) {
            CreateBioLinkRequest req = linkRequests.get(i);
            int position = req.position() != null
                    ? req.position() : i + 1;

            links.add(BioLink.builder()
                    .bioPage(page)
                    .title(req.title())
                    .url(req.url())
                    .iconType(req.iconType())
                    .position(position)
                    .clickCount(0L)
                    .isActive(true)
                    .hasAdvancedConfig(false)
                    .build());
        }

        bioLinkRepo.saveAll(links);
        return bioPageRepo.findByIdWithLinks(page.getId())
                .orElse(page);
    }
}