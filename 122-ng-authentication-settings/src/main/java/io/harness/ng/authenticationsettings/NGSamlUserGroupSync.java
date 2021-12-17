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
import io.harness.ng.core.user.entities.UserMembership.UserMembershipKeys;
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
import org.springframework.data.mongodb.core.query.Criteria;

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
    removeUsersFromScopesPostSync(userGroupsToSync, user.getUuid());
  }

  @VisibleForTesting
  void removeUsersFromScopesPostSync(List<UserGroup> userGroupsToSync, String userId) {
    log.info("[NGSamlUserGroupSync] Checking removal of user: {} from all diff scopes post sync", userId);

    userGroupsToSync.forEach(userGroup -> {
      Criteria criteria = Criteria.where(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY)
              .is(userGroup.getAccountIdentifier())
              .and(UserMembershipKeys.ORG_IDENTIFIER_KEY)
              .is(userGroup.getOrgIdentifier())
              .and(UserMembershipKeys.PROJECT_IDENTIFIER_KEY)
              .is(userGroup.getProjectIdentifier());
      if (!checkUserIsAGroupMember(criteria, userId)) {
        // remove user from project
        criteria = Criteria.where(UserMembershipKeys.userId)
                .is(userId)
                .and(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY)
                .is(userGroup.getAccountIdentifier())
                .and(UserMembershipKeys.ORG_IDENTIFIER_KEY)
                .is(userGroup.getOrgIdentifier())
                .and(UserMembershipKeys.PROJECT_IDENTIFIER_KEY)
                .is(userGroup.getProjectIdentifier());

        log.info(
                "[NGSamlUserGroupSync] Removing user: {} from project scope {} as it is not part of project scope post sync",
                userId, userGroup.getProjectIdentifier());
        ngUserService.removeUserWithCriteria(userId, UserMembershipUpdateSource.SYSTEM, criteria);
      }

      criteria = Criteria.where(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY)
              .is(userGroup.getAccountIdentifier())
              .and(UserMembershipKeys.ORG_IDENTIFIER_KEY)
              .is(userGroup.getOrgIdentifier());

      if (!checkUserIsAGroupMember(criteria, userId)) {
        // remove user from org
        criteria = Criteria.where(UserMembershipKeys.userId)
                .is(userId)
                .and(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY)
                .is(userGroup.getAccountIdentifier())
                .and(UserMembershipKeys.ORG_IDENTIFIER_KEY)
                .is(userGroup.getOrgIdentifier());

        log.info(
                "[NGSamlUserGroupSync] Removing user: {} from org scope {} as it is not part of org scope post sync",
                userId, userGroup.getOrgIdentifier());
        ngUserService.removeUserWithCriteria(userId, UserMembershipUpdateSource.SYSTEM, criteria);
      }

      criteria = Criteria.where(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY)
              .is(userGroup.getAccountIdentifier());

      if (!checkUserIsAGroupMember(criteria, userId)) {
        // remove user from account
        criteria = Criteria.where(UserMembershipKeys.userId)
                .is(userId)
                .and(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY)
                .is(userGroup.getAccountIdentifier());

        log.info(
                "[NGSamlUserGroupSync] Removing user: {} from account scope {} as it is not part of account scope post sync",
                userId, userGroup.getAccountIdentifier());
        ngUserService.removeUserWithCriteria(userId, UserMembershipUpdateSource.SYSTEM, criteria);
      }
    });
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

  @VisibleForTesting
  public boolean checkUserIsAGroupMember(Criteria criteria, String userId) {
    List<UserGroup> userGroupsForCriteria = this.getUserGroupsForCriteria(criteria);
    if (isNotEmpty(userGroupsForCriteria)) {
      for (UserGroup userGroup : userGroupsForCriteria) {
        if (userGroup.getUsers().contains(userId)) {
          return true;
        }
      }
    }
    return false;
  }

  public List<UserGroup> getUserGroupsForCriteria(Criteria criteria) {
    return userGroupService.list(criteria);
  }
}