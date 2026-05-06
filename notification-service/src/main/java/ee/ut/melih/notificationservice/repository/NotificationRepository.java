package ee.ut.melih.notificationservice.repository;

import ee.ut.melih.notificationservice.domain.Notification;
import ee.ut.melih.notificationservice.domain.NotificationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  Page<Notification> findByRecipientIdOrderBySentAtDesc(UUID recipientId, Pageable pageable);

  long countByRecipientIdAndStatusNot(UUID recipientId, NotificationStatus status);

  List<Notification> findByRecipientIdAndStatusNot(UUID recipientId, NotificationStatus status);
}
