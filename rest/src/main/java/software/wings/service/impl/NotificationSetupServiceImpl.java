package software.wings.service.impl;

import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.inject.Singleton;

import software.wings.beans.Base;
import software.wings.beans.ErrorCode;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.SettingsService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by rishi on 10/30/16.
 */
@Singleton
@ValidateOnExecution
public class NotificationSetupServiceImpl implements NotificationSetupService {
  @Inject private WingsPersistence wingsPersistence;

  @Inject private SettingsService settingsService;

  /**
   * Gets supported channel type details.
   *
   * @param accountId the app id
   * @return the supported channel type details
   */
  public Map<NotificationChannelType, Object> getSupportedChannelTypeDetails(String accountId) {
    Map<NotificationChannelType, Object> supportedChannelTypeDetails = new HashMap<>();
    for (NotificationChannelType notificationChannelType : NotificationChannelType.values()) {
      if (notificationChannelType.getSettingVariableTypes() != null) {
        List<SettingAttribute> settingAttributes = settingsService.getSettingAttributesByType(
            accountId, notificationChannelType.getSettingVariableTypes().name());
        if (settingAttributes != null && !settingAttributes.isEmpty()) {
          supportedChannelTypeDetails.put(notificationChannelType, new Object());
          // Put more details for the given notificationChannelType, else leave it as blank object.
        }
      }
    }
    return supportedChannelTypeDetails;
  }

  @Override
  public List<NotificationGroup> listNotificationGroups(String accountId) {
    return listNotificationGroups(aPageRequest().addFilter("accountId", Operator.EQ, accountId).build()).getResponse();
  }

  @Override
  public List<NotificationGroup> listNotificationGroups(String accountId, String roleId, String name) {
    return listNotificationGroups(aPageRequest()
                                      .addFilter("accountId", Operator.EQ, accountId)
                                      .addFilter("roleId", Operator.EQ, roleId)
                                      .addFilter("name", Operator.EQ, name)
                                      .build())
        .getResponse();
  }

  @Override
  public PageResponse<NotificationGroup> listNotificationGroups(PageRequest<NotificationGroup> pageRequest) {
    return wingsPersistence.query(NotificationGroup.class, pageRequest);
  }

  @Override
  public NotificationGroup readNotificationGroup(String accountId, String notificationGroupId) {
    return wingsPersistence.get(NotificationGroup.class, Base.GLOBAL_APP_ID, notificationGroupId);
  }

  @Override
  public NotificationGroup createNotificationGroup(NotificationGroup notificationGroup) {
    return wingsPersistence.saveAndGet(NotificationGroup.class, notificationGroup);
  }

  @Override
  public NotificationGroup updateNotificationGroup(NotificationGroup notificationGroup) {
    NotificationGroup existingGroup =
        wingsPersistence.get(NotificationGroup.class, Base.GLOBAL_APP_ID, notificationGroup.getUuid());
    if (!existingGroup.isEditable()) {
      throw new WingsException(ErrorCode.UPDATE_NOT_ALLOWED);
    }
    return wingsPersistence.saveAndGet(NotificationGroup.class, notificationGroup); // TODO:: selective update
  }

  @Override
  public boolean deleteNotificationGroups(String accountId, String notificationGroupId) {
    NotificationGroup notificationGroup =
        wingsPersistence.get(NotificationGroup.class, Base.GLOBAL_APP_ID, notificationGroupId);
    if (!notificationGroup.isEditable()) {
      throw new WingsException(ErrorCode.DELETE_NOT_ALLOWED);
    }
    return wingsPersistence.delete(NotificationGroup.class, Base.GLOBAL_APP_ID, notificationGroupId);
  }

  @Override
  public List<NotificationGroup> listNotificationGroups(String accountId, String name) {
    return listNotificationGroups(
        aPageRequest().addFilter("accountId", Operator.EQ, accountId).addFilter("name", Operator.EQ, name).build())
        .getResponse();
  }
}
