package com.supplybase.partners.community;

import com.supplybase.partners.identity.CurrentUser;
import com.supplybase.partners.partner.Partner;
import com.supplybase.partners.partner.PartnerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/community")
@PreAuthorize("hasRole('PARTNER')")
public class CommunityController {

    private final CommunityService communityService;
    private final PartnerService partnerService;
    private final CurrentUser currentUser;

    public CommunityController(CommunityService communityService, PartnerService partnerService, CurrentUser currentUser) {
        this.communityService = communityService;
        this.partnerService = partnerService;
        this.currentUser = currentUser;
    }

    @GetMapping("/announcements")
    public List<Announcement> announcements() {
        return communityService.announcements();
    }

    @GetMapping("/posts")
    public Page<CommunityPost> posts(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return communityService.posts(PageRequest.of(page, size));
    }

    @GetMapping("/posts/{postId}/comments")
    public List<CommunityComment> comments(@PathVariable Long postId) {
        return communityService.comments(postId);
    }

    @PostMapping("/posts")
    public CommunityPost createPost(@RequestBody Map<String, String> body) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return communityService.createPost(partner.getId(), body.get("body"));
    }

    @PostMapping("/posts/{postId}/comments")
    public CommunityComment comment(@PathVariable Long postId, @RequestBody Map<String, String> body) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return communityService.createComment(partner.getId(), postId, body.get("body"));
    }

    @PostMapping("/reports")
    public CommunityReport report(@RequestBody Map<String, String> body) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return communityService.report(partner.getId(), CommunityReport.TargetType.valueOf(body.get("targetType")),
                Long.valueOf(body.get("targetId")), body.get("reason"));
    }
}
