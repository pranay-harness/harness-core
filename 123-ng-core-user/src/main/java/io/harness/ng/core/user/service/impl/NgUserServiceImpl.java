package io.harness.ng.core.user.service.impl;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.user.UserMembershipUpdateSource.SYSTEM;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.remote.client.NGRestUtils.getResponse;
import static io.harness.springdata.TransactionUtils.DEFAULT_TRANSACTION_RETRY_POLICY;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentCreateRequestDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.beans.Scope.ScopeKeys;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.UserGroupFilterDTO;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.events.AddCollaboratorEvent;
import io.harness.ng.core.events.RemoveCollaboratorEvent;
import io.harness.ng.core.invites.dto.UserMetadataDTO;
import io.harness.ng.core.remote.ProjectMapper;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserMembershipUpdateSource;
import io.harness.ng.core.user.entities.UserGroup;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.entities.UserMembership.UserMembershipKeys;
import io.harness.ng.core.user.remote.dto.UserFilter;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.outbox.api.OutboxService;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.repositories.user.spring.UserMembershipRepository;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.PrincipalType;
import io.harness.user.remote.UserClient;
import io.harness.user.remote.UserFilterNG;
import io.harness.utils.PageUtils;
import io.harness.utils.ScopeUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.support.TransactionTemplate;

@Singleton
@Slf4j
@OwnedBy(PL)
public class NgUserServiceImpl implements NgUserService {
  private static final String ACCOUNT_ADMIN = "_account_admin";
  public static final String ACCOUNT_VIEWER = "_account_viewer";
  public static final String ORGANIZATION_VIEWER = "_organization_viewer";
  public static final String PROJECT_VIEWER = "_project_viewer";
  private static final String DEFAULT_RESOURCE_GROUP_IDENTIFIER = "_all_resources";
  private final List<String> MANAGED_ROLE_IDENTIFIERS =
      ImmutableList.of(ACCOUNT_VIEWER, ORGANIZATION_VIEWER, PROJECT_VIEWER);
  public static final int DEFAULT_PAGE_SIZE = 1000;
  private final UserClient userClient;
  private final UserMembershipRepository userMembershipRepository;
  private final AccessControlAdminClient accessControlAdminClient;
  private final TransactionTemplate transactionTemplate;
  private final OutboxService outboxService;
  private final UserGroupService userGroupService;

  private final RetryPolicy<Object> transactionRetryPolicy = DEFAULT_TRANSACTION_RETRY_POLICY;

  @Inject
  public NgUserServiceImpl(UserClient userClient, UserMembershipRepository userMembershipRepository,
      AccessControlAdminClient accessControlAdminClient,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate, OutboxService outboxService,
      UserGroupService userGroupService) {
    this.userClient = userClient;
    this.userMembershipRepository = userMembershipRepository;
    this.accessControlAdminClient = accessControlAdminClient;
    this.transactionTemplate = transactionTemplate;
    this.outboxService = outboxService;
    this.userGroupService = userGroupService;
  }

  @Override
  public Page<UserInfo> listCurrentGenUsers(String accountIdentifier, String searchString, Pageable pageable) {
    io.harness.beans.PageResponse<UserInfo> userPageResponse = RestClientUtils.getResponse(userClient.list(
        accountIdentifier, String.valueOf(pageable.getOffset()), String.valueOf(pageable.getPageSize()), searchString));
    List<UserInfo> users = userPageResponse.getResponse();
    return new PageImpl<>(users, pageable, users.size());
  }

