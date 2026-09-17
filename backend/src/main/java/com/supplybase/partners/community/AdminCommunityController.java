package com.supplybase.partners.community;

import com.supplybase.partners.identity.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/community")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommunityController {

    private final CommunityService communityService;
    private final CommunityReportRepository reportRepository;
    private final CurrentUser currentUser;

    public AdminCommunityController(CommunityService communityService, CommunityReportRepository reportRepository, CurrentUser currentUser) {
        this.communityService = communityService;
        this.reportRepository = reportRepository;
        this.currentUser = currentUser;
    }

    @PostMapping("/announcements")
    public Announcement publish(@RequestBody Map<String, String> body) {
        return communityService.publishAnnouncement(currentUser.userId(), body.get("title"), body.get("body"));
    }

    @GetMapping("/reports")
    public List<CommunityReport> openReports() {
        return reportRepository.findAllByStatus(CommunityReport.Status.OPEN);
    }

    @PostMapping("/posts/{postId}/moderate")
    public void moderate(@PathVariable Long postId, @RequestBody Map<String, Boolean> body) {
        communityService.moderatePost(postId, Boolean.TRUE.equals(body.get("hide")), currentUser.userId());
    }

    @PostMapping("/reports/{reportId}/resolve")
    public void resolveReport(@PathVariable Long reportId, @RequestBody Map<String, Boolean> body) {
        communityService.resolveReport(reportId, Boolean.TRUE.equals(body.get("dismiss")), currentUser.userId());
    }
}
