package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.NotificationGroup;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import javax.validation.Valid;

/**
 * Created by rishi on 10/30/16.
 */
public interface NotificationSetupService {
  /**
   * List notification groups list.
   *
   * @param appId the app id
   * @return the list
   */
  List<NotificationGroup> listNotificationGroups(String appId);

  /**
   * List notification groups page response.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<NotificationGroup> listNotificationGroups(PageRequest<NotificationGroup> pageRequest);

  /**
   * Read notification group notification group.
   *
   * @param appId               the app id
   * @param notificationGroupId the notification group id
   * @return the notification group
   */
  NotificationGroup readNotificationGroup(String appId, String notificationGroupId);

  /**
   * Create notification group notification group.
   *
   * @param notificationGroup the notification group
   * @return the notification group
   */
  @ValidationGroups(Create.class) NotificationGroup createNotificationGroup(@Valid NotificationGroup notificationGroup);

  /**
   * Update notification group notification group.
   *
   * @param notificationGroup the notification group
   * @return the notification group
   */
  @ValidationGroups(Update.class) NotificationGroup updateNotificationGroup(@Valid NotificationGroup notificationGroup);

  /**
   * Delete notification groups boolean.
   *
   * @param appId               the app id
   * @param notificationGroupId the notification group id
   * @return the boolean
   */
  boolean deleteNotificationGroups(@NotEmpty String appId, @NotEmpty String notificationGroupId);
}
