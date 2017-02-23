package software.wings.service.impl;

import static software.wings.beans.ErrorCode.INVALID_REQUEST;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;

import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ActionableNotification;
import software.wings.beans.Application;
import software.wings.beans.Notification;
import software.wings.beans.NotificationAction.NotificationActionType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.NotificationDispatcherService;
import software.wings.service.intfc.NotificationService;

import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 7/22/16.
 */
@Singleton
@ValidateOnExecution
public class NotificationServiceImpl implements NotificationService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private Injector injector;
  @Inject private ExecutorService executorService;
  @Inject private AppService appService;
  @Inject private NotificationDispatcherService notificationDispatcherService;

  private Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public PageResponse<Notification> list(PageRequest<Notification> pageRequest) {
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
  public Notification act(
      @NotEmpty String appId, @NotEmpty String notificationId, @NotNull NotificationActionType actionType) {
    Notification notification = get(appId, notificationId);
    if (notification == null) {
      throw new WingsException(INVALID_REQUEST, "message", "Notification doesn't exist");
    }
    if (!(notification instanceof ActionableNotification)) {
      throw new WingsException(INVALID_REQUEST, "message", "Notification not actionable");
    }
    ActionableNotification actionableNotification = (ActionableNotification) notification;
    if (actionableNotification.getNotificationActions().stream().noneMatch(
            notificationAction -> notificationAction.getType() == actionType)) {
      throw new WingsException(INVALID_REQUEST, "message", "Action not supported for NotificationType");
    }
    injector.injectMembers(actionableNotification);
    boolean actionCompleted = actionableNotification.performAction(actionType);
    if (actionCompleted) {
      markNotificationCompleted(appId, notificationId);
    }
    Notification savedNotification = get(appId, notificationId);
    notificationDispatcherService.dispatchNotification(savedNotification);
    return savedNotification;
  }

  @Override
  public void markNotificationCompleted(@NotEmpty String appId, @NotEmpty String notificationId) {
    wingsPersistence.updateFields(Notification.class, notificationId, ImmutableMap.of("complete", true));
  }

  @Override
  public void deleteByApplication(String appId) {
    wingsPersistence.delete(wingsPersistence.createQuery(Notification.class).field("appId").equal(appId));
  }

  @Override
  public void sendNotificationAsync(@Valid Notification notification) {
    executorService.execute(() -> sendNotification(notification));
  }

  private void sendNotification(Notification notification) {
    Application application = appService.get(notification.getAppId());
    notification.setAccountId(application.getAccountId());
    Notification savedNotification = save(notification);
    notificationDispatcherService.dispatchNotification(savedNotification);
  }
}
