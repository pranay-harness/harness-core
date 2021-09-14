/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.authenticationsettings;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.user.entities.UserGroup;
import io.harness.ng.core.user.remote.dto.UserMetadataDTO;
import io.harness.ng.core.user.service.NgUserService;

import software.wings.security.authentication.SamlUserAuthorization;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    // audting and handling notification service for that user to send an email is left
    List<UserGroup> userGroupsToSync = userGroupService.getUserGroupsBySsoId(ssoId);
    log.info("[NGSamlUserGroupSync] Syncing saml user groups for user: {}  userGroups: {}",
        samlUserAuthorization.getEmail(), userGroupsToSync);
    updateUserGroups(userGroupsToSync, samlUserAuthorization, accountIdentifier);
  }

  private void updateUserGroups(
      List<UserGroup> userGroupsToSync, SamlUserAuthorization samlUserAuthorization, String accountIdentifier) {
    Optional<UserMetadataDTO> userOpt = ngUserService.getUserByEmail(samlUserAuthorization.getEmail(), false);
    if (!userOpt.isPresent()) {
      return;
    }
    UserMetadataDTO user = userOpt.get();
    final List<String> newUserGroups =
        samlUserAuthorization.getUserGroups() != null ? samlUserAuthorization.getUserGroups() : new ArrayList<>();
    log.info("[NGSamlUserGroupSync] SAML authorisation user groups for user: {} are: {}",
        samlUserAuthorization.getEmail(), newUserGroups.toString());

    List<UserGroup> userAddedToGroups = new ArrayList<>();

    userGroupsToSync.forEach(userGroup -> {
      Scope scope =
          Scope.of(userGroup.getAccountIdentifier(), userGroup.getOrgIdentifier(), userGroup.getProjectIdentifier());
      if (userGroupService.checkMember(scope.getAccountIdentifier(), scope.getOrgIdentifier(),
              scope.getProjectIdentifier(), userGroup.getIdentifier(), user.getUuid())
          && !newUserGroups.contains(userGroup.getSsoGroupId())) {
        log.info("[NGSamlUserGroupSync] Removing user: {} from user group: {} in account: {}", user.getUuid(),
            userGroup.getName(), userGroup.getAccountIdentifier());
        userGroupService.removeMember(scope, userGroup.getIdentifier(), user.getUuid());
      } else if (!userGroupService.checkMember(scope.getAccountIdentifier(), scope.getOrgIdentifier(),
                     scope.getProjectIdentifier(), userGroup.getIdentifier(), user.getUuid())
          && newUserGroups.contains(userGroup.getSsoGroupId())) {
        userAddedToGroups.add(userGroup);
      } else {
        log.info("[NGSamlUserGroupSync]: Should not come here User: {} Scope: {} UserGroup: {}", user.getUuid(), scope,
            userGroup.getName());
      }
    });

    log.info("[NGSamlUserGroupSync] Adding user {} to groups {} in saml authorization.",
        samlUserAuthorization.getEmail(), userAddedToGroups.toString());

    userGroupService.addUserToUserGroups(accountIdentifier, user.getUuid(), userAddedToGroups);
  }
}
