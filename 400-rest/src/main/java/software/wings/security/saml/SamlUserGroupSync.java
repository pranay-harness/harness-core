/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.security.saml;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

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
    log.info("Syncing saml user groups for user: {}", samlUserAuthorization.getEmail());

    List<UserGroup> userGroupsToSync = userGroupService.getUserGroupsBySsoId(accountId, ssoId);
    updateUserGroups(userGroupsToSync, samlUserAuthorization, accountId);
  }

  private void updateUserGroups(
      List<UserGroup> userGroupsToSync, SamlUserAuthorization samlUserAuthorization, String accountId) {
    User user = userService.getUserByEmail(samlUserAuthorization.getEmail());

    List<String> newUserGroups = samlUserAuthorization.getUserGroups();
    log.info("SAML authorisation user groups for user: {} are: {}", samlUserAuthorization.getEmail(),
        newUserGroups.toString());

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
