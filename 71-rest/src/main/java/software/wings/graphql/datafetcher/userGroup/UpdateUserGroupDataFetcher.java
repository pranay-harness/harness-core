package software.wings.graphql.datafetcher.userGroup;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.userGroup.input.QLUpdateUserGroupInput;
import software.wings.graphql.schema.mutation.userGroup.payload.QLUpdateUserGroupPayload;
import software.wings.graphql.schema.type.usergroup.QLSSOSettingInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UserGroupService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class UpdateUserGroupDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateUserGroupInput, QLUpdateUserGroupPayload> {
  @Inject UserGroupService userGroupService;
  @Inject UserGroupPermissionValidator userGroupPermissionValidator;
  @Inject UserGroupController userGroupController;
  @Inject SSOSettingService ssoSettingService;
  @Inject UserGroupPermissionsController userGroupPermissionsController;

  @Inject
  public UpdateUserGroupDataFetcher(UserGroupService userGroupService,
      UserGroupPermissionValidator userGroupPermissionValidator, UserGroupController userGroupController,
      SSOSettingService ssoSettingService) {
    super(QLUpdateUserGroupInput.class, QLUpdateUserGroupPayload.class);
    this.userGroupService = userGroupService;
    this.userGroupPermissionValidator = userGroupPermissionValidator;
    this.userGroupController = userGroupController;
    this.ssoSettingService = ssoSettingService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT)
  protected QLUpdateUserGroupPayload mutateAndFetch(QLUpdateUserGroupInput parameter, MutationContext mutationContext) {
    String userGroupId = parameter.getUserGroupId();
    UserGroup existingUserGroup = userGroupService.get(mutationContext.getAccountId(), userGroupId);

    if (existingUserGroup == null) {
      throw new InvalidRequestException(String.format("No user group exists with the id %s", userGroupId));
    }
    QLUpdateUserGroupInput userGroupInput = parameter;
    boolean overViewUpdated = false;
    // Update name
    if (userGroupInput.getName().isPresent()) {
      String name = userGroupInput.getName().getValue().map(StringUtils::strip).orElse(null);
      if (isBlank(name)) {
        throw new InvalidRequestException("The name supplied in the update user group request is blank");
      }
      List<UserGroup> userGroups = userGroupService.listByName(mutationContext.getAccountId(), Arrays.asList(name));
      List<String> userGroupIds = Collections.emptyList();
      if (userGroups != null) {
        userGroupIds = userGroups.stream().map(UserGroup::getUuid).collect(Collectors.toList());
      }
      // We will throw a error whenever someone update the name, with which a usergroup already exists
      if (!isEmpty(userGroups) && !userGroupIds.contains(userGroupId)) {
        throw new InvalidRequestException(String.format("A user group already exists with the name %s", name));
      }
      existingUserGroup.setName(name);
      overViewUpdated = true;
    }
    // Update Description
    if (userGroupInput.getDescription().isPresent()) {
      existingUserGroup.setDescription(userGroupInput.getDescription().getValue().orElse(null));
      overViewUpdated = true;
    }

    if (overViewUpdated) {
      userGroupService.updateOverview(existingUserGroup);
    }

    // Update Permissions
    if (userGroupInput.getPermissions().isPresent()) {
      userGroupPermissionValidator.validatePermission(
          userGroupInput.getPermissions().getValue().orElse(null), mutationContext.getAccountId());
      existingUserGroup.setAccountPermissions(userGroupPermissionsController.populateUserGroupAccountPermissionEntity(
          userGroupInput.getPermissions().getValue().orElse(null)));
      existingUserGroup.setAppPermissions(userGroupPermissionsController.populateUserGroupAppPermissionEntity(
          userGroupInput.getPermissions().getValue().orElse(null)));
      userGroupService.updatePermissions(existingUserGroup);
    }

    // Update the Users
    if (userGroupInput.getUserIds().isPresent()) {
      List<String> userIds = userGroupInput.getUserIds().getValue().orElse(null);
      userGroupController.validateTheUserIds(userIds, mutationContext.getAccountId());
      existingUserGroup.setMemberIds(userIds);
      existingUserGroup.setMembers(userGroupController.populateUserGroupMembersField(userIds));
      boolean sendNotification = false;
      if (existingUserGroup.getNotificationSettings() != null) {
        sendNotification = existingUserGroup.getNotificationSettings().isSendMailToNewMembers();
      }
      userGroupService.updateMembers(existingUserGroup, sendNotification);
    }

    // Update SSOSettings
    if (userGroupInput.getSsoSetting().isPresent()) {
      QLSSOSettingInput ssoProvider = userGroupInput.getSsoSetting().getValue().orElse(null);
      UserGroupSSOSettings ssoSettings = userGroupController.populateUserGroupSSOSettings(ssoProvider);
      if (ssoProvider.getLdapSettings() == null && ssoProvider.getSamlSettings() == null) {
        userGroupService.unlinkSsoGroup(mutationContext.getAccountId(), userGroupId, false);
      } else {
        userGroupService.linkToSsoGroup(mutationContext.getAccountId(), userGroupId, ssoSettings.getSsoType(),
            ssoSettings.getLinkedSSOId(), ssoSettings.getSsoGroupId(), ssoSettings.getSsoGroupName());
      }
    }

    // Update NotificationSettings
    if (userGroupInput.getNotificationSettings().isPresent()) {
      existingUserGroup.setNotificationSettings(userGroupController.populateNotificationSettingsEntity(
          userGroupInput.getNotificationSettings().getValue().orElse(null)));
      userGroupService.updateNotificationSettings(
          mutationContext.getAccountId(), userGroupId, existingUserGroup.getNotificationSettings());
    }

    UserGroup updatedUserGroup = userGroupService.get(mutationContext.getAccountId(), userGroupId);
    return userGroupController.populateUpdateUserGroupPayload(updatedUserGroup, parameter.getClientMutationId());
  }
}
