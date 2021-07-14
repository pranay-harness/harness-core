package software.wings.security.saml;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.security.authentication.SamlUserAuthorization;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class SamlUserGroupSync {
  @Inject private UserGroupService userGroupService;
  @Inject private UserService userService;

  public void syncUserGroup(
      final SamlUserAuthorization samlUserAuthorization, final String accountId, final String ssoId) {
    List<UserGroup> userGroupsToSync = userGroupService.getUserGroupsBySsoId(accountId, ssoId);
    log.info("Syncing SAML user groups userGroups:{} accountId:{} ssoId:{}", userGroupsToSync, accountId, ssoId);
    updateUserGroups(userGroupsToSync, samlUserAuthorization, accountId);
  }

  private void updateUserGroups(
      List<UserGroup> userGroupsToSync, SamlUserAuthorization samlUserAuthorization, String accountId) {
    User user = userService.getUserByEmail(samlUserAuthorization.getEmail());
    if (user == null) {
      throw new WingsException(ErrorCode.USER_DOES_NOT_EXIST, "Cannot find User in DB");
    }
    List<String> newUserGroups = samlUserAuthorization.getUserGroups();
    log.info("SAML authorisation user groups for user: {} are: {}", user.getUuid(), newUserGroups.toString());

    List<UserGroup> userAddedToGroups = new ArrayList<>();

    userGroupsToSync.forEach(userGroup -> {
      if (userGroup.hasMember(user) && !newUserGroups.contains(userGroup.getSsoGroupId())) {
        log.info("Removing user: {} from user group: {} in account: {}", samlUserAuthorization.getEmail(),
            userGroup.getName(), userGroup.getAccountId());
        userGroupService.removeMembers(userGroup, Collections.singletonList(user), false, true);
      } else if (!userGroup.hasMember(user) && newUserGroups.contains(userGroup.getSsoGroupId())) {
        userAddedToGroups.add(userGroup);
      }
    });

    log.info("Adding user {} to groups {} in saml authorization.", samlUserAuthorization.getEmail(),
        userAddedToGroups.toString());
    userService.addUserToUserGroups(accountId, user, userAddedToGroups, true, true);
  }
}
