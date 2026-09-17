package com.supplybase.partners.kit;

import com.supplybase.partners.common.domain.AuditService;
import com.supplybase.partners.common.domain.BadRequestException;
import com.supplybase.partners.common.domain.ForbiddenException;
import com.supplybase.partners.common.domain.NotFoundException;
import com.supplybase.partners.notification.NotificationOutbox;
import com.supplybase.partners.notification.NotificationService;
import com.supplybase.partners.partner.OnboardingStage;
import com.supplybase.partners.partner.Partner;
import com.supplybase.partners.partner.PartnerRepository;
import com.supplybase.partners.partner.PartnerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Kit ordering is idempotent on a client-supplied key: retrying a booking
 * request (e.g. after a flaky connection) returns the original order instead
 * of creating a duplicate. Payment completion only ever comes from the
 * dev simulator here (synchronous, clearly labelled) or, in a real deployment,
 * a verified provider webhook event -- never a bare frontend "success" flag.
 */
@Service
public class KitOrderService {

    private final StarterKitRepository starterKitRepository;
    private final StarterKitItemRepository starterKitItemRepository;
    private final KitOrderRepository kitOrderRepository;
    private final KitOrderEventRepository kitOrderEventRepository;
    private final PartnerService partnerService;
    private final PartnerRepository partnerRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final String paymentsMode;

    public KitOrderService(StarterKitRepository starterKitRepository, StarterKitItemRepository starterKitItemRepository,
                            KitOrderRepository kitOrderRepository, KitOrderEventRepository kitOrderEventRepository,
                            PartnerService partnerService, PartnerRepository partnerRepository, AuditService auditService,
                            NotificationService notificationService,
                            @Value("${supplybase.providers.kit-payments.mode}") String paymentsMode) {
        this.starterKitRepository = starterKitRepository;
        this.starterKitItemRepository = starterKitItemRepository;
        this.kitOrderRepository = kitOrderRepository;
        this.kitOrderEventRepository = kitOrderEventRepository;
        this.partnerService = partnerService;
        this.partnerRepository = partnerRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.paymentsMode = paymentsMode;
    }

    public record Quote(StarterKit kit, List<StarterKitItem> items) {}

    public Optional<Quote> quoteFor(Partner partner) {
        if (partner.getPrimaryCategoryId() == null) {
            return Optional.empty();
        }
        return starterKitRepository.findFirstByCategoryIdAndActiveTrue(partner.getPrimaryCategoryId())
                .map(kit -> new Quote(kit, starterKitItemRepository.findAllByKitId(kit.getId())));
    }

    public List<KitOrder> ordersFor(Long partnerId) {
        return kitOrderRepository.findAllByPartnerIdOrderByCreatedAtDesc(partnerId);
    }

    public List<KitOrderEvent> eventsFor(Long kitOrderId) {
        return kitOrderEventRepository.findAllByKitOrderIdOrderByCreatedAtAsc(kitOrderId);
    }

    @Transactional
    public KitOrder book(Partner partner, String deliveryAddressLine, Long deliveryCityId, String idempotencyKey) {
        Optional<KitOrder> existing = kitOrderRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (partner.getOnboardingStage() != OnboardingStage.STARTER_KIT) {
            throw new BadRequestException("The starter kit can only be booked after screening has passed.");
        }
        StarterKit kit = starterKitRepository.findFirstByCategoryIdAndActiveTrue(partner.getPrimaryCategoryId())
                .orElseThrow(() -> new BadRequestException("No starter kit is configured for your category."));

        KitOrder order = new KitOrder();
        order.setPartnerId(partner.getId());
        order.setKitId(kit.getId());
        order.setDeliveryAddressLine(deliveryAddressLine);
        order.setDeliveryCityId(deliveryCityId);
        order.setPricePaise(kit.getPricePaise());
        order.setFeesPaise(kit.getFeesPaise());
        order.setTotalPaise(kit.getPricePaise() + kit.getFeesPaise());
        order.setStatus(KitOrder.Status.PENDING_PAYMENT);
        order.setIdempotencyKey(idempotencyKey);

        try {
            kitOrderRepository.save(order);
        } catch (DataIntegrityViolationException e) {
            // A concurrent retry with the same key won the race; return that row instead of failing.
            return kitOrderRepository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> e);
        }
        kitOrderEventRepository.save(new KitOrderEvent(order.getId(), KitOrderEvent.EventType.CREATED));