  @Override
  public PageResponse<UserMetadataDTO> listUsers(Scope scope, PageRequest pageRequest, UserFilter userFilter) {
    Criteria criteria = Criteria.where(UserMembershipKeys.scopes)
                            .elemMatch(Criteria.where(ScopeKeys.accountIdentifier)
                                           .is(scope.getAccountIdentifier())
                                           .and(ScopeKeys.orgIdentifier)
                                           .is(scope.getOrgIdentifier())
                                           .and(ScopeKeys.projectIdentifier)
                                           .is(scope.getProjectIdentifier()));
    if (userFilter != null) {
      if (isNotBlank(userFilter.getSearchTerm())) {
        criteria.orOperator(Criteria.where(UserMembershipKeys.name).regex(userFilter.getSearchTerm(), "i"),
            Criteria.where(UserMembershipKeys.emailId).regex(userFilter.getSearchTerm(), "i"));
      }
      if (userFilter.getIdentifiers() != null) {
        criteria.and(UserMembershipKeys.userId).in(userFilter.getIdentifiers());
      }
    }
    Page<UserMembership> userMembershipPage = userMembershipRepository.findAll(criteria, getPageRequest(pageRequest));
    List<UserMetadataDTO> users = userMembershipPage.getContent()
                                      .stream()
                                      .map(userMembership
                                          -> UserMetadataDTO.builder()
                                                 .uuid(userMembership.getUserId())
                                                 .email(userMembership.getEmailId())
                                                 .name(userMembership.getName())
                                                 .build())
                                      .collect(toList());
    return PageUtils.getNGPageResponse(userMembershipPage, users);
  }

  @Override
  public List<String> listUserIds(Scope scope) {
    Criteria criteria = Criteria.where(UserMembershipKeys.scopes)
                            .elemMatch(Criteria.where(ScopeKeys.accountIdentifier)
                                           .is(scope.getAccountIdentifier())
                                           .and(ScopeKeys.orgIdentifier)
                                           .is(scope.getOrgIdentifier())
                                           .and(ScopeKeys.projectIdentifier)
                                           .is(scope.getProjectIdentifier()));
    return userMembershipRepository.findAllUserIds(criteria, Pageable.unpaged()).getContent();
  }

  @Override
  public List<UserMembership> listUserMemberships(Criteria criteria) {
    return userMembershipRepository.findAll(criteria);
  }

  public Optional<UserInfo> getUserFromEmail(String email) {
    return RestClientUtils.getResponse(userClient.getUserByEmailId(email));
  }

  @Override
  public List<UserInfo> listCurrentGenUsers(String accountId, UserFilterNG userFilter) {
    return RestClientUtils.getResponse(userClient.listUsers(
        accountId, UserFilterNG.builder().emailIds(userFilter.getEmailIds()).userIds(userFilter.getUserIds()).build()));
  }

  @Override
  public List<UserMetadataDTO> listUsersHavingRole(Scope scope, String roleIdentifier) {
    PageResponse<RoleAssignmentResponseDTO> roleAssignmentPage =
        getResponse(accessControlAdminClient.getFilteredRoleAssignments(scope.getAccountIdentifier(),
            scope.getOrgIdentifier(), scope.getProjectIdentifier(), 0, DEFAULT_PAGE_SIZE,
            RoleAssignmentFilterDTO.builder().roleFilter(Collections.singleton(roleIdentifier)).build()));
    List<PrincipalDTO> principals =
        roleAssignmentPage.getContent().stream().map(dto -> dto.getRoleAssignment().getPrincipal()).collect(toList());
    Set<String> userIds = principals.stream()
                              .filter(principal -> USER.equals(principal.getType()))
                              .map(PrincipalDTO::getIdentifier)
                              .collect(Collectors.toCollection(HashSet::new));
    List<String> userGroupIds = principals.stream()
                                    .filter(principal -> USER_GROUP.equals(principal.getType()))
                                    .map(PrincipalDTO::getIdentifier)
                                    .distinct()
                                    .collect(toList());
    UserGroupFilterDTO userGroupFilterDTO = UserGroupFilterDTO.builder()
                                                .accountIdentifier(scope.getAccountIdentifier())
                                                .orgIdentifier(scope.getOrgIdentifier())
                                                .projectIdentifier(scope.getProjectIdentifier())
                                                .identifierFilter(new HashSet<>(userGroupIds))
                                                .build();
    List<UserGroup> userGroups = userGroupService.list(userGroupFilterDTO);
    userGroups.forEach(userGroup -> userIds.addAll(userGroup.getUsers()));
    return getUserMetadata(new ArrayList<>(userIds));
  }

