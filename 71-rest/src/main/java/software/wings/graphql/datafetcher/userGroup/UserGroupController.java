package software.wings.graphql.datafetcher.userGroup;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.graphql.datafetcher.userGroup.UserGroupPermissionsController.populateUserGroupAccountPermissionEntity;
import static software.wings.graphql.datafetcher.userGroup.UserGroupPermissionsController.populateUserGroupAppPermissionEntity;
import static software.wings.graphql.datafetcher.userGroup.UserGroupPermissionsController.populateUserGroupPermissions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.User;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.notification.SlackNotificationConfiguration;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.graphql.schema.mutation.userGroup.input.QLCreateUserGroupInput;
import software.wings.graphql.schema.mutation.userGroup.payload.QLCreateUserGroupPayload;
import software.wings.graphql.schema.mutation.userGroup.payload.QLUpdateUserGroupPayload;
import software.wings.graphql.schema.type.permissions.QLGroupPermissions;
import software.wings.graphql.schema.type.usergroup.QLNotificationSettings;
import software.wings.graphql.schema.type.usergroup.QLSlackNotificationSetting;
import software.wings.graphql.schema.type.usergroup.QLUserGroup;
import software.wings.graphql.schema.type.usergroup.QLUserGroup.QLUserGroupBuilder;
import software.wings.service.intfc.UserService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class UserGroupController {
  @Inject private UserGroupPermissionValidator userGroupPermissionValidator;
  @Inject private UserService userService;
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

  public QLCreateUserGroupPayload populateCreateUserGroupPayload(UserGroup userGroup, String requestId) {
    QLGroupPermissions permissions = populateUserGroupPermissions(userGroup);
    QLNotificationSettings notificationSettings = populateNotificationSettings(userGroup);
    QLUserGroup userGroupOutput = QLUserGroup.builder()
                                      .name(userGroup.getName())
                                      .id(userGroup.getUuid())
                                      .description(userGroup.getDescription())
                                      .permissions(permissions)
                                      .isSSOLinked(userGroup.isSsoLinked())
                                      .importedByScim(userGroup.isImportedByScim())
                                      .notificationSettings(notificationSettings)
                                      .build();
    return QLCreateUserGroupPayload.builder().requestId(requestId).userGroup(userGroupOutput).build();
  }

  public QLUpdateUserGroupPayload populateUpdateUserGroupPayload(UserGroup userGroup, String requestId) {
    QLGroupPermissions permissions = populateUserGroupPermissions(userGroup);
    QLNotificationSettings notificationSettings = populateNotificationSettings(userGroup);
    QLUserGroup userGroupOutput = QLUserGroup.builder()
                                      .name(userGroup.getName())
                                      .id(userGroup.getUuid())
                                      .description(userGroup.getDescription())
                                      .permissions(permissions)
                                      .isSSOLinked(userGroup.isSsoLinked())
                                      .importedByScim(userGroup.isImportedByScim())
                                      .notificationSettings(notificationSettings)
                                      .build();
    return QLUpdateUserGroupPayload.builder().requestId(requestId).userGroup(userGroupOutput).build();
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

  public NotificationSettings populateNotificationSettingsEntity(QLNotificationSettings notificationSetting) {
    if (notificationSetting == null) {
      return new NotificationSettings(false, false, Collections.emptyList(), null, "");
    }
    QLSlackNotificationSetting slackNotificationInput = notificationSetting.getSlackNotificationSetting();
    SlackNotificationSetting slackConfig = null;
    if (slackNotificationInput != null) {
      slackConfig = new SlackNotificationSetting(
          slackNotificationInput.getSlackChannelName(), slackNotificationInput.getSlackWebhookURL());
    } else {
      slackConfig = new SlackNotificationSetting("", "");
    }
    boolean sendEmailToNewMembers = false;
    boolean sendNotitficationToNewMembers = false;
    if (notificationSetting.getSendNotificationToMembers() != null) {
      sendNotitficationToNewMembers = notificationSetting.getSendNotificationToMembers();
    }

    if (notificationSetting.getSendMailToNewMembers() != null) {
      sendEmailToNewMembers = notificationSetting.getSendMailToNewMembers();
    }

    return new NotificationSettings(sendNotitficationToNewMembers, sendEmailToNewMembers,
        notificationSetting.getGroupEmailAddresses() == null ? Collections.emptyList()
                                                             : notificationSetting.getGroupEmailAddresses(),
        slackConfig, notificationSetting.getPagerDutyIntegrationKey());
  }

  public void validateTheUserIds(List<String> userIds) {
    if (isEmpty(userIds)) {
      return;
    }
    List<String> idsInput = new ArrayList<>(userIds);
    PageRequest<User> req = aPageRequest().addFieldsIncluded("_id").addFilter("_id", IN, userIds.toArray()).build();
    PageResponse<User> res = userService.list(req, false);
    // This Ids are wrong
    List<String> idsPresent = res.stream().map(User::getUuid).collect(Collectors.toList());
    idsInput.removeAll(idsPresent);
    if (isNotEmpty(idsInput)) {
      throw new InvalidRequestException(
          String.format("Invalid id/s %s provided in the request", String.join(", ", idsInput)));
    }
  }

  public List<User> populateUserGroupMembersField(List<String> userGroupIds) {
    if (userGroupIds == null) {
      return null;
    }
    return userGroupIds.stream().map(id -> userService.get(id)).collect(Collectors.toList());
  }

  public UserGroup populateUserGroupEntity(QLCreateUserGroupInput userGroupInput) {
    userGroupPermissionValidator.validatePermission(userGroupInput.getPermissions());
    AccountPermissions accountPermissions = populateUserGroupAccountPermissionEntity(userGroupInput.getPermissions());
    Set<AppPermission> appPermissions = populateUserGroupAppPermissionEntity(userGroupInput.getPermissions());
    validateTheUserIds(userGroupInput.getUserIds());
    return UserGroup.builder()
        .name(userGroupInput.getName())
        .description(userGroupInput.getDescription())
        .notificationSettings(populateNotificationSettingsEntity(userGroupInput.getNotificationSettings()))
        .memberIds(userGroupInput.getUserIds())
        .members(populateUserGroupMembersField(userGroupInput.getUserIds()))
        .appPermissions(appPermissions)
        .accountPermissions(accountPermissions)
        .build();
  }
}
