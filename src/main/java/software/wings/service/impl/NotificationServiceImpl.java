package software.wings.service.impl;

import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCodes;
import software.wings.beans.Notification;
import software.wings.beans.NotificationAction;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.NotificationService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 7/22/16.
 */
public class NotificationServiceImpl implements NotificationService {
  @Inject private WingsPersistence wingsPersistence;
  private Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public PageResponse<Notification> list(@Valid PageRequest<Notification> pageRequest) {
    return wingsPersistence.query(Notification.class, pageRequest);
  }

  @Override
  public Notification save(Notification notification) {
    return wingsPersistence.saveAndGet(Notification.class, notification);
  }

  @Override
  public Notification get(@NotEmpty String appId, @NotEmpty String notificationId) {
    return wingsPersistence.get(Notification.class, appId, notificationId);
  }

  @Override
  public Notification update(@Valid Notification notification) {
    throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Update operation not supported");
  }

  @Override
  public Notification act(
      @NotEmpty String appId, @NotEmpty String notificationId, @NotNull NotificationAction notificationAction) {
    throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Action operation not supported");
  }
}
