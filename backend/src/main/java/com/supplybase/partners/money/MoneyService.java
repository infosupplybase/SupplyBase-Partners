package com.supplybase.partners.money;

import com.supplybase.partners.common.domain.AuditService;
import com.supplybase.partners.common.domain.BadRequestException;
import com.supplybase.partners.common.domain.NotFoundException;
import com.supplybase.partners.partner.BankAccount;
import com.supplybase.partners.partner.BankAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * All INR amounts are integer paise throughout. recordEarningForCompletedJob
 * is guarded both here (existence check) and at the database level (a unique
 * constraint on earning_entry.service_request_id) so a completed job can
 * never post two earning entries even under a retried request.
 */
@Service
public class MoneyService {

    private final EarningEntryRepository earningEntryRepository;
    private final EarningAdjustmentRepository earningAdjustmentRepository;
    private final PayoutRepository payoutRepository;
    private final PayoutEventRepository payoutEventRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AuditService auditService;
    private final String payoutsMode;

    public MoneyService(EarningEntryRepository earningEntryRepository, EarningAdjustmentRepository earningAdjustmentRepository,
                         PayoutRepository payoutRepository, PayoutEventRepository payoutEventRepository,
                         BankAccountRepository bankAccountRepository, AuditService auditService,
                         @Value("${supplybase.providers.payouts.mode}") String payoutsMode) {
        this.earningEntryRepository = earningEntryRepository;
        this.earningAdjustmentRepository = earningAdjustmentRepository;
        this.payoutRepository = payoutRepository;
        this.payoutEventRepository = payoutEventRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.auditService = auditService;
        this.payoutsMode = payoutsMode;
    }

    @Transactional
    public EarningEntry recordEarningForCompletedJob(Long partnerId, Long serviceRequestId, long jobValuePaise, long partnerEarningPaise) {
        if (earningEntryRepository.findByServiceRequestId(serviceRequestId).isPresent()) {
            return earningEntryRepository.findByServiceRequestId(serviceRequestId).get();
        }
        EarningEntry entry = new EarningEntry();
        entry.setPartnerId(partnerId);
        entry.setServiceRequestId(serviceRequestId);
        entry.setGrossPaise(jobValuePaise);
        entry.setCommissionPaise(jobValuePaise - partnerEarningPaise);
        entry.setNetPaise(partnerEarningPaise);
        entry.setStatus(EarningEntry.Status.EARNED);
        Instant now = Instant.now();
        entry.setEarnedAt(now);
        entry.setAvailableAt(now);
        return earningEntryRepository.save(entry);
    }

    public record Summary(long estimatedPaise, long earnedPaise, long availablePaise, long scheduledPaise, long paidPaise) {}

    public Summary summary(Long partnerId, Instant from, Instant to) {
        List<EarningEntry> entries = earningEntryRepository.findAllByPartnerIdAndEarnedAtBetweenOrderByEarnedAtDesc(partnerId, from, to);
        long earned = entries.stream().filter(e -> e.getStatus() == EarningEntry.Status.EARNED).mapToLong(EarningEntry::getNetPaise).sum();
        long adjustments = earningAdjustmentRepository.findAllByPartnerIdOrderByCreatedAtDesc(partnerId).stream()
                .mapToLong(a -> switch (a.getType()) {
                    case BONUS -> a.getAmountPaise();
                    case PENALTY, DEDUCTION, REVERSAL -> -a.getAmountPaise();
                }).sum();
        long paid = payoutRepository.findAllByPartnerIdOrderByRequestedAtDesc(partnerId).stream()
                .filter(p -> p.getStatus() == Payout.Status.PAID || p.getStatus() == Payout.Status.MANUAL_RECONCILED)
                .mapToLong(Payout::getAmountPaise).sum();
        long inFlight = payoutRepository.findAllByPartnerIdOrderByRequestedAtDesc(partnerId).stream()
                .filter(p -> p.getStatus() == Payout.Status.REQUESTED || p.getStatus() == Payout.Status.SCHEDULED || p.getStatus() == Payout.Status.PROCESSING)
                .mapToLong(Payout::getAmountPaise).sum();
        long available = earned + adjustments - paid - inFlight;
        return new Summary(earned, earned, Math.max(0, available), inFlight, paid);
    }

    public List<EarningEntry> ledger(Long partnerId) {
        return earningEntryRepository.findAllByPartnerIdOrderByEarnedAtDesc(partnerId);
    }

    public List<EarningAdjustment> adjustments(Long partnerId) {
        return earningAdjustmentRepository.findAllByPartnerIdOrderByCreatedAtDesc(partnerId);
    }

