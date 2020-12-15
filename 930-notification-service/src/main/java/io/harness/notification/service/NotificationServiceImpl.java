package io.harness.notification.service;

import io.harness.NotificationRequest;
import io.harness.Team;
import io.harness.ng.beans.PageRequest;
import io.harness.notification.beans.NotificationProcessingResponse;
import io.harness.notification.entities.Notification;
import io.harness.notification.exception.NotificationException;
import io.harness.notification.remote.mappers.NotificationMapper;
import io.harness.notification.repositories.NotificationRepository;
import io.harness.notification.service.api.ChannelService;
import io.harness.notification.service.api.NotificationService;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class NotificationServiceImpl implements NotificationService {
  private final ChannelService channelService;
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
      return !NotificationProcessingResponse.isNotificationResquestFailed(
          previousNotification.get().getProcessingResponses());
    }

    Notification notification = NotificationMapper.toNotification(notificationRequest);

    if (Objects.isNull(notification)) {
      log.error(
          "There is format mismatch between the proto generated class and its equivalent for persistence. Ignoring notification request for processing {}",
          notificationRequest.getId());
      return false;
    }

    notificationRepository.save(notification);
    NotificationProcessingResponse processingResponse = null;
    try {
      processingResponse = channelService.send(notificationRequest);
    } catch (NotificationException e) {
      log.error("Could not send notification.", e);
    }
    notification.setProcessingResponses(processingResponse.getResult());
    notification.setShouldRetry(!(NotificationProcessingResponse.isNotificationResquestFailed(processingResponse)
        || processingResponse.equals(NotificationProcessingResponse.trivialResponseWithNoRetries)));
    notification.setRetries(1);
    notificationRepository.save(notification);
    return !NotificationProcessingResponse.isNotificationResquestFailed(processingResponse);
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
    NotificationProcessingResponse processingResponse = null;
    try {
      processingResponse = channelService.send(notificationRequest);
    } catch (NotificationException e) {
      log.error("Could not send notification.", e);
    }
    notification.setProcessingResponses(processingResponse.getResult());
    notification.setShouldRetry(!(NotificationProcessingResponse.isNotificationResquestFailed(processingResponse)
        || processingResponse.equals(NotificationProcessingResponse.trivialResponseWithNoRetries)));
    notification.setRetries(notification.getRetries() + 1);
    notificationRepository.save(notification);
  }

  @Override
  public Optional<Notification> getnotification(String notificationId) {
    return notificationRepository.findDistinctById(notificationId);
  }

  @Override
  public Page<Notification> list(Team team, PageRequest pageRequest) {
    Criteria criteria = Criteria.where(Notification.NotificationKeys.team).is(team);
    return notificationRepository.findAll(criteria, PageUtils.getPageRequest(pageRequest));
  }
}
