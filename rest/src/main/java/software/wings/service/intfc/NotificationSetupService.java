package software.wings.service.intfc;

import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.NotificationGroup;
import software.wings.beans.Role;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.util.List;
import javax.validation.Valid;

/**
 * Created by rishi on 10/30/16.
 */
public interface NotificationSetupService {
  /**
   * List notification groups list.
   *
   * @param accountId the account Id
   * @return the list
   */
  List<NotificationGroup> listNotificationGroups(String accountId);

  /**
   * List notification groups
   * @param accountId
   * @param role the role id
   * @param name
   * @return the list
   */
  List<NotificationGroup> listNotificationGroups(String accountId, Role role, String name);

  /**
   * Finds the notificaiton group
   * @param accountId
   * @param name
   * @return
   */
  List<NotificationGroup> listNotificationGroups(String accountId, String name);

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
   * @param accountId           the account id
   * @param notificationGroupId the notification group id
   * @return the notification group
   */
  NotificationGroup readNotificationGroup(String accountId, String notificationGroupId);

  List<NotificationGroup> readNotificationGroups(String accountId, List<String> notificationGroupIds);

  NotificationGroup readNotificationGroupByName(String accountId, String notificationGroupName);

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
   * @param accountId           the account id
   * @param notificationGroupId the notification group id
   * @return the boolean
   */
  boolean deleteNotificationGroups(@NotEmpty String accountId, @NotEmpty String notificationGroupId);

  /**
   *
   * @param accountId
   * @return
   */
  List<NotificationGroup> listDefaultNotificationGroup(String accountId);

  List<String> getUserEmailAddressFromNotificationGroups(String accountId, List<NotificationGroup> notificationGroups);

  boolean deleteNotificationGroups(String accountId, String notificationGroupId, boolean syncFromGit);
}
