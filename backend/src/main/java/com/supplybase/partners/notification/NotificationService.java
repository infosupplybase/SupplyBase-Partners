package com.supplybase.partners.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Outbox pattern: every notification is durably queued first, then an
 * attempt is recorded. In local/demo mode delivery is simulated (logged,
 * never actually sent) and immediately marked SENT; a real provider
 * integration would instead have a worker poll QUEUED rows and record
 * SUCCESS/FAILURE attempts with retry/backoff.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationDeliveryAttemptRepository attemptRepository;
    private final NotificationReadStateRepository readStateRepository;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationOutboxRepository outboxRepository,
                                NotificationDeliveryAttemptRepository attemptRepository,
                                NotificationReadStateRepository readStateRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.attemptRepository = attemptRepository;
        this.readStateRepository = readStateRepository;
        this.objectMapper = objectMapper;
    }

    /** Preferred over the raw-String overload: serializes the payload as real JSON rather than trusting a hand-built string. */
    @Transactional
    public NotificationOutbox enqueue(Long userId, NotificationOutbox.Channel channel, String templateKey,
                                       String language, Map<String, Object> payload) {
        return enqueue(userId, channel, templateKey, language, objectMapper.writeValueAsString(payload));
    }

    @Transactional
    public NotificationOutbox enqueue(Long userId, NotificationOutbox.Channel channel, String templateKey,
                                       String language, String payloadJson) {
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setUserId(userId);
        outbox.setChannel(channel);
        outbox.setTemplateKey(templateKey);
        outbox.setLanguage(language == null ? "en" : language);
        outbox.setPayloadJson(payloadJson);
        outbox.setStatus(NotificationOutbox.Status.QUEUED);
        outboxRepository.save(outbox);

        dispatchDevLocal(outbox);
        return outbox;
    }

    private void dispatchDevLocal(NotificationOutbox outbox) {
        log.info("[DEV_LOCAL {}] template={} user={} payload={} (preview only, nothing actually sent)",
                outbox.getChannel(), outbox.getTemplateKey(), outbox.getUserId(), outbox.getPayloadJson());
        outbox.setAttempts(outbox.getAttempts() + 1);
        outbox.setLastAttemptAt(Instant.now());
        outbox.setStatus(NotificationOutbox.Status.SENT);
        outboxRepository.save(outbox);

        NotificationDeliveryAttempt attempt = new NotificationDeliveryAttempt();
        attempt.setOutboxId(outbox.getId());
        attempt.setAttemptNumber(outbox.getAttempts());
        attempt.setProvider("DEV_LOCAL");
        attempt.setStatus(NotificationDeliveryAttempt.Status.SUCCESS);
        attemptRepository.save(attempt);
    }

    public List<NotificationOutbox> inboxFor(Long userId) {
        return outboxRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markRead(Long userId, Long outboxId) {
        if (readStateRepository.findByUserIdAndOutboxId(userId, outboxId).isPresent()) {
            return;
        }
        NotificationReadState state = new NotificationReadState();
        state.setUserId(userId);
        state.setOutboxId(outboxId);
        readStateRepository.save(state);
    }
}
