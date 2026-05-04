package ee.ut.melih.notificationservice.service;

import ee.ut.melih.notificationservice.domain.Notification;

public interface MessageDispatcher {
  void dispatch(Notification notification);
}