  @Override
  public Optional<UserMembership> getUserMembership(String userId) {
    return userMembershipRepository.findDistinctByUserId(userId);
  }

  @Override
  public Optional<UserMetadataDTO> getUserMetadata(String userId) {
    Optional<UserMembership> userMembershipOptional = getUserMembership(userId);
    if (!userMembershipOptional.isPresent()) {
      return Optional.empty();
    }
    UserMembership userMembership = userMembershipOptional.get();
    UserMetadataDTO user = UserMetadataDTO.builder()
                               .uuid(userMembership.getUuid())
                               .email(userMembership.getEmailId())
                               .name(userMembership.getName())
                               .build();
    if (user.getName() == null) {
      Optional<UserInfo> userInfo = getUserById(userId);
      if (!userInfo.isPresent()) {
        return Optional.of(user);
      }
      String username = userInfo.get().getName();
      user.setName(username);
      userMembership.setName(username);
      Update update = new Update().set(UserMembershipKeys.name, username);
      update(userId, update);
    }
    return Optional.of(user);
  }

  @Override
  public List<UserMetadataDTO> getUserMetadata(List<String> userIds) {
    return userMembershipRepository.getUserMetadata(Criteria.where(UserMembershipKeys.userId).in(userIds));
  }

  @Override
  public void addUserToScope(UserInfo user, Scope scope, UserMembershipUpdateSource source) {
    addUserToScope(user.getUuid(), scope, true, source);
  }

  @Override
  public void addUserToScope(UserInfo user, Scope scope, boolean postCreation, UserMembershipUpdateSource source) {
    addUserToScope(user.getUuid(), scope, postCreation, source);
  }

  @Override
  public void addUserToScope(String userId, Scope scope, String roleIdentifier, UserMembershipUpdateSource source) {
    List<RoleAssignmentDTO> roleAssignmentDTOs = new ArrayList<>(1);
    if (!StringUtils.isBlank(roleIdentifier)) {
      RoleAssignmentDTO roleAssignmentDTO = RoleAssignmentDTO.builder()
                                                .roleIdentifier(roleIdentifier)
                                                .disabled(false)
                                                .principal(PrincipalDTO.builder().type(USER).identifier(userId).build())
                                                .resourceGroupIdentifier(DEFAULT_RESOURCE_GROUP_IDENTIFIER)
                                                .build();
      roleAssignmentDTOs.add(roleAssignmentDTO);
    }
    addUserToScope(userId, scope, roleAssignmentDTOs, source);
  }

  @Override
  public void addUserToScope(
      String userId, Scope scope, List<RoleAssignmentDTO> roleAssignmentDTOs, UserMembershipUpdateSource source) {
    addUserToScope(userId, scope, true, source);
    createRoleAssignments(userId, scope, roleAssignmentDTOs);
  }

  private void createRoleAssignments(String userId, Scope scope, List<RoleAssignmentDTO> roleAssignmentDTOs) {
    List<RoleAssignmentDTO> managedRoleAssignments =
        roleAssignmentDTOs.stream().filter(this::isRoleAssignmentManaged).collect(toList());
    List<RoleAssignmentDTO> userRoleAssignments =
        roleAssignmentDTOs.stream()
            .filter(((Predicate<RoleAssignmentDTO>) this::isRoleAssignmentManaged).negate())
            .collect(toList());

    try {
      RoleAssignmentCreateRequestDTO createRequestDTO =
          RoleAssignmentCreateRequestDTO.builder().roleAssignments(managedRoleAssignments).build();
      getResponse(accessControlAdminClient.createMultiRoleAssignment(scope.getAccountIdentifier(),
          scope.getOrgIdentifier(), scope.getProjectIdentifier(), true, createRequestDTO));

      createRequestDTO = RoleAssignmentCreateRequestDTO.builder().roleAssignments(userRoleAssignments).build();
      getResponse(accessControlAdminClient.createMultiRoleAssignment(scope.getAccountIdentifier(),
          scope.getOrgIdentifier(), scope.getProjectIdentifier(), false, createRequestDTO));

    } catch (Exception e) {
      log.error("Cannot create all of the role assignments in [{}] for user [{}] at [{}]", roleAssignmentDTOs, userId,
          ScopeUtils.toString(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier()));
    }
  }

  private boolean isRoleAssignmentManaged(RoleAssignmentDTO roleAssignmentDTO) {
    return MANAGED_ROLE_IDENTIFIERS.stream().anyMatch(
               roleIdentifier -> roleIdentifier.equals(roleAssignmentDTO.getRoleIdentifier()))
        && DEFAULT_RESOURCE_GROUP_IDENTIFIER.equals(roleAssignmentDTO.getResourceGroupIdentifier());
  }

  private void addUserToScope(
      String userId, Scope scope, boolean addUserToParentScope, UserMembershipUpdateSource source) {
    ensureUserMembership(userId);
    addUserToScopeInternal(userId, source, scope, getDefaultRoleIdentifier(scope));

    // Adding user to the account for sign in flow to work
    addUserToAccount(userId, scope);
    if (addUserToParentScope) {
      addUserToParentScope(userId, scope, source);
    }
  }

  private String getDefaultRoleIdentifier(Scope scope) {
    if (scope == null) {
      return null;
    }
    if (!isBlank(scope.getProjectIdentifier())) {
      return PROJECT_VIEWER;
    } else if (!isBlank(scope.getOrgIdentifier())) {
      return ORGANIZATION_VIEWER;
    }
    return ACCOUNT_VIEWER;
  }

  private void ensureUserMembership(String userId) {
    Optional<UserMembership> userMembershipOptional = getUserMembership(userId);
    if (userMembershipOptional.isPresent()) {
      return;
    }
    Optional<UserInfo> userInfoOptional = getUserById(userId);
    UserInfo userInfo = userInfoOptional.orElseThrow(
        () -> new InvalidRequestException(String.format("User with id %s doesn't exists", userId)));
    UserMembership userMembership = UserMembership.builder()
                                        .userId(userInfo.getUuid())
                                        .name(userInfo.getName())
                                        .emailId(userInfo.getEmail())
                                        .build();
    try {
      userMembershipRepository.save(userMembership);
    } catch (DuplicateKeyException e) {
      log.info(
          "DuplicateKeyException while creating usermembership for user id {}. This race condition is benign", userId);
    }
  }

  private void addUserToParentScope(String userId, Scope scope, UserMembershipUpdateSource source) {
    //  Adding user to the parent scopes as well
    if (!isBlank(scope.getProjectIdentifier())) {
      Scope orgScope = Scope.builder()
                           .accountIdentifier(scope.getAccountIdentifier())
                           .orgIdentifier(scope.getOrgIdentifier())
                           .build();
      Failsafe.with(transactionRetryPolicy)
          .get(() -> addUserToScopeInternal(userId, source, orgScope, ORGANIZATION_VIEWER));
    }

    if (!isBlank(scope.getOrgIdentifier())) {
      Scope accountScope = Scope.builder().accountIdentifier(scope.getAccountIdentifier()).build();
      Failsafe.with(transactionRetryPolicy)
          .get(() -> addUserToScopeInternal(userId, source, accountScope, ACCOUNT_VIEWER));
    }
  }