    public List<Payout> payouts(Long partnerId) {
        return payoutRepository.findAllByPartnerIdOrderByRequestedAtDesc(partnerId);
    }

    @Transactional
    public Payout requestPayout(Long partnerId, long amountPaise) {
        if (amountPaise <= 0) {
            throw new BadRequestException("Payout amount must be positive.");
        }
        BankAccount account = bankAccountRepository.findByPartnerId(partnerId)
                .orElseThrow(() -> new BadRequestException("Add a bank account before requesting a payout."));
        if (account.getVerificationStatus() == BankAccount.VerificationStatus.FAILED) {
            throw new BadRequestException("Your bank account failed verification; update it before requesting a payout.");
        }
        Summary summary = summary(partnerId, Instant.EPOCH, Instant.now());
        if (amountPaise > summary.availablePaise()) {
            throw new BadRequestException("Requested amount exceeds your available balance.");
        }

        Payout payout = new Payout();
        payout.setPartnerId(partnerId);
        payout.setBankAccountId(account.getId());
        payout.setAmountPaise(amountPaise);
        payout.setStatus(Payout.Status.REQUESTED);
        payoutRepository.save(payout);

        if ("dev_simulator".equalsIgnoreCase(payoutsMode)) {
            payout.setStatus(Payout.Status.PAID);
            payout.setProviderReference("DEV-SIM-" + UUID.randomUUID());
            payout.setPaidAt(Instant.now());
            payoutRepository.save(payout);
            PayoutEvent event = new PayoutEvent();
            event.setPayoutId(payout.getId());
            event.setEventType("PAID_SIMULATED");
            payoutEventRepository.save(event);
        }
        return payout;
    }

    @Transactional
    public EarningAdjustment adminAdjust(Long partnerId, EarningAdjustment.Type type, long amountPaise, String reason, Long actorUserId) {
        EarningAdjustment adjustment = new EarningAdjustment();
        adjustment.setPartnerId(partnerId);
        adjustment.setType(type);
        adjustment.setAmountPaise(amountPaise);
        adjustment.setReason(reason);
        adjustment.setCreatedByUserId(actorUserId);
        earningAdjustmentRepository.save(adjustment);
        auditService.record(actorUserId, "EARNING_ADJUSTMENT_" + type, "Partner", partnerId, reason);
        return adjustment;
    }

    @Transactional
    public EarningEntry reverseEarning(Long earningEntryId, String reason, Long actorUserId) {
        EarningEntry entry = earningEntryRepository.findById(earningEntryId)
                .orElseThrow(() -> new NotFoundException("Earning entry not found."));
        if (entry.getStatus() == EarningEntry.Status.REVERSED) {
            throw new BadRequestException("This earning entry is already reversed.");
        }
        entry.setStatus(EarningEntry.Status.REVERSED);
        earningEntryRepository.save(entry);

        EarningAdjustment adjustment = new EarningAdjustment();
        adjustment.setPartnerId(entry.getPartnerId());
        adjustment.setEarningEntryId(entry.getId());
        adjustment.setType(EarningAdjustment.Type.REVERSAL);
        adjustment.setAmountPaise(entry.getNetPaise());
        adjustment.setReason(reason);
        adjustment.setCreatedByUserId(actorUserId);
        earningAdjustmentRepository.save(adjustment);
        auditService.record(actorUserId, "EARNING_REVERSED", "EarningEntry", earningEntryId, reason);
        return entry;
    }

    /** Idempotent: a duplicate provider_event_id delivery is a no-op (unique index also enforces this at the DB). */
    @Transactional
    public void handlePayoutWebhookEvent(Long payoutId, String eventType, String providerEventId, String payloadJson) {
        if (providerEventId != null && payoutEventRepository.findByProviderEventId(providerEventId).isPresent()) {
            return;
        }
        Payout payout = payoutRepository.findById(payoutId).orElseThrow(() -> new NotFoundException("Payout not found."));
        PayoutEvent event = new PayoutEvent();
        event.setPayoutId(payoutId);
        event.setEventType(eventType);
        event.setProviderEventId(providerEventId);
        event.setPayloadJson(payloadJson);
        payoutEventRepository.save(event);

        switch (eventType) {
            case "PAID" -> {
                payout.setStatus(Payout.Status.PAID);
                payout.setPaidAt(Instant.now());
            }
            case "FAILED" -> payout.setStatus(Payout.Status.FAILED);
            default -> { /* informational event, no status change */ }
        }
        payoutRepository.save(payout);
    }
}
