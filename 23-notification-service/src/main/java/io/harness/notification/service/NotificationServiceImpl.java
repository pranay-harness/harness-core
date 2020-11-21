package io.harness.notification.service;

import io.harness.NotificationRequest;
import io.harness.notification.NotificationRequestProcessor;
import io.harness.notification.entities.Notification;
import io.harness.notification.remote.mappers.NotificationMapper;
import io.harness.notification.repositories.NotificationRepository;
import io.harness.notification.service.api.NotificationService;

import com.google.inject.Inject;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class NotificationServiceImpl implements NotificationService {
  private final NotificationRequestProcessor notificationRequestProcessor;
  private final NotificationRepository notificationRepository;

  @Override
  public boolean processNewMessage(NotificationRequest notificationRequest) {
    if (Objects.isNull(notificationRequest.getId())) {
      return false;
    }
    log.info("Received Message in group 'notification_microservice_v1': {}", notificationRequest.getId());
    Optional<Notification> previousNotification = notificationRepository.findDistinctById(notificationRequest.getId());
    if (previousNotification.isPresent()) {
      log.info("Duplicate notification request recieved {}", notificationRequest.getId());
      return previousNotification.get().getSent();
    }

    Notification notification = NotificationMapper.toNotification(notificationRequest);

    if (Objects.isNull(notification)) {
      log.error(
          "There is format mismatch between the proto generated class and its equivalent for persistence. Ignoring notification request for processing {}",
          notificationRequest.getId());
      return false;
    }

    notificationRepository.save(notification);
    boolean sent = notificationRequestProcessor.process(notificationRequest);
    notification.setSent(sent);
    notification.setRetries(1);
    notificationRepository.save(notification);
    return sent;
  }

  @Override
  public void processRetries(Notification notification) {
    NotificationRequest notificationRequest = NotificationMapper.toNotificationRequest(notification);
    if (Objects.isNull(notification)) {
      log.error(
          "There is format mismatch between the proto generated class and its equivalent for persistence. Ignoring notification request for processing {}",
          notificationRequest.getId());
      return;
    }
    log.info("Retrying sending notification {}", notificationRequest.getId());
    boolean sent = notificationRequestProcessor.process(notificationRequest);
    notification.setSent(sent);
    notification.setRetries(notification.getRetries() + 1);
    notificationRepository.save(notification);
  }
}
