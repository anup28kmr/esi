package ee.ut.melih.notificationservice.service;

import ee.ut.melih.notificationservice.domain.Notification;
import ee.ut.melih.notificationservice.domain.NotificationStatus;
import ee.ut.melih.notificationservice.dto.SendNotificationRequest;
import ee.ut.melih.notificationservice.exception.DispatchException;
import ee.ut.melih.notificationservice.exception.NotificationNotFoundException;
import ee.ut.melih.notificationservice.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private final NotificationRepository repository;
  private final MessageDispatcher dispatcher;

  public NotificationService(NotificationRepository repository, MessageDispatcher dispatcher) {
    this.repository = repository;
    this.dispatcher = dispatcher;
  }

  @Transactional(readOnly = true)
  public Page<Notification> list(UUID recipientId, Pageable pageable) {
    return repository.findByRecipientIdOrderBySentAtDesc(recipientId, pageable);
  }

  @Transactional(readOnly = true)
  public Notification get(UUID id, UUID recipientId) {
    Notification notification =
        repository.findById(id).orElseThrow(() -> new NotificationNotFoundException(id));
    if (!notification.getRecipientId().equals(recipientId)) {
      throw new NotificationNotFoundException(id);
    }
    return notification;
  }

  @Transactional(readOnly = true)
  public long unreadCount(UUID recipientId) {
    return repository.countByRecipientIdAndStatusNot(recipientId, NotificationStatus.READ);
  }

  @Transactional
  public Notification markRead(UUID id, UUID recipientId) {
    Notification notification = get(id, recipientId);
    notification.markRead();
    return repository.save(notification);
  }

  @Transactional
  public int markAllRead(UUID recipientId) {
    List<Notification> unread =
        repository.findByRecipientIdAndStatusNot(recipientId, NotificationStatus.READ);
    unread.forEach(Notification::markRead);
    repository.saveAll(unread);
    return unread.size();
  }

  @Transactional
  public Notification send(SendNotificationRequest request) {
    Notification notification =
        new Notification(request.recipientId(), request.channel(), request.message(), request.eventType());
    notification = repository.save(notification);
    try {
      dispatcher.dispatch(notification);
      notification.markSent(Instant.now());
    } catch (RuntimeException ex) {
      log.warn("Dispatch failed for notification {}: {}", notification.getId(), ex.getMessage());
      notification.markFailed();
      repository.save(notification);
      throw new DispatchException("Failed to dispatch notification", ex);
    }
    return repository.save(notification);
  }
}
