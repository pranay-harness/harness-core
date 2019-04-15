package software.wings.security.saml;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInvite.UserInviteBuilder;
import software.wings.beans.security.UserGroup;
import software.wings.security.authentication.SamlUserAuthorization;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;

import java.util.ArrayList;
import java.util.Arrays;
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
    List<UserGroup> userAddedToGroups = new ArrayList<>();
    User user = userService.getUserByEmail(samlUserAuthorization.getEmail());

    List<String> newUserGroups = samlUserAuthorization.getUserGroups();

    logger.info("Adding user {} to groups {} in saml authorization.", samlUserAuthorization.getEmail(),
        newUserGroups.toString());

    userGroupsToSync.forEach(userGroup -> {
      if (userGroup.getMembers().contains(user) && !newUserGroups.contains(userGroup.getSsoGroupId())) {
        userGroupService.removeMembers(userGroup, Arrays.asList(user), false);
      } else if (!userGroup.getMembers().contains(user) && newUserGroups.contains(userGroup.getSsoGroupId())) {
        userAddedToGroups.add(userGroup);
      }
    });

    if (!userAddedToGroups.isEmpty()) {
      UserInvite userInvite = UserInviteBuilder.anUserInvite()
                                  .withAccountId(accountId)
                                  .withEmail(user.getEmail())
                                  .withName(user.getName())
                                  .withUserGroups(userAddedToGroups)
                                  .build();
      userService.inviteUser(userInvite);
    }
  }
}
