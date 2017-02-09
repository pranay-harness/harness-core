package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 10/30/16.
 */
public interface NotificationSetupService {
  Map<NotificationChannelType, Object> getSupportedChannelTypeDetails(String appId);

  List<NotificationGroup> listNotificationGroups(String appId);

  PageResponse<NotificationGroup> listNotificationGroups(PageRequest<NotificationGroup> pageRequest);

  NotificationGroup readNotificationGroup(String appId, String notificationGroupId);

  NotificationGroup createNotificationGroup(NotificationGroup notificationGroup);

  NotificationGroup updateNotificationGroup(NotificationGroup notificationGroup);

  boolean deleteNotificationGroups(@NotEmpty String appId, @NotEmpty String notificationGroupId);

  List<NotificationRule> listNotificationRules(String appId);

  PageResponse<NotificationRule> listNotificationRules(PageRequest<NotificationRule> pageRequest);

  NotificationRule readNotificationRule(String appId, String notificationRuleId);

  NotificationRule createNotificationRule(NotificationRule notificationRule);

  NotificationRule updateNotificationRule(NotificationRule notificationRule);

  boolean deleteNotificationRule(@NotEmpty String appId, @NotEmpty String notificationRuleId);
}