        if ("dev_simulator".equalsIgnoreCase(paymentsMode)) {
            simulatePaymentSuccess(order);
        }
        return order;
    }

    private void simulatePaymentSuccess(KitOrder order) {
        kitOrderEventRepository.save(new KitOrderEvent(order.getId(), KitOrderEvent.EventType.PAYMENT_INITIATED));
        order.setStatus(KitOrder.Status.PROCESSING);
        kitOrderRepository.save(order);
        kitOrderEventRepository.save(new KitOrderEvent(order.getId(), KitOrderEvent.EventType.PAYMENT_SUCCEEDED));
    }

    @Transactional
    public KitOrder cancel(Long partnerId, Long orderId) {
        KitOrder order = requireOwned(orderId, partnerId);
        if (order.getStatus() == KitOrder.Status.SHIPPED || order.getStatus() == KitOrder.Status.DELIVERED) {
            throw new BadRequestException("A shipped or delivered order can no longer be cancelled here.");
        }
        boolean wasPaid = order.getStatus() == KitOrder.Status.PAID || order.getStatus() == KitOrder.Status.PROCESSING;
        order.setStatus(KitOrder.Status.CANCELLED);
        kitOrderRepository.save(order);
        kitOrderEventRepository.save(new KitOrderEvent(order.getId(), KitOrderEvent.EventType.CANCELLED));
        if (wasPaid) {
            kitOrderEventRepository.save(new KitOrderEvent(order.getId(), KitOrderEvent.EventType.REFUNDED));
        }
        return order;
    }

    @Transactional
    public KitOrder adminUpdateStatus(Long orderId, KitOrder.Status newStatus, Long actorUserId) {
        KitOrder order = kitOrderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found."));
        order.setStatus(newStatus);
        kitOrderRepository.save(order);
        KitOrderEvent.EventType eventType = switch (newStatus) {
            case SHIPPED -> KitOrderEvent.EventType.SHIPPED;
            case DELIVERED -> KitOrderEvent.EventType.DELIVERED;
            case CANCELLED -> KitOrderEvent.EventType.CANCELLED;
            case REFUNDED -> KitOrderEvent.EventType.REFUNDED;
            default -> null;
        };
        if (eventType != null) {
            kitOrderEventRepository.save(new KitOrderEvent(order.getId(), eventType));
        }
        auditService.record(actorUserId, "KIT_ORDER_STATUS_CHANGED", "KitOrder", order.getId(), "New status: " + newStatus);
        partnerRepository.findById(order.getPartnerId()).ifPresent(p -> notificationService.enqueue(
                p.getUserId(), NotificationOutbox.Channel.INAPP, "kit.order.status",
                null, "{\"orderId\":" + order.getId() + ",\"status\":\"" + newStatus + "\"}"));

        if (newStatus == KitOrder.Status.DELIVERED) {
            partnerService.advanceStageIfCurrent(order.getPartnerId(), OnboardingStage.STARTER_KIT, OnboardingStage.PROFILE);
        }
        return order;
    }

    /** If no kit is configured for the partner's category, the starter-kit gate is not applicable. */
    @Transactional
    public boolean skipIfNotRequired(Partner partner) {
        if (partner.getOnboardingStage() != OnboardingStage.STARTER_KIT) {
            return false;
        }
        boolean kitExists = starterKitRepository.findFirstByCategoryIdAndActiveTrue(partner.getPrimaryCategoryId()).isPresent();
        if (kitExists) {
            return false;
        }
        partnerService.advanceStageIfCurrent(partner.getId(), OnboardingStage.STARTER_KIT, OnboardingStage.PROFILE);
        return true;
    }

    @Transactional
    public void adminExempt(Long partnerId, String reason, Long actorUserId) {
        partnerService.advanceStageIfCurrent(partnerId, OnboardingStage.STARTER_KIT, OnboardingStage.PROFILE);
        auditService.record(actorUserId, "KIT_REQUIREMENT_EXEMPTED", "Partner", partnerId, reason);
    }

    private KitOrder requireOwned(Long orderId, Long partnerId) {
        KitOrder order = kitOrderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found."));
        if (!order.getPartnerId().equals(partnerId)) {
            throw new ForbiddenException("You do not own this order.");
        }
        return order;
    }
}
