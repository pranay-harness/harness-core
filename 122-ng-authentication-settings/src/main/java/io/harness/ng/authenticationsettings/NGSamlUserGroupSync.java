package io.harness.ng.authenticationsettings;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.user.UserMembershipUpdateSource;
import io.harness.ng.core.user.entities.UserGroup;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.remote.dto.UserMetadataDTO;
import io.harness.ng.core.user.service.NgUserService;

import software.wings.security.authentication.SamlUserAuthorization;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

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
      if (!newUserGroups.contains(userGroup.getSsoGroupId())) {
        if (userGroupService.checkMember(scope.getAccountIdentifier(), scope.getOrgIdentifier(),
                scope.getProjectIdentifier(), userGroup.getIdentifier(), user.getUuid())) {
          log.info("[NGSamlUserGroupSync] Removing user: {} from user group: {} in account: {}", user.getUuid(),
              userGroup.getName(), userGroup.getAccountIdentifier());
          userGroupService.removeMember(scope, userGroup.getIdentifier(), user.getUuid());
        }

        if (!checkUserIsOtherGroupMember(scope, user.getUuid())) {
          log.info("[NGSamlUserGroupSync] Removing user: {} from scope account: [{}], org:[{}], project:[{}]",
              user.getUuid(), scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());
          ngUserService.removeUserFromScope(user.getUuid(), scope, UserMembershipUpdateSource.SYSTEM);
        }
      } else if (!userGroupService.checkMember(scope.getAccountIdentifier(), scope.getOrgIdentifier(),
                     scope.getProjectIdentifier(), userGroup.getIdentifier(), user.getUuid())
          && newUserGroups.contains(userGroup.getSsoGroupId())) {
        log.info(
            "[NGSamlUserGroupSync] Adding user {} to scope account: [{}], org:[{}], project:[{}] and  groups {} in saml authorization.",
            samlUserAuthorization.getEmail(), scope.getAccountIdentifier(), scope.getOrgIdentifier(),
            scope.getProjectIdentifier(), userAddedToGroups.toString());
        Optional<UserMembership> currentScopeUserMembership = ngUserService.getUserMembership(user.getUuid(), scope);
        if (!currentScopeUserMembership.isPresent()) {
          ngUserService.addUserToScope(user.getUuid(), scope, null,
              Collections.singletonList(userGroup.getIdentifier()), UserMembershipUpdateSource.SYSTEM);
        } else {
          userAddedToGroups.add(userGroup);
        }
      } else {
        log.info("[NGSamlUserGroupSync]: Should not come here User: {} Scope: {} UserGroup: {}", user.getUuid(), scope,
            userGroup.getName());
      }
    });
    userGroupService.addUserToUserGroups(accountIdentifier, user.getUuid(), userAddedToGroups);
  }

  @VisibleForTesting
  public boolean checkUserIsOtherGroupMember(Scope scope, String userId) {
    List<UserGroup> userGroupsAtScope =
        getUserGroupsAtScope(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());
    if (isNotEmpty(userGroupsAtScope)) {
      for (UserGroup userGroup : userGroupsAtScope) {
        if (userGroup.getUsers().contains(userId)) {
          return true;
        }
      }
      return false;
    }
    return false;
  }

  @VisibleForTesting
  public List<UserGroup> getUserGroupsAtScope(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Page<UserGroup> pagedUserGroups = null;
    List<UserGroup> userGroups = new ArrayList<>();
    do {
      pagedUserGroups = userGroupService.list(accountIdentifier, orgIdentifier, projectIdentifier, null,
          getPageRequest(PageRequest.builder()
                             .pageIndex(pagedUserGroups == null ? 0 : pagedUserGroups.getNumber() + 1)
                             .pageSize(40)
                             .build()));
      if (pagedUserGroups != null) {
        userGroups.addAll(pagedUserGroups.stream().collect(Collectors.toList()));
      }
    } while (pagedUserGroups != null && pagedUserGroups.hasNext());
    return userGroups;
  }
}