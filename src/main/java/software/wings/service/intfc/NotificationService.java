package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Notification;
import software.wings.beans.NotificationAction;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 7/22/16.
 */
public interface NotificationService {
  /**
   * List page response.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<Notification> list(PageRequest<Notification> pageRequest);

  /**
   * Save notification.
   *
   * @param notification the notification
   * @return the notification
   */
  @ValidationGroups(Create.class) Notification save(Notification notification);

  /**
   * Get notification.
   *
   * @param appId          the app id
   * @param notificationId the notification id
   * @return the notification
   */
  Notification get(@NotEmpty String appId, @NotEmpty String notificationId);

  /**
   * Update notification.
   *
   * @param notification the notification
   * @return the notification
   */
  @ValidationGroups(Update.class) Notification update(@Valid Notification notification);

  /**
   * Act notification.
   *
   * @param appId              the app id
   * @param notificationId     the notification id
   * @param notificationAction the notification action
   * @return the notification
   */
  Notification act(
      @NotEmpty String appId, @NotEmpty String notificationId, @NotNull NotificationAction notificationAction);
}
