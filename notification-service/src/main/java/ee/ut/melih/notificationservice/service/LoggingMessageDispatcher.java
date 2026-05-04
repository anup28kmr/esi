package ee.ut.melih.notificationservice.service;

import ee.ut.melih.notificationservice.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingMessageDispatcher implements MessageDispatcher {

  private static final Logger log = LoggerFactory.getLogger(LoggingMessageDispatcher.class);

  @Override
  public void dispatch(Notification notification) {
    log.info(
        "Dispatching notification id={} channel={} recipient={}",
        notification.getId(),
        notification.getChannel(),
        notification.getRecipientId());
  }
}