  private UserMembership addUserToScopeInternal(
      String userId, UserMembershipUpdateSource source, Scope scope, String roleIdentifier) {
    Optional<UserMembership> userMembershipOpt = getUserMembership(userId);
    UserMembership userMembership =
        userMembershipOpt.orElseThrow(() -> new IllegalStateException("Usermembership doesn't exist for " + userId));
    if (!userMembership.getScopes().contains(scope)) {
      Update update = new Update().push(UserMembershipKeys.scopes, scope);
      String email = userMembership.getEmailId();
      userMembership = Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        UserMembership updatedUserMembership = userMembershipRepository.update(userId, update);
        outboxService.save(new AddCollaboratorEvent(scope.getAccountIdentifier(), scope, email, userId, source));
        return updatedUserMembership;
      }));
    }

    try {
      RoleAssignmentDTO roleAssignmentDTO = RoleAssignmentDTO.builder()
                                                .principal(PrincipalDTO.builder().type(USER).identifier(userId).build())
                                                .resourceGroupIdentifier(DEFAULT_RESOURCE_GROUP_IDENTIFIER)
                                                .disabled(false)
                                                .roleIdentifier(roleIdentifier)
                                                .build();
      NGRestUtils.getResponse(accessControlAdminClient.createMultiRoleAssignment(scope.getAccountIdentifier(),
          scope.getOrgIdentifier(), scope.getProjectIdentifier(), true,
          RoleAssignmentCreateRequestDTO.builder()
              .roleAssignments(Collections.singletonList(roleAssignmentDTO))
              .build()));
    } catch (Exception e) {
      /**
       *  It's expected that user will already have this roleassignment.
       */
    }
    return userMembership;
  }

  private void addUserToAccount(String userId, Scope scope) {
    log.info("Adding user {} to account {}", userId, scope.getAccountIdentifier());
    try {
      RestClientUtils.getResponse(userClient.addUserToAccount(userId, scope.getAccountIdentifier()));
    } catch (Exception e) {
      log.error("Couldn't add user to the account", e);
    }
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
    return userMembershipOpt.map(userMembership -> userMembership.getScopes().contains(scope)).orElse(false);
  }

  @Override
  public boolean update(String userId, Update update) {
    return userMembershipRepository.update(userId, update) != null;
  }

  @Override
  public boolean removeUserFromScope(String userId, Scope scope, UserMembershipUpdateSource source) {
    Optional<UserMembership> userMembershipOptional = getUserMembership(userId);
    if (!userMembershipOptional.isPresent()) {
      return false;
    }
    UserMembership userMembership = userMembershipOptional.get();
    if (!UserMembershipUpdateSource.SYSTEM.equals(source) && isUserPartOfChildScope(userMembership, scope)) {
      throw new InvalidRequestException(getDeleteUserErrorMessage(scope));
    }
    if (ScopeUtils.isAccountScope(scope)) {
      List<UserMetadataDTO> accountAdmins =
          listUsersHavingRole(Scope.builder().accountIdentifier(scope.getAccountIdentifier()).build(), ACCOUNT_ADMIN);
      if (accountAdmins.stream().allMatch(userMetadata -> userId.equals(userMetadata.getUuid()))) {
        throw new InvalidRequestException("This user is the only account-admin left. Can't Remove it");
      }
    }

    List<Scope> scopes = userMembership.getScopes();
    if (!scopes.contains(scope)) {
      return true;
    }
    scopes.remove(scope);
    Update update = new Update().pull(UserMembershipKeys.scopes, scope);
    Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      UserMembership updatedUserMembership = userMembershipRepository.update(userId, update);
      outboxService.save(new RemoveCollaboratorEvent(
          scope.getAccountIdentifier(), scope, userMembership.getEmailId(), userId, source));
      return updatedUserMembership;
    }));
    boolean isUserRemovedFromAccount =
        scopes.stream().noneMatch(scope1 -> scope1.getAccountIdentifier().equals(scope.getAccountIdentifier()));
    if (isUserRemovedFromAccount) {
      RestClientUtils.getResponse(userClient.safeDeleteUser(userId, scope.getAccountIdentifier()));
    }
    return true;
  }

  @Override
  public boolean removeUserFromAccount(String userId, String accountIdentifier) {
    Optional<UserMembership> userMembershipOptional = getUserMembership(userId);
    if (!userMembershipOptional.isPresent()) {
      return true;
    }
    UserMembership userMembership = userMembershipOptional.get();
    List<Scope> scopesInsideAccount = userMembership.getScopes()
                                          .stream()
                                          .filter(scope -> accountIdentifier.equals(scope.getAccountIdentifier()))
                                          .collect(Collectors.toList());
    scopesInsideAccount.forEach(scope -> removeUserFromScope(userId, scope, SYSTEM));
    return true;
  }

  @Override
  public boolean removeUser(String userId) {
    Optional<UserMembership> userMembershipOptional = getUserMembership(userId);
    if (!userMembershipOptional.isPresent()) {
      return true;
    }
    List<Scope> scopes = userMembershipOptional.get().getScopes();
    scopes.forEach(scope -> removeUserFromScope(userId, scope, SYSTEM));
    userMembershipRepository.delete(userMembershipOptional.get());
    return true;
  }

  private String getDeleteUserErrorMessage(Scope scope) {
    return String.format("Please delete the user from the %ss in this %s and try again",
        StringUtils.capitalize(ScopeUtils.getImmediateNextScope(scope.getAccountIdentifier(), scope.getOrgIdentifier())
                                   .toString()
                                   .toLowerCase()),
        StringUtils.capitalize(ScopeUtils
                                   .getMostSignificantScope(scope.getAccountIdentifier(), scope.getOrgIdentifier(),
                                       scope.getProjectIdentifier())
                                   .toString()
                                   .toLowerCase()));
  }

  private boolean isUserPartOfChildScope(UserMembership userMembership, Scope scope) {
    if (!isBlank(scope.getProjectIdentifier())) {
      return false;
    }
    if (!isBlank(scope.getOrgIdentifier())) {
      return userMembership.getScopes().stream().anyMatch(scope1
          -> scope.getOrgIdentifier().equals(scope1.getOrgIdentifier()) && !isBlank(scope1.getProjectIdentifier()));
    }
    return userMembership.getScopes().stream().anyMatch(scope1
        -> scope.getAccountIdentifier().equals(scope1.getAccountIdentifier()) && !isBlank(scope1.getOrgIdentifier()));
  }

  @Override
  public Set<String> filterUsersWithScopeMembership(List<String> userIds, String accountIdentifier,
      @Nullable String orgIdentifier, @Nullable String projectIdentifier) {
    return userMembershipRepository.filterUsersWithMembership(
        userIds, accountIdentifier, orgIdentifier, projectIdentifier);
  }

  @Override
  public Page<ProjectDTO> listProjects(String accountId, PageRequest pageRequest) {
    Optional<String> userId = getUserIdentifierFromSecurityContext();
    if (userId.isPresent()) {
      Pageable pageable = PageUtils.getPageRequest(pageRequest);
      List<Project> projects = userMembershipRepository.findProjectList(userId.get(), accountId, pageable);
      List<ProjectDTO> projectDTOList = projects.stream().map(ProjectMapper::writeDTO).collect(Collectors.toList());
      return new PageImpl<>(projectDTOList, pageable, userMembershipRepository.getProjectCount(userId.get()));
    } else {
      throw new IllegalStateException("user login required");
    }
  }

  @Override
  public boolean isUserPasswordSet(String accountIdentifier, String email) {
    return RestClientUtils.getResponse(userClient.isUserPasswordSet(accountIdentifier, email));
  }

  private Optional<String> getUserIdentifierFromSecurityContext() {
    Optional<String> userId = Optional.empty();
    if (SourcePrincipalContextBuilder.getSourcePrincipal() != null
        && SourcePrincipalContextBuilder.getSourcePrincipal().getType() == PrincipalType.USER) {
      userId = Optional.of(SourcePrincipalContextBuilder.getSourcePrincipal().getName());
    }
    return userId;
  }
}
