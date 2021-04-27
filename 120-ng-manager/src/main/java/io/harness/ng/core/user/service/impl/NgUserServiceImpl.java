package io.harness.ng.core.user.service.impl;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.remote.client.NGRestUtils.getResponse;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageResponse;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.events.UserMembershipAddEvent;
import io.harness.ng.core.events.UserMembershipRemoveEvent;
import io.harness.ng.core.remote.ProjectMapper;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserMembershipUpdateMechanism;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.entities.UserMembership.Scope;
import io.harness.ng.core.user.entities.UserMembership.Scope.ScopeKeys;
import io.harness.ng.core.user.entities.UserMembership.UserMembershipKeys;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.outbox.api.OutboxService;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.repositories.user.spring.UserMembershipRepository;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.PrincipalType;
import io.harness.user.remote.UserClient;
import io.harness.user.remote.UserSearchFilter;
import io.harness.utils.PageUtils;
import io.harness.utils.RetryUtils;
import io.harness.utils.ScopeUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@Singleton
@Slf4j
@OwnedBy(PL)
public class NgUserServiceImpl implements NgUserService {
  public static final String ACCOUNT_VIEWER = "_account_viewer";
  public static final String ORGANIZATION_VIEWER = "_organization_viewer";
  public static final String PROJECT_VIEWER = "_project_viewer";
  private static final String DEFAULT_RESOURCE_GROUP_IDENTIFIER = "_all_resources";
  private final UserClient userClient;
  private final UserMembershipRepository userMembershipRepository;
  private final AccessControlAdminClient accessControlAdminClient;
  private final TransactionTemplate transactionTemplate;
  private final OutboxService outboxService;

  private final RetryPolicy<Object> transactionRetryPolicy = RetryUtils.getRetryPolicy("[Retrying] attempt: {}",
      "[Failed] attempt: {}", ImmutableList.of(TransactionException.class), Duration.ofSeconds(1), 3, log);

  @Inject
  public NgUserServiceImpl(UserClient userClient, UserMembershipRepository userMembershipRepository,
      AccessControlAdminClient accessControlAdminClient,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate, OutboxService outboxService) {
    this.userClient = userClient;
    this.userMembershipRepository = userMembershipRepository;
    this.accessControlAdminClient = accessControlAdminClient;
    this.transactionTemplate = transactionTemplate;
    this.outboxService = outboxService;
  }

  @Override
  public Page<UserInfo> list(String accountIdentifier, String searchString, Pageable pageable) {
    PageResponse<UserInfo> userPageResponse = RestClientUtils.getResponse(userClient.list(
        accountIdentifier, String.valueOf(pageable.getOffset()), String.valueOf(pageable.getPageSize()), searchString));
    List<UserInfo> users = userPageResponse.getResponse();
    return new PageImpl<>(users, pageable, users.size());
  }

