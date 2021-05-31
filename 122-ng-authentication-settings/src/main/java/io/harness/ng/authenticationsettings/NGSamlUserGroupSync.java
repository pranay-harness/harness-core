package io.harness.ng.authenticationsettings;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.entities.UserGroup;
import io.harness.ng.core.user.service.NgUserService;

import software.wings.security.authentication.SamlUserAuthorization;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class NGSamlUserGroupSync {
  @Inject private UserGroupService userGroupService;
  @Inject private NgUserService ngUserService;

  public void syncUserGroup(
      final String accountIdentifier, final String ssoId, final String email, final List<String> userGroups) {
    SamlUserAuthorization samlUserAuthorization =
        SamlUserAuthorization.builder().email(email).userGroups(userGroups).build();
    log.info("Syncing saml user groups for user: {}", samlUserAuthorization.getEmail());

    // audting and handling notification service for that user to send an email is left

    List<UserGroup> userGroupsToSync = userGroupService.getUserGroupsBySsoId(accountIdentifier, ssoId);
    updateUserGroups(userGroupsToSync, samlUserAuthorization, accountIdentifier);
  }

  private void updateUserGroups(
      List<UserGroup> userGroupsToSync, SamlUserAuthorization samlUserAuthorization, String accountIdentifier) {
    UserInfo userInfo = ngUserService.getUserFromEmail(samlUserAuthorization.getEmail()).get();

    final List<String> newUserGroups =
        samlUserAuthorization.getUserGroups() != null ? samlUserAuthorization.getUserGroups() : new ArrayList<>();
    log.info("SAML authorisation user groups for user: {} are: {}", samlUserAuthorization.getEmail(),
        newUserGroups.toString());

    List<UserGroup> userAddedToGroups = new ArrayList<>();

    Scope scope =
        Scope.builder().accountIdentifier(accountIdentifier).orgIdentifier(null).projectIdentifier(null).build();
    userGroupsToSync.forEach(userGroup -> {
      if (userGroupService.checkMember(accountIdentifier, null, null, userGroup.getIdentifier(), userInfo.getUuid())
          && !newUserGroups.contains(userGroup.getSsoGroupId())) {
        log.info("Removing user: {} from user group: {} in account: {}", samlUserAuthorization.getEmail(),
            userGroup.getName(), userGroup.getAccountIdentifier());
        userGroupService.removeMember(scope, userGroup.getIdentifier(), userInfo.getUuid());
      } else if (!userGroupService.checkMember(
                     accountIdentifier, null, null, userGroup.getIdentifier(), userInfo.getUuid())
          && newUserGroups.contains(userGroup.getSsoGroupId())) {
        userAddedToGroups.add(userGroup);
      }
    });

    log.info("Adding user {} to groups {} in saml authorization.", samlUserAuthorization.getEmail(),
        userAddedToGroups.toString());

    userGroupService.addUserToUserGroups(accountIdentifier, userInfo, userAddedToGroups);
  }
}