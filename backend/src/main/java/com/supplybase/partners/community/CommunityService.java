package com.supplybase.partners.community;

import com.supplybase.partners.common.domain.AuditService;
import com.supplybase.partners.common.domain.ForbiddenException;
import com.supplybase.partners.common.domain.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommunityService {

    private final AnnouncementRepository announcementRepository;
    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;
    private final CommunityReportRepository reportRepository;
    private final AuditService auditService;

    public CommunityService(AnnouncementRepository announcementRepository, CommunityPostRepository postRepository,
                             CommunityCommentRepository commentRepository, CommunityReportRepository reportRepository,
                             AuditService auditService) {
        this.announcementRepository = announcementRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.reportRepository = reportRepository;
        this.auditService = auditService;
    }

    public List<Announcement> announcements() {
        return announcementRepository.findAllByActiveTrueOrderByPublishedAtDesc();
    }

    public Page<CommunityPost> posts(Pageable pageable) {
        return postRepository.findAllByStatusOrderByCreatedAtDesc(CommunityPost.Status.VISIBLE, pageable);
    }

    public List<CommunityComment> comments(Long postId) {
        return commentRepository.findAllByPostIdAndStatusOrderByCreatedAtAsc(postId, CommunityComment.Status.VISIBLE);
    }

    @Transactional
    public CommunityPost createPost(Long partnerId, String body) {
        CommunityPost post = new CommunityPost();
        post.setPartnerId(partnerId);
        post.setBody(body);
        return postRepository.save(post);
    }

    @Transactional
    public CommunityComment createComment(Long partnerId, Long postId, String body) {
        postRepository.findById(postId).orElseThrow(() -> new NotFoundException("Post not found."));
        CommunityComment comment = new CommunityComment();
        comment.setPostId(postId);
        comment.setPartnerId(partnerId);
        comment.setBody(body);
        return commentRepository.save(comment);
    }

    @Transactional
    public CommunityReport report(Long partnerId, CommunityReport.TargetType type, Long targetId, String reason) {
        CommunityReport report = new CommunityReport();
        report.setReportedByPartnerId(partnerId);
        report.setTargetType(type);
        report.setTargetId(targetId);
        report.setReason(reason);
        return reportRepository.save(report);
    }

    @Transactional
    public Announcement publishAnnouncement(Long actorUserId, String title, String body) {
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setBody(body);
        a.setPublishedByUserId(actorUserId);
        return announcementRepository.save(a);
    }

    @Transactional
    public void moderatePost(Long postId, boolean hide, Long actorUserId) {
        CommunityPost post = postRepository.findById(postId).orElseThrow(() -> new NotFoundException("Post not found."));
        post.setStatus(hide ? CommunityPost.Status.HIDDEN : CommunityPost.Status.VISIBLE);
        postRepository.save(post);
        auditService.record(actorUserId, hide ? "POST_HIDDEN" : "POST_UNHIDDEN", "CommunityPost", postId, null);
    }

    @Transactional
    public void resolveReport(Long reportId, boolean dismiss, Long actorUserId) {
        CommunityReport report = reportRepository.findById(reportId).orElseThrow(() -> new NotFoundException("Report not found."));
        report.setStatus(dismiss ? CommunityReport.Status.DISMISSED : CommunityReport.Status.REVIEWED);
        reportRepository.save(report);
    }

    public void requirePartnerOwnsPost(Long postId, Long partnerId) {
        CommunityPost post = postRepository.findById(postId).orElseThrow(() -> new NotFoundException("Post not found."));
        if (!post.getPartnerId().equals(partnerId)) {
            throw new ForbiddenException("You do not own this post.");
        }
    }
}
