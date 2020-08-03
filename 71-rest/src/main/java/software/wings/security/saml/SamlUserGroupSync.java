package software.wings.security.saml;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.security.authentication.SamlUserAuthorization;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class SamlUserGroupSync {
  @Inject private UserGroupService userGroupService;
  @Inject private UserService userService;

  public void syncUserGroup(
      final SamlUserAuthorization samlUserAuthorization, final String accountId, final String ssoId) {
    logger.info("Syncing saml groups for user: {}", samlUserAuthorization.getEmail());

    List<UserGroup> userGroupsToSync = userGroupService.getUserGroupsBySsoId(accountId, ssoId);
    updateUserGroups(userGroupsToSync, samlUserAuthorization, accountId);
  }

  private void updateUserGroups(
      List<UserGroup> userGroupsToSync, SamlUserAuthorization samlUserAuthorization, String accountId) {
    User user = userService.getUserByEmail(samlUserAuthorization.getEmail());

    List<String> newUserGroups = samlUserAuthorization.getUserGroups();
    logger.info("Adding user {} to groups {} in saml authorization.", samlUserAuthorization.getEmail(),
        newUserGroups.toString());

    List<UserGroup> userAddedToGroups = new ArrayList<>();

    userGroupsToSync.forEach(userGroup -> {
      if (userGroup.getMembers().contains(user) && !newUserGroups.contains(userGroup.getSsoGroupId())) {
        logger.info("Removing user: {} from user group: {} in account: {}", samlUserAuthorization.getEmail(),
            userGroup.getName(), userGroup.getAccountId());
        userGroupService.removeMembers(userGroup, Collections.singletonList(user), false, true);
      } else if (!userGroup.getMembers().contains(user) && newUserGroups.contains(userGroup.getSsoGroupId())) {
        userAddedToGroups.add(userGroup);
      }
    });

    userService.addUserToUserGroups(accountId, user, userAddedToGroups, true, true);
  }
}
