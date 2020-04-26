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
    userService.loadUserGroupsForUsers(Collections.singletonList(user), accountId);
    List<UserGroup> finalUserGroupsOfUser = user.getUserGroups();
    List<String> newUserGroups = samlUserAuthorization.getUserGroups();

    logger.info("Adding user {} to groups {} in saml authorization.", samlUserAuthorization.getEmail(),
        newUserGroups.toString());

    userGroupsToSync.forEach(userGroup -> {
      if (userGroup.getMembers().contains(user) && !newUserGroups.contains(userGroup.getSsoGroupId())) {
        finalUserGroupsOfUser.remove(userGroup);
      } else if (!userGroup.getMembers().contains(user) && newUserGroups.contains(userGroup.getSsoGroupId())) {
        finalUserGroupsOfUser.add(userGroup);
      }
    });

    UserInvite userInvite = UserInviteBuilder.anUserInvite()
                                .withAccountId(accountId)
                                .withEmail(user.getEmail())
                                .withName(user.getName())
                                .withUserGroups(finalUserGroupsOfUser)
                                .build();
    userService.inviteUserNew(userInvite);
  }
}
