package software.wings.graphql.datafetcher.userGroup;

import static software.wings.graphql.datafetcher.userGroup.UserGroupPermissionsController.populateUserGroupAccountPermissionEntity;
import static software.wings.graphql.datafetcher.userGroup.UserGroupPermissionsController.populateUserGroupAppPermissionEntity;
import static software.wings.graphql.datafetcher.userGroup.UserGroupPermissionsController.populateUserGroupPermissions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.notification.SlackNotificationConfiguration;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.graphql.schema.mutation.userGroup.input.QLCreateUserGroupInput;
import software.wings.graphql.schema.mutation.userGroup.payload.QLCreateUserGroupPayload;
import software.wings.graphql.schema.type.permissions.QLGroupPermissions;
import software.wings.graphql.schema.type.usergroup.QLNotificationSettings;
import software.wings.graphql.schema.type.usergroup.QLSlackNotificationSetting;
import software.wings.graphql.schema.type.usergroup.QLUserGroup.QLUserGroupBuilder;

import java.util.Collections;
import java.util.Set;

@Slf4j
@Singleton
public class UserGroupController {
  @Inject private UserGroupPermissionValidator userGroupPermissionValidator;
  public QLUserGroupBuilder populateUserGroupOutput(UserGroup userGroup, QLUserGroupBuilder builder) {
    QLGroupPermissions permissions = populateUserGroupPermissions(userGroup);
    QLNotificationSettings notificationSettings = populateNotificationSettings(userGroup);
    return builder.name(userGroup.getName())
        .id(userGroup.getUuid())
        .description(userGroup.getDescription())
        .permissions(permissions)
        .isSSOLinked(userGroup.isSsoLinked())
        .importedByScim(userGroup.isImportedByScim())
        .notificationSettings(notificationSettings);
  }

  public QLCreateUserGroupPayload populateCreateUserGroupPayload(UserGroup userGroup) {
    QLGroupPermissions permissions = populateUserGroupPermissions(userGroup);
    QLNotificationSettings notificationSettings = populateNotificationSettings(userGroup);
    return QLCreateUserGroupPayload.builder()
        .name(userGroup.getName())
        .id(userGroup.getUuid())
        .description(userGroup.getDescription())
        .permissions(permissions)
        .isSSOLinked(userGroup.isSsoLinked())
        .importedByScim(userGroup.isImportedByScim())
        .notificationSettings(notificationSettings)
        .build();
  }

  private QLNotificationSettings populateNotificationSettings(UserGroup userGroup) {
    NotificationSettings notificationSettings = userGroup.getNotificationSettings();
    if (notificationSettings == null) {
      return null;
    }
    return QLNotificationSettings.builder()
        .sendNotificationToMembers(notificationSettings.isUseIndividualEmails())
        .sendMailToNewMembers(notificationSettings.isSendMailToNewMembers())
        .groupEmailAddresses(notificationSettings.getEmailAddresses())
        .slackNotificationSetting(populateSlackNotificationSettings(notificationSettings))
        .build();
  }

  private QLSlackNotificationSetting populateSlackNotificationSettings(NotificationSettings notificationSettings) {
    SlackNotificationConfiguration slackconfig = notificationSettings.getSlackConfig();
    if (slackconfig == null) {
      return null;
    }
    return QLSlackNotificationSetting.builder()
        .slackChannelName(slackconfig.getName())
        .slackWebhookURL(slackconfig.getOutgoingWebhookUrl())
        .build();
  }

  private NotificationSettings populateNotificationSettingsEntity(QLNotificationSettings notificationSetting) {
    if (notificationSetting == null) {
      return new NotificationSettings(false, false, Collections.emptyList(), null, "");
    }
    QLSlackNotificationSetting slackNotificationInput = notificationSetting.getSlackNotificationSetting();
    SlackNotificationSetting slackConfig = null;
    if (slackNotificationInput != null) {
      slackConfig = new SlackNotificationSetting(
          slackNotificationInput.getSlackChannelName(), slackNotificationInput.getSlackWebhookURL());
    }

    return new NotificationSettings(notificationSetting.getSendNotificationToMembers(),
        notificationSetting.getSendMailToNewMembers(),
        notificationSetting.getGroupEmailAddresses() == null ? Collections.emptyList()
                                                             : notificationSetting.getGroupEmailAddresses(),
        slackConfig, notificationSetting.getPagerDutyIntegrationKey());
  }

  public UserGroup populateUserGroupEntity(QLCreateUserGroupInput userGroupInput) {
    userGroupPermissionValidator.validatePermission(userGroupInput.getPermissions());
    AccountPermissions accountPermissions = populateUserGroupAccountPermissionEntity(userGroupInput.getPermissions());
    Set<AppPermission> appPermissions = populateUserGroupAppPermissionEntity(userGroupInput.getPermissions());
    return UserGroup.builder()
        .name(userGroupInput.getName())
        .description(userGroupInput.getDescription())
        .notificationSettings(populateNotificationSettingsEntity(userGroupInput.getNotificationSettings()))
        .appPermissions(appPermissions)
        .accountPermissions(accountPermissions)
        .build();
  }
}
