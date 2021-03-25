package io.harness.ng.core.user.services.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.remote.client.NGRestUtils.getResponse;


import static java.util.stream.Collectors.toList;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageResponse;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.invites.dto.UserSearchDTO;
import io.harness.ng.core.invites.entities.UserMembership;
import io.harness.ng.core.invites.entities.UserMembership.Scope;
import io.harness.ng.core.invites.remote.UserSearchMapper;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.remote.client.RestClientUtils;
import io.harness.repositories.invites.spring.UserMembershipRepository;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@Slf4j
@OwnedBy(PL)
public class NgUserServiceImpl implements NgUserService {
  private final UserClient userClient;
  private final UserMembershipRepository userMembershipRepository;
  private final AccessControlAdminClient accessControlAdminClient;

  @Inject
  public NgUserServiceImpl(UserClient userClient, UserMembershipRepository userMembershipRepository,
      AccessControlAdminClient accessControlAdminClient) {
    this.userClient = userClient;
    this.userMembershipRepository = userMembershipRepository;
    this.accessControlAdminClient = accessControlAdminClient;
  }

  @Override
  public Page<UserInfo> list(String accountIdentifier, String searchString, Pageable pageable) {
    //  @Ankush remove the offset and limit from the following statement because it is redundant pagination
    PageResponse<UserInfo> userPageResponse = RestClientUtils.getResponse(userClient.list(
        accountIdentifier, String.valueOf(pageable.getOffset()), String.valueOf(pageable.getPageSize()), searchString));
    List<UserInfo> users = userPageResponse.getResponse();
    return new PageImpl<>(users, pageable, users.size());
  }

  @Override
  public io.harness.ng.beans.PageResponse<UserSearchDTO> listUsersAtScope(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, PageRequest pageRequest) {
    Pageable pageable = PageUtils.getPageRequest(pageRequest);
    Criteria criteria =
        Criteria.where(UserMembership.UserMembershipKeys.scopes + "." + Scope.ScopeKeys.accountIdentifier)
            .is(accountIdentifier)
            .and(UserMembership.UserMembershipKeys.scopes + "." + Scope.ScopeKeys.orgIdentifier)
            .is(orgIdentifier)
            .and(UserMembership.UserMembershipKeys.scopes + "." + Scope.ScopeKeys.projectIdentifier)
            .is(projectIdentifier);
    Page<UserMembership> userMembershipPage = userMembershipRepository.findAll(criteria, pageable);
    List<String> userIds = userMembershipPage.getContent().stream().map(UserMembership::getUserId).collect(toList());
    List<UserInfo> users = this.getUsersByIds(userIds);
    List<UserSearchDTO> userSearchDTOs = users.stream().map(UserSearchMapper::writeDTO).collect(toList());
    return PageUtils.getNGPageResponse(userMembershipPage, userSearchDTOs);
  }

  @Override
  public List<UserMembership> listUserMemberships(Criteria criteria) {
    return userMembershipRepository.findAll(criteria);
  }

  // isSignedUp flag: Users in current gen exists even if they are in invited state and not signed up yet.
  public Optional<UserInfo> getUserFromEmail(String email) {
    List<UserInfo> users = getUsersFromEmail(Collections.singletonList(email));
    if (isEmpty(users)) {
      return Optional.empty();
    }
    return Optional.of(users.get(0));
  }

  @Override
  public List<UserInfo> getUsersFromEmail(List<String> emailIds) {
    return RestClientUtils.getResponse(userClient.getUsersFromEmail(emailIds));
  }

  @Override
  public List<String> getUsersHavingRole(Scope scope, String roleIdentifier) {
    List<RoleAssignmentResponseDTO> roleAssignmentResponses =
        getResponse(accessControlAdminClient.getFilteredRoleAssignments(scope.getAccountIdentifier(),
                        scope.getOrgIdentifier(), scope.getProjectIdentifier(), 0, 1000,
                        RoleAssignmentFilterDTO.builder().roleFilter(Collections.singleton(roleIdentifier)).build()))
            .getContent();
    return roleAssignmentResponses.stream()
        .filter(roleAssignmentResponse
            -> roleAssignmentResponse.getRoleAssignment().getPrincipal().getType().equals(PrincipalType.USER))
        .map(roleAssignmentResponse -> roleAssignmentResponse.getRoleAssignment().getPrincipal().getIdentifier())
        .collect(toList());
  }

  @Override
  public Optional<UserMembership> getUserMembership(String userId) {
    return userMembershipRepository.findDistinctByUserId(userId);
  }

  @Override
  public void addUserToScope(UserInfo user, Scope scope) {
    addUserToScope(user.getUuid(), user.getEmail(), scope);
  }

  @Override
  public void addUserToScope(String userId, String emailId, Scope scope) {
    Optional<UserMembership> userMembershipOptional = userMembershipRepository.findDistinctByUserId(userId);

    UserMembership userMembership = userMembershipOptional.orElseGet(
        () -> UserMembership.builder().userId(userId).emailId(emailId).scopes(new ArrayList<>()).build());
    if (!userMembership.getScopes().contains(scope)) {
      userMembership.getScopes().add(scope);
    }
    userMembershipRepository.save(userMembership);
    postCreation(userId, scope.getAccountIdentifier());
  }

  @Override
  public void addUserToScope(String userId, Scope scope) {
    Optional<UserInfo> userOptional = getUserById(userId);
    if (!userOptional.isPresent()) {
      return;
    }
    UserInfo user = userOptional.get();
    addUserToScope(user.getUuid(), user.getEmail(), scope);
  }

  private void postCreation(String userId, String accountIdentifier) {
    RestClientUtils.getResponse(userClient.addUserToAccount(userId, accountIdentifier));
  }

  @Override
  public List<UserInfo> getUsersByIds(List<String> userIds) {
    return RestClientUtils.getResponse(userClient.getUsersByIds(userIds));
  }

  @Override
  public Optional<UserInfo> getUserById(String userId) {
    List<UserInfo> users = getUsersByIds(Collections.singletonList(userId));
    return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
  }

  @Override
  public boolean isUserInAccount(String accountId, String userId) {
    return Boolean.TRUE.equals(RestClientUtils.getResponse(userClient.isUserInAccount(accountId, userId)));
  }

  @Override
  public void removeUserFromScope(
      String userId, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Optional<UserMembership> userMembershipOptional = getUserMembership(userId);
    if (!userMembershipOptional.isPresent()) {
      return;
    }
    UserMembership userMembership = userMembershipOptional.get();
    Scope scope = Scope.builder()
                      .accountIdentifier(accountIdentifier)
                      .orgIdentifier(orgIdentifier)
                      .projectIdentifier(projectIdentifier)
                      .build();
    List<Scope> scopes = userMembership.getScopes();
    if (!scopes.contains(scope)) {
      return;
    }
    scopes.remove(scope);
    boolean isUserRemovedFromAccount =
        scopes.stream().noneMatch(scope1 -> scope1.getAccountIdentifier().equals(accountIdentifier));
    if (isUserRemovedFromAccount) {
      RestClientUtils.getResponse(userClient.safeDeleteUser(userId, accountIdentifier));
    }
  }

  @Override
  public boolean removeUserMembership(String userId) {
    return userMembershipRepository.deleteUserMembershipByUserId(userId) > 0;
  }
}
