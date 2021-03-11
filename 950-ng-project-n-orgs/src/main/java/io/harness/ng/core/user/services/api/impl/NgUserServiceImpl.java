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
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.invites.dto.UserSearchDTO;
import io.harness.ng.core.invites.entities.UserMembership;
import io.harness.ng.core.invites.entities.UserMembership.Scope;
import io.harness.ng.core.invites.entities.UserMembership.Scope.ScopeKeys;
import io.harness.ng.core.invites.entities.UserMembership.UserMembershipKeys;
import io.harness.ng.core.invites.remote.UserSearchMapper;
import io.harness.ng.core.user.User;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class NgUserServiceImpl implements NgUserService {
  UserClient userClient;
  UserMembershipRepository userMembershipRepository;
  AccessControlAdminClient accessControlAdminClient;

  @Override
  public Page<User> list(String accountIdentifier, String searchString, Pageable pageable) {
    List<User> users = RestClientUtils
                           .getResponse(userClient.list(accountIdentifier, String.valueOf(pageable.getOffset()),
                               String.valueOf(pageable.getPageSize()), searchString))
                           .getResponse();
    return new PageImpl<>(users, pageable, users.size());
  }

  @Override
  public PageResponse<UserSearchDTO> listUsersAtScope(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, PageRequest pageRequest) {
    Pageable pageable = PageUtils.getPageRequest(pageRequest);
    Criteria criteria = Criteria.where(UserMembershipKeys.scopes + "." + ScopeKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(UserMembershipKeys.scopes + "." + ScopeKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(UserMembershipKeys.scopes + "." + ScopeKeys.projectIdentifier)
                            .is(projectIdentifier);
    Page<UserMembership> userMembershipPage = userMembershipRepository.findAll(criteria, pageable);
    List<String> userIds = userMembershipPage.getContent().stream().map(UserMembership::getUserId).collect(toList());
    List<User> users = getUsersByIds(userIds);
    List<UserSearchDTO> userSearchDTOs = users.stream().map(UserSearchMapper::writeDTO).collect(toList());
    return PageUtils.getNGPageResponse(userMembershipPage, userSearchDTOs);
  }

  @Override
  public List<UserMembership> listUserMemberships(Criteria criteria) {
    return userMembershipRepository.findAll(criteria);
  }

  public Optional<User> getUserFromEmail(String emailId) {
    List<User> users = getUsersFromEmail(Collections.singletonList(emailId));
    if (isEmpty(users)) {
      return Optional.empty();
    }
    return Optional.of(users.get(0));
  }

  public List<User> getUsersFromEmail(List<String> emailIds) {
    return RestClientUtils.getResponse(userClient.getUsersFromEmail(emailIds));
  }

  @Override
  public Optional<UserMembership> getUserMembership(String userId) {
    return userMembershipRepository.findDistinctByUserId(userId);
  }

  @Override
  public void addUserToScope(User user, Scope scope) {
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
  }

  @Override
  public void removeScope(String userId, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Optional<UserMembership> userMembershipOpt = userMembershipRepository.findDistinctByUserId(userId);
    if (!userMembershipOpt.isPresent()) {
      return;
    }
    UserMembership userMembership = userMembershipOpt.get();
    userMembership.getScopes().remove(Scope.builder()
                                          .accountIdentifier(accountIdentifier)
                                          .orgIdentifier(orgIdentifier)
                                          .projectIdentifier(projectIdentifier)
                                          .build());
    userMembershipRepository.save(userMembership);
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
  public List<User> getUsersByIds(List<String> userIds) {
    return RestClientUtils.getResponse(userClient.getUsersByIds(userIds));
  }

  @Override
  public UserMembership addUserToScope(String userId, Scope scope) {
    Optional<UserMembership> userMembershipOptional = getUserMembership(userId);
    if (!userMembershipOptional.isPresent()) {
      return null;
    }
    UserMembership userMembership = userMembershipOptional.get();
    List<Scope> scopes = userMembership.getScopes();
    if (!scopes.contains(scope)) {
      userMembership.getScopes().add(scope);
      userMembershipRepository.save(userMembership);
    }
    return userMembership;
  }

  @Override
  public boolean isUserInAccount(String accountId, String userId) {
    return Boolean.TRUE.equals(RestClientUtils.getResponse(userClient.isUserInAccount(accountId, userId)));
  }

  @Override
  public boolean removeUserMembership(String userId) {
    Long numDeleted = userMembershipRepository.deleteUserMembershipByUserId(userId);
    return numDeleted > 0;
  }
}
