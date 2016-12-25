package software.wings.service.impl;

import static software.wings.dl.PageRequest.Builder.aPageRequest;

import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.SettingsService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by rishi on 10/30/16.
 */
public class NotificationSetupServiceImpl implements NotificationSetupService {
  @Inject private WingsPersistence wingsPersistence;

  @Inject private SettingsService settingsService;

  @Override
  public Map<NotificationChannelType, Object> getSupportedChannelTypeDetails(String appId) {
    Map<NotificationChannelType, Object> supportedChannelTypeDetails = new HashMap<>();
    for (NotificationChannelType notificationChannelType : NotificationChannelType.values()) {
      if (notificationChannelType.getSettingVariableTypes() != null) {
        List<SettingAttribute> settingAttributes =
            settingsService.getSettingAttributesByType(appId, notificationChannelType.getSettingVariableTypes().name());
        if (settingAttributes != null && !settingAttributes.isEmpty()) {
          supportedChannelTypeDetails.put(notificationChannelType, new Object());
          // Put more details for the given notificationChannelType, else leave it as blank object.
        }
      }
    }
    return supportedChannelTypeDetails;
  }

  @Override
  public List<NotificationGroup> listNotificationGroups(String appId) {
    return listNotificationGroups(aPageRequest().addFilter("appId", Operator.EQ, appId).build()).getResponse();
  }

  @Override
  public PageResponse<NotificationGroup> listNotificationGroups(PageRequest<NotificationGroup> pageRequest) {
    return wingsPersistence.query(NotificationGroup.class, pageRequest);
  }

  @Override
  public NotificationGroup readNotificationGroup(String appId, String notificationGroupId) {
    return wingsPersistence.get(NotificationGroup.class, appId, notificationGroupId);
  }

  @Override
  public NotificationGroup createNotificationGroup(NotificationGroup notificationGroup) {
    return wingsPersistence.saveAndGet(NotificationGroup.class, notificationGroup);
  }

  @Override
  public NotificationGroup updateNotificationGroup(NotificationGroup notificationGroup) {
    // TODO:
    return null;
  }

  @Override
  public boolean deleteNotificationGroups(String appId, String notificationGroupId) {
    return wingsPersistence.delete(NotificationGroup.class, appId, notificationGroupId);
  }

  @Override
  public List<NotificationRule> listNotificationRules(String appId) {
    return wingsPersistence.query(NotificationRule.class, aPageRequest().addFilter("appId", Operator.EQ, appId).build())
        .getResponse();
  }

  @Override
  public PageResponse<NotificationRule> listNotificationRules(PageRequest<NotificationRule> pageRequest) {
    return wingsPersistence.query(NotificationRule.class, pageRequest);
  }

  @Override
  public NotificationRule readNotificationRule(String appId, String notificationRuleId) {
    return wingsPersistence.get(NotificationRule.class, appId, notificationRuleId);
  }

  @Override
  public NotificationRule createNotificationRule(NotificationRule notificationRule) {
    return wingsPersistence.saveAndGet(NotificationRule.class, notificationRule);
  }

  @Override
  public NotificationRule updateNotificationRule(NotificationRule notificationRule) {
    // TODO:
    return null;
  }

  @Override
  public boolean deleteNotificationRule(String appId, String notificationRuleId) {
    return wingsPersistence.delete(NotificationRule.class, appId, notificationRuleId);
  }
}