  @Override
  public List<String> listUsersAtScope(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Criteria criteria = Criteria.where(UserMembershipKeys.scopes + "." + ScopeKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(UserMembershipKeys.scopes + "." + ScopeKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(UserMembershipKeys.scopes + "." + ScopeKeys.projectIdentifier)
                            .is(projectIdentifier);
    Page<UserMembership> userMembershipPage = userMembershipRepository.findAll(criteria, Pageable.unpaged());
    return userMembershipPage.getContent().stream().map(UserMembership::getUserId).collect(toList());
  }

  @Override
  public List<UserMembership> listUserMemberships(Criteria criteria) {
    return userMembershipRepository.findAll(criteria);
  }

  public Optional<UserInfo> getUserFromEmail(String email) {
    return RestClientUtils.getResponse(userClient.getUserByEmailId(email));
  }

  @Override
  public List<UserInfo> getUsersFromEmail(List<String> emailIds, String accountId) {
    return RestClientUtils.getResponse(
        userClient.listUsers(UserSearchFilter.builder().emailIds(emailIds).build(), accountId));
  }

  @Override
  public List<String> getUsersHavingRole(Scope scope, String roleIdentifier) {
    List<RoleAssignmentResponseDTO> roleAssignmentResponses =
        getResponse(accessControlAdminClient.getFilteredRoleAssignments(scope.getAccountIdentifier(),
                        scope.getOrgIdentifier(), scope.getProjectIdentifier(), 0, 1000,
                        RoleAssignmentFilterDTO.builder().roleFilter(Collections.singleton(roleIdentifier)).build()))
            .getContent();
    return roleAssignmentResponses.stream()
        .filter(
            roleAssignmentResponse -> roleAssignmentResponse.getRoleAssignment().getPrincipal().getType().equals(USER))
        .map(roleAssignmentResponse -> roleAssignmentResponse.getRoleAssignment().getPrincipal().getIdentifier())
        .distinct()
        .collect(toList());
  }

  @Override
  public Optional<UserMembership> getUserMembership(String userId) {
    return userMembershipRepository.findDistinctByUserId(userId);
  }

  @Override
  public void addUserToScope(UserInfo user, Scope scope, UserMembershipUpdateMechanism mechanism) {
    addUserToScope(user.getUuid(), user.getEmail(), scope, true, mechanism);
  }

  @Override
  public void addUserToScope(
      UserInfo user, Scope scope, boolean postCreation, UserMembershipUpdateMechanism mechanism) {
    addUserToScope(user.getUuid(), user.getEmail(), scope, postCreation, mechanism);
  }

  @Override
  public void addUserToScope(
      String userId, Scope scope, String roleIdentifier, UserMembershipUpdateMechanism mechanism) {
    Optional<UserInfo> userOptional = getUserById(userId);
    if (!userOptional.isPresent()) {
      return;
    }
    UserInfo user = userOptional.get();
    addUserToScope(user.getUuid(), user.getEmail(), scope, true, mechanism);
    if (!StringUtils.isBlank(roleIdentifier)) {
      RoleAssignmentDTO roleAssignmentDTO = RoleAssignmentDTO.builder()
                                                .roleIdentifier(roleIdentifier)
                                                .disabled(false)
                                                .principal(PrincipalDTO.builder().type(USER).identifier(userId).build())
                                                .resourceGroupIdentifier(DEFAULT_RESOURCE_GROUP_IDENTIFIER)
                                                .build();
      try {
        getResponse(accessControlAdminClient.createRoleAssignment(
            scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier(), roleAssignmentDTO));
      } catch (Exception e) {
        log.error(
            "Can't create role assignment with roleIdentifier {} and resourceGroupIdentifier {} for user {} at {}",
            roleIdentifier, DEFAULT_RESOURCE_GROUP_IDENTIFIER, userId,
            ScopeUtils.toString(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier()));
      }
    }
  }

  private void addUserToScope(String userId, String emailId, Scope scope, boolean addUserToParentScope,
      UserMembershipUpdateMechanism mechanism) {
    Optional<UserMembership> userMembershipOptional = userMembershipRepository.findDistinctByUserId(userId);
    UserMembership userMembership = userMembershipOptional.orElseGet(
        () -> UserMembership.builder().userId(userId).emailId(emailId).scopes(new ArrayList<>()).build());
    if (!userMembership.getScopes().contains(scope)) {
      userMembership.getScopes().add(scope);
      //    Adding user to the account for signin flow to work
      addUserToAccount(userId, scope);
    }
    UserMembership finalUserMembership = userMembership;
    userMembership = Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      UserMembership updatedUserMembership = userMembershipRepository.save(finalUserMembership);
      outboxService.save(new UserMembershipAddEvent(scope.getAccountIdentifier(), scope, emailId, userId, mechanism));
      return updatedUserMembership;
    }));
    if (addUserToParentScope) {
      addUserToParentScope(userMembership, userId, scope, mechanism);
    }
  }

  private void addUserToParentScope(
      UserMembership userMembership, String userId, Scope scope, UserMembershipUpdateMechanism mechanism) {
    //  Adding user to the parent scopes as well
    if (!isBlank(scope.getProjectIdentifier())) {
      Scope orgScope = Scope.builder()
                           .accountIdentifier(scope.getAccountIdentifier())
                           .orgIdentifier(scope.getOrgIdentifier())
                           .build();
      if (!userMembership.getScopes().contains(orgScope)) {
        userMembership.getScopes().add(orgScope);
        UserMembership finalUserMembership = userMembership;
        userMembership = Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
          UserMembership updatedUserMembership = userMembershipRepository.save(finalUserMembership);
          outboxService.save(new UserMembershipAddEvent(scope.getAccountIdentifier(), orgScope,
              finalUserMembership.getEmailId(), finalUserMembership.getUserId(), mechanism));
          return updatedUserMembership;
        }));
      }
      RoleAssignmentDTO roleAssignmentDTO = RoleAssignmentDTO.builder()
                                                .principal(PrincipalDTO.builder().type(USER).identifier(userId).build())
                                                .resourceGroupIdentifier(DEFAULT_RESOURCE_GROUP_IDENTIFIER)
                                                .disabled(false)
                                                .roleIdentifier(ORGANIZATION_VIEWER)
                                                .build();

      try {
        NGRestUtils.getResponse(accessControlAdminClient.createRoleAssignment(
            scope.getAccountIdentifier(), scope.getOrgIdentifier(), null, roleAssignmentDTO));
      } catch (Exception e) {
        log.error("Couldn't add user to org scope as org viewer", e);
      }
    }

    if (!isBlank(scope.getOrgIdentifier())) {
      Scope accountScope = Scope.builder().accountIdentifier(scope.getAccountIdentifier()).build();
      if (!userMembership.getScopes().contains(accountScope)) {
        userMembership.getScopes().add(accountScope);
        UserMembership finalUserMembership = userMembership;
        Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
          UserMembership updatedUserMembership = userMembershipRepository.save(finalUserMembership);
          outboxService.save(new UserMembershipAddEvent(scope.getAccountIdentifier(), accountScope,
              finalUserMembership.getEmailId(), finalUserMembership.getUserId(), mechanism));
          return updatedUserMembership;
        }));
      }
      RoleAssignmentDTO roleAssignmentDTO = RoleAssignmentDTO.builder()
                                                .principal(PrincipalDTO.builder().type(USER).identifier(userId).build())
                                                .resourceGroupIdentifier(DEFAULT_RESOURCE_GROUP_IDENTIFIER)
                                                .disabled(false)
                                                .roleIdentifier(ACCOUNT_VIEWER)
                                                .build();

      try {
        NGRestUtils.getResponse(
            accessControlAdminClient.createRoleAssignment(scope.getAccountIdentifier(), null, null, roleAssignmentDTO));
      } catch (Exception e) {
        log.error("Couldn't add user to the account scope", e);
      }
    }
  }

  private void addUserToAccount(String userId, Scope scope) {
    try {
      RestClientUtils.getResponse(userClient.addUserToAccount(userId, scope.getAccountIdentifier()));
    } catch (Exception e) {
      log.error("Couldn't add user to the account", e);
    }
  }

  @Override
  public List<UserInfo> getUsersByIds(List<String> userIds, String accountId) {
    return RestClientUtils.getResponse(
        userClient.listUsers(UserSearchFilter.builder().userIds(userIds).build(), accountId));
  }

  @Override
  public Optional<UserInfo> getUserById(String userId) {
    return RestClientUtils.getResponse(userClient.getUserById(userId));
  }

  @Override
  public boolean isUserInAccount(String accountId, String userId) {
    return Boolean.TRUE.equals(RestClientUtils.getResponse(userClient.isUserInAccount(accountId, userId)));
  }

  @Override
  public boolean isUserAtScope(String userId, Scope scope) {
    Optional<UserMembership> userMembershipOpt = getUserMembership(userId);
    if (!userMembershipOpt.isPresent()) {
      return false;
    }
    return userMembershipOpt.get().getScopes().contains(scope);
  }

  @Override
  public void removeUserFromScope(String userId, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, UserMembershipUpdateMechanism mechanism) {
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
    Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      UserMembership updatedUserMembership = userMembershipRepository.save(userMembership);
      outboxService.save(new UserMembershipRemoveEvent(
          scope.getAccountIdentifier(), scope, userMembership.getEmailId(), userId, mechanism));
      return updatedUserMembership;
    }));
    boolean isUserRemovedFromAccount =
        scopes.stream().noneMatch(scope1 -> scope1.getAccountIdentifier().equals(accountIdentifier));
    if (isUserRemovedFromAccount) {
      RestClientUtils.getResponse(userClient.safeDeleteUser(userId, accountIdentifier));
    }
  }

  @Override
  public Set<String> filterUsersWithScopeMembership(List<String> userIds, String accountIdentifier,
      @Nullable String orgIdentifier, @Nullable String projectIdentifier) {
    return userMembershipRepository.filterUsersWithMembership(
        userIds, accountIdentifier, orgIdentifier, projectIdentifier);
  }

  @Override
  public Page<ProjectDTO> listProjects(String accountId, PageRequest pageRequest) {
    Optional<String> userId = getUserIdentifier();
    if (userId.isPresent()) {
      Pageable pageable = PageUtils.getPageRequest(pageRequest);
      List<Project> projects = userMembershipRepository.findProjectList(userId.get(), pageable);
      List<ProjectDTO> projectDTOList = projects.stream().map(ProjectMapper::writeDTO).collect(Collectors.toList());
      return new PageImpl<>(projectDTOList, pageable, userMembershipRepository.getProjectCount(userId.get()));
    } else {
      throw new IllegalStateException("user login required");
    }
  }

  private Optional<String> getUserIdentifier() {
    Optional<String> userId = Optional.empty();
    if (SourcePrincipalContextBuilder.getSourcePrincipal() != null
        && SourcePrincipalContextBuilder.getSourcePrincipal().getType() == PrincipalType.USER) {
      userId = Optional.of(SourcePrincipalContextBuilder.getSourcePrincipal().getName());
    }
    return userId;
  }
}
