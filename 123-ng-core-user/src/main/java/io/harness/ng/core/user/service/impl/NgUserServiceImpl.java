package io.harness.ng.core.user.service.impl;

import static io.harness.accesscontrol.principals.PrincipalType.SERVICE_ACCOUNT;
import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.remote.client.NGRestUtils.getResponse;
import static io.harness.springdata.TransactionUtils.DEFAULT_TRANSACTION_RETRY_POLICY;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.lang.Boolean.TRUE;
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
import io.harness.exception.ProcessingException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.dto.GatewayAccountRequestDTO;
import io.harness.ng.core.dto.UserGroupFilterDTO;
import io.harness.ng.core.events.AddCollaboratorEvent;
import io.harness.ng.core.events.RemoveCollaboratorEvent;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserMembershipUpdateSource;
import io.harness.ng.core.user.entities.UserGroup;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.entities.UserMembership.UserMembershipKeys;
import io.harness.ng.core.user.entities.UserMetadata;
import io.harness.ng.core.user.entities.UserMetadata.UserMetadataKeys;
import io.harness.ng.core.user.exception.InvalidUserRemoveRequestException;
import io.harness.ng.core.user.remote.dto.UserFilter;
import io.harness.ng.core.user.remote.dto.UserMetadataDTO;
import io.harness.ng.core.user.remote.mapper.UserMetadataMapper;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.outbox.api.OutboxService;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.repositories.user.spring.UserMembershipRepository;
import io.harness.repositories.user.spring.UserMetadataRepository;
import io.harness.user.remote.UserClient;
import io.harness.user.remote.UserFilterNG;
import io.harness.utils.PageUtils;
import io.harness.utils.ScopeUtils;
import io.harness.utils.TimeLogger;

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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
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
  private static final String PROCESSING_EXCEPTION_MESSAGE = "Could not process user remove request in stipulated time";
  public static final String THREAD_POOL_NAME = "user-service-delete";
  private static final String ACCOUNT_ADMIN = "_account_admin";
  private static final String ACCOUNT_VIEWER = "_account_viewer";
  private static final String ORGANIZATION_VIEWER = "_organization_viewer";
  private static final String ORG_ADMIN = "_organization_admin";
  private static final String PROJECT_ADMIN = "_project_admin";
  private static final String PROJECT_VIEWER = "_project_viewer";
  private static final String DEFAULT_RESOURCE_GROUP_IDENTIFIER = "_all_resources";
  private final List<String> MANAGED_ROLE_IDENTIFIERS =
      ImmutableList.of(ACCOUNT_VIEWER, ORGANIZATION_VIEWER, PROJECT_VIEWER);
  public static final int DEFAULT_PAGE_SIZE = 10000;
  private final UserClient userClient;
  private final UserMembershipRepository userMembershipRepository;
  private final AccessControlAdminClient accessControlAdminClient;
  private final TransactionTemplate transactionTemplate;
  private final OutboxService outboxService;
  private final UserGroupService userGroupService;
  private final UserMetadataRepository userMetadataRepository;
  private final ExecutorService executorService;
  private static final RetryPolicy<Object> transactionRetryPolicy = DEFAULT_TRANSACTION_RETRY_POLICY;

  @Inject
  public NgUserServiceImpl(UserClient userClient, UserMembershipRepository userMembershipRepository,
      @Named("PRIVILEGED") AccessControlAdminClient accessControlAdminClient,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate, OutboxService outboxService,
      UserGroupService userGroupService, UserMetadataRepository userMetadataRepository,
      @Named(THREAD_POOL_NAME) ExecutorService executorService) {
    this.userClient = userClient;
    this.userMembershipRepository = userMembershipRepository;
    this.accessControlAdminClient = accessControlAdminClient;
    this.transactionTemplate = transactionTemplate;
    this.outboxService = outboxService;
    this.userGroupService = userGroupService;
    this.userMetadataRepository = userMetadataRepository;
    this.executorService = executorService;
  }

  @Override
  public Page<UserInfo> listCurrentGenUsers(String accountIdentifier, String searchString, Pageable pageable) {
    io.harness.beans.PageResponse<UserInfo> userPageResponse =
        RestClientUtils.getResponse(userClient.list(accountIdentifier, String.valueOf(pageable.getOffset()),
            String.valueOf(pageable.getPageSize()), searchString, false));
    List<UserInfo> users = userPageResponse.getResponse();
    return new PageImpl<>(users, pageable, users.size());
  }

  @Override
  public PageResponse<UserMetadataDTO> listUsers(Scope scope, PageRequest pageRequest, UserFilter userFilter) {
    Criteria userMembershipCriteria;
    if (userFilter == null || UserFilter.ParentFilter.NO_PARENT_SCOPES.equals(userFilter.getParentFilter())) {
      userMembershipCriteria = getUserMembershipCriteria(scope, false);
    } else if (UserFilter.ParentFilter.INCLUDE_PARENT_SCOPES.equals(userFilter.getParentFilter())) {
      userMembershipCriteria = getUserMembershipCriteria(scope, true);
    } else {
      userMembershipCriteria = getUserMembershipCriteria(scope, true);
      List<String> negativeUserIds = listUserIds(scope);
      userMembershipCriteria.and(UserMembershipKeys.userId).nin(negativeUserIds);
    }
    Criteria userMetadataCriteria = new Criteria();
    if (userFilter != null) {
      if (isNotBlank(userFilter.getSearchTerm())) {
        userMetadataCriteria.orOperator(Criteria.where(UserMetadataKeys.name).regex(userFilter.getSearchTerm(), "i"),
            Criteria.where(UserMetadataKeys.email).regex(userFilter.getSearchTerm(), "i"));
      }
      if (userFilter.getIdentifiers() != null) {
        userMembershipCriteria.and(UserMembershipKeys.userId).in(userFilter.getIdentifiers());
      }
    }
    Page<String> userMembershipPage =
        userMembershipRepository.findAllUserIds(userMembershipCriteria, Pageable.unpaged());
    if (userMembershipPage.isEmpty()) {
      return PageResponse.getEmptyPageResponse(pageRequest);
    }

    userMetadataCriteria.and(UserMetadataKeys.userId).in(userMembershipPage.getContent());
    Page<UserMetadata> userMetadataPage =
        userMetadataRepository.findAll(userMetadataCriteria, getPageRequest(pageRequest));

    return PageUtils.getNGPageResponse(userMetadataPage.map(UserMetadataMapper::toDTO));
  }

  private Criteria getUserMembershipCriteria(Scope scope, boolean includeParentScopes) {
    Criteria userMembershipCriteria = new Criteria();
    if (includeParentScopes) {
      List<Criteria> scopeCriterion = new ArrayList<>();
      scopeCriterion.add(Criteria.where(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY)
                             .is(scope.getAccountIdentifier())
                             .and(UserMembershipKeys.ORG_IDENTIFIER_KEY)
                             .is(null)
                             .and(UserMembershipKeys.PROJECT_IDENTIFIER_KEY)
                             .is(null));
      if (isNotEmpty(scope.getOrgIdentifier())) {
        scopeCriterion.add(Criteria.where(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY)
                               .is(scope.getAccountIdentifier())
                               .and(UserMembershipKeys.ORG_IDENTIFIER_KEY)
                               .is(scope.getOrgIdentifier())
                               .and(UserMembershipKeys.PROJECT_IDENTIFIER_KEY)
                               .is(null));
        if (isNotEmpty(scope.getProjectIdentifier())) {
          scopeCriterion.add(Criteria.where(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY)
                                 .is(scope.getAccountIdentifier())
                                 .and(UserMembershipKeys.ORG_IDENTIFIER_KEY)
                                 .is(scope.getOrgIdentifier())
                                 .and(UserMembershipKeys.PROJECT_IDENTIFIER_KEY)
                                 .is(scope.getProjectIdentifier()));
        }
      }
      if (scopeCriterion.size() == 1) {
        userMembershipCriteria = scopeCriterion.get(0);
      } else {
        userMembershipCriteria.orOperator(scopeCriterion.toArray(new Criteria[0]));
      }
    } else {
      userMembershipCriteria.and(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY)
          .is(scope.getAccountIdentifier())
          .and(UserMembershipKeys.ORG_IDENTIFIER_KEY)
          .is(scope.getOrgIdentifier())
          .and(UserMembershipKeys.PROJECT_IDENTIFIER_KEY)
          .is(scope.getProjectIdentifier());
    }
    return userMembershipCriteria;
  }

  @Override
  public List<String> listUserIds(Scope scope) {
    Criteria criteria = Criteria.where(UserMembershipKeys.scope + "." + ScopeKeys.accountIdentifier)
                            .is(scope.getAccountIdentifier())
                            .and(UserMembershipKeys.scope + "." + ScopeKeys.orgIdentifier)
                            .is(scope.getOrgIdentifier())
                            .and(UserMembershipKeys.scope + "." + ScopeKeys.projectIdentifier)
                            .is(scope.getProjectIdentifier());
    return userMembershipRepository.findAllUserIds(criteria, Pageable.unpaged()).getContent();
  }

  @Override
  public List<UserMetadataDTO> listUsers(Scope scope) {
    List<String> userIds = listUserIds(scope);
    return getUserMetadata(userIds);
  }

  public Optional<UserMetadataDTO> getUserByEmail(String email, boolean fetchFromCurrentGen) {
    if (!fetchFromCurrentGen) {
      Optional<UserMetadata> user = userMetadataRepository.findDistinctByEmail(email);
      return user.map(UserMetadataMapper::toDTO);
    } else {
      Optional<UserInfo> userInfo = RestClientUtils.getResponse(userClient.getUserByEmailId(email));
      UserMetadataDTO userMetadataDTO = userInfo
                                            .map(user
                                                -> UserMetadataDTO.builder()
                                                       .uuid(user.getUuid())
                                                       .name(user.getName())
                                                       .email(user.getEmail())
                                                       .locked(user.isLocked())
                                                       .build())
                                            .orElse(null);
      return Optional.ofNullable(userMetadataDTO);
    }
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
    if (!userGroupIds.isEmpty()) {
      UserGroupFilterDTO userGroupFilterDTO = UserGroupFilterDTO.builder()
                                                  .accountIdentifier(scope.getAccountIdentifier())
                                                  .orgIdentifier(scope.getOrgIdentifier())
                                                  .projectIdentifier(scope.getProjectIdentifier())
                                                  .identifierFilter(new HashSet<>(userGroupIds))
                                                  .build();
      List<UserGroup> userGroups = userGroupService.list(userGroupFilterDTO);
      userGroups.forEach(userGroup -> userIds.addAll(userGroup.getUsers()));
    }
    return getUserMetadata(new ArrayList<>(userIds));
  }

  @Override
  public Optional<UserMembership> getUserMembership(String userId, Scope scope) {
    Criteria criteria = Criteria.where(UserMetadataKeys.userId)
                            .is(userId)
                            .and(UserMembershipKeys.scope + "." + ScopeKeys.accountIdentifier)
                            .is(scope.getAccountIdentifier())
                            .and(UserMembershipKeys.scope + "." + ScopeKeys.orgIdentifier)
                            .is(scope.getOrgIdentifier())
                            .and(UserMembershipKeys.scope + "." + ScopeKeys.projectIdentifier)
                            .is(scope.getProjectIdentifier());
    UserMembership userMemberships = userMembershipRepository.findOne(criteria);
    return Optional.ofNullable(userMemberships);
  }

  @Override
  public Optional<UserMetadataDTO> getUserMetadata(String userId) {
    return userMetadataRepository.findDistinctByUserId(userId).map(UserMetadataMapper::toDTO);
  }

  @Override
  public List<UserMetadataDTO> getUserMetadata(List<String> userIds) {
    return userMetadataRepository.findAll(Criteria.where(UserMembershipKeys.userId).in(userIds), Pageable.unpaged())
        .map(UserMetadataMapper::toDTO)
        .stream()
        .collect(toList());
  }

  @Override
  public Page<UserMembership> listUserMemberships(Criteria criteria, Pageable pageable) {
    return userMembershipRepository.findAll(criteria, pageable);
  }

  @Override
  public void addServiceAccountToScope(
      String serviceAccountId, Scope scope, String roleIdentifier, UserMembershipUpdateSource source) {
    List<RoleAssignmentDTO> roleAssignmentDTOs = new ArrayList<>(1);
    if (!StringUtils.isBlank(roleIdentifier)) {
      RoleAssignmentDTO roleAssignmentDTO =
          RoleAssignmentDTO.builder()
              .roleIdentifier(roleIdentifier)
              .disabled(false)
              .principal(PrincipalDTO.builder().type(SERVICE_ACCOUNT).identifier(serviceAccountId).build())
              .resourceGroupIdentifier(DEFAULT_RESOURCE_GROUP_IDENTIFIER)
              .build();
      roleAssignmentDTOs.add(roleAssignmentDTO);
    }
    createRoleAssignments(serviceAccountId, scope, roleAssignmentDTOs);
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
      log.error("Could not create all of the role assignments in [{}] for user [{}] at [{}]", roleAssignmentDTOs,
          userId,
          ScopeUtils.toString(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier()));
    }
  }

  private boolean isRoleAssignmentManaged(RoleAssignmentDTO roleAssignmentDTO) {
    return MANAGED_ROLE_IDENTIFIERS.stream().anyMatch(
               roleIdentifier -> roleIdentifier.equals(roleAssignmentDTO.getRoleIdentifier()))
        && DEFAULT_RESOURCE_GROUP_IDENTIFIER.equals(roleAssignmentDTO.getResourceGroupIdentifier());
  }

  @Override
  public void addUserToScope(
      String userId, Scope scope, boolean addUserToParentScope, UserMembershipUpdateSource source) {
    ensureUserMetadata(userId);
    addUserToScopeInternal(userId, source, scope, getDefaultRoleIdentifier(scope));
    if (addUserToParentScope) {
      addUserToParentScope(userId, scope, source);
    }
  }

  private String getDefaultRoleIdentifier(Scope scope) {
    if (!isBlank(scope.getProjectIdentifier())) {
      return PROJECT_VIEWER;
    } else if (!isBlank(scope.getOrgIdentifier())) {
      return ORGANIZATION_VIEWER;
    }
    return ACCOUNT_VIEWER;
  }

  private void ensureUserMetadata(String userId) {
    Optional<UserMetadata> userMetadataOpt = userMetadataRepository.findDistinctByUserId(userId);
    if (userMetadataOpt.isPresent()) {
      return;
    }
    Optional<UserInfo> userInfoOptional = getUserById(userId);
    UserInfo userInfo = userInfoOptional.orElseThrow(
        () -> new InvalidRequestException(String.format("User with id %s doesn't exists", userId)));

    userMetadataRepository.deleteByEmail(userInfo.getEmail());
    UserMetadata userMetadata = UserMetadata.builder()
                                    .userId(userInfo.getUuid())
                                    .name(userInfo.getName())
                                    .email(userInfo.getEmail())
                                    .locked(userInfo.isLocked())
                                    .build();
    try {
      userMetadataRepository.save(userMetadata);
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
      addUserToScopeInternal(userId, source, orgScope, ORGANIZATION_VIEWER);
    }

    if (!isBlank(scope.getOrgIdentifier())) {
      Scope accountScope = Scope.builder().accountIdentifier(scope.getAccountIdentifier()).build();
      addUserToScopeInternal(userId, source, accountScope, ACCOUNT_VIEWER);
    }
  }

  private void addUserToScopeInternal(
      String userId, UserMembershipUpdateSource source, Scope scope, String roleIdentifier) {
    Optional<UserMetadata> userMetadata = userMetadataRepository.findDistinctByUserId(userId);
    String publicIdentifier = userMetadata.map(UserMetadata::getEmail).orElse(userId);

    Failsafe.with(transactionRetryPolicy).get(() -> {
      UserMembership userMembership = null;
      try {
        userMembership = userMembershipRepository.save(UserMembership.builder().userId(userId).scope(scope).build());
      } catch (DuplicateKeyException e) {
        //  This is benign. Move on.
      }
      if (userMembership != null) {
        outboxService.save(
            new AddCollaboratorEvent(scope.getAccountIdentifier(), scope, publicIdentifier, userId, source));
      }
      return userMembership;
    });

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
       *  It's expected that user might already have this roleassignment.
       */
    }
  }

  public void addUserToCG(String userId, Scope scope) {
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
    return TRUE.equals(RestClientUtils.getResponse(userClient.isUserInAccount(accountId, userId)));
  }

  @Override
  public boolean isUserAtScope(String userId, Scope scope) {
    Optional<UserMembership> userMembershipOpt = getUserMembership(userId, scope);
    return userMembershipOpt.isPresent();
  }

  @Override
  public boolean updateUserMetadata(UserMetadataDTO user) {
    Optional<UserMetadata> savedUserOpt = userMetadataRepository.findDistinctByUserId(user.getUuid());
    if (!savedUserOpt.isPresent()) {
      return true;
    }
    if (!isBlank(user.getName())) {
      Update update = new Update();
      update.set(UserMetadataKeys.name, user.getName());
      update.set(UserMetadataKeys.locked, user.isLocked());
      return userMetadataRepository.updateFirst(user.getUuid(), update) != null;
    }
    return true;
  }

  @Override
  public boolean removeUserFromScope(String userId, Scope scope, UserMembershipUpdateSource source) {
    log.info("Trying to remove user {} from scope {}", userId, ScopeUtils.toString(scope));
    try (TimeLogger timeLogger = new TimeLogger(LoggerFactory.getLogger(getClass().getName()))) {
      Optional<UserMembership> currentScopeUserMembership = getUserMembership(userId, scope);
      if (!currentScopeUserMembership.isPresent()) {
        return false;
      }
      Criteria userMembershipCriteria = getCriteriaForFetchingChildScopes(userId, scope);
      List<UserMembership> userMemberships = userMembershipRepository.findAll(userMembershipCriteria);
      log.info(
          "User {} is part of {} scopes in account {}", userId, userMemberships.size(), scope.getAccountIdentifier());

      if (!UserMembershipUpdateSource.SYSTEM.equals(source)) {
        List<Scope> childScopes = userMemberships.stream().map(UserMembership::getScope).collect(toList());
        List<Scope> lastAdminScopes = getLastAdminScopes(userId, childScopes);
        if (!lastAdminScopes.isEmpty()) {
          throw new InvalidUserRemoveRequestException(
              InvalidUserRemoveRequestException.getExceptionMessageForUserRemove(lastAdminScopes), lastAdminScopes);
        }
      }

      Optional<UserMetadata> userMetadata = userMetadataRepository.findDistinctByUserId(userId);
      String publicIdentifier = userMetadata.map(UserMetadata::getEmail).orElse(userId);
      userMemberships.forEach(
          userMembership -> Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
            userMembershipRepository.delete(userMembership);
            outboxService.save(
                new RemoveCollaboratorEvent(scope.getAccountIdentifier(), scope, publicIdentifier, userId, source));
            return userMembership;
          })));

      UserMembership anotherMembership =
          userMembershipRepository.findOne(Criteria.where(UserMetadataKeys.userId).is(userId));
      if (anotherMembership == null) {
        RestClientUtils.getResponse(userClient.safeDeleteUser(userId, scope.getAccountIdentifier()));
      }
    }
    return true;
  }

  private List<Scope> getLastAdminScopes(String userId, List<Scope> scopes) {
    List<Callable<Boolean>> tasks = new ArrayList<>();
    scopes.forEach(scope -> tasks.add(() -> isUserLastAdminAtScope(userId, scope)));
    List<Future<Boolean>> futures;
    try {
      futures = executorService.invokeAll(tasks, 10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ProcessingException(PROCESSING_EXCEPTION_MESSAGE);
    }

    List<Boolean> results = new ArrayList<>();
    for (int i = 0; i < futures.size(); i++) {
      try {
        results.add(futures.get(i).get());
      } catch (Exception e) {
        log.error(PROCESSING_EXCEPTION_MESSAGE, e);
        throw new ProcessingException(PROCESSING_EXCEPTION_MESSAGE);
      }
    }

    List<Scope> lastAdminScopes = new ArrayList<>();
    for (int i = 0; i < results.size(); i++) {
      if (TRUE.equals(results.get(i))) {
        lastAdminScopes.add(scopes.get(i));
      }
    }
    return lastAdminScopes;
  }

  private Criteria getCriteriaForFetchingChildScopes(String userId, Scope scope) {
    Criteria criteria = Criteria.where(UserMembershipKeys.userId)
                            .is(userId)
                            .and(UserMembershipKeys.scope + "." + ScopeKeys.accountIdentifier)
                            .is(scope.getAccountIdentifier());
    if (isNotBlank(scope.getProjectIdentifier())) {
      criteria.and(UserMembershipKeys.scope + "." + ScopeKeys.orgIdentifier).is(scope.getOrgIdentifier());
      criteria.and(UserMembershipKeys.scope + "." + ScopeKeys.projectIdentifier).is(scope.getProjectIdentifier());
    } else if (isNotBlank(scope.getOrgIdentifier())) {
      criteria.and(UserMembershipKeys.scope + "." + ScopeKeys.orgIdentifier).is(scope.getOrgIdentifier());
    }
    return criteria;
  }

  private Boolean isUserLastAdminAtScope(String userId, Scope scope) {
    String roleIdentifier;
    if (!isBlank(scope.getProjectIdentifier())) {
      roleIdentifier = PROJECT_ADMIN;
    } else if (!isBlank(scope.getOrgIdentifier())) {
      roleIdentifier = ORG_ADMIN;
    } else {
      roleIdentifier = ACCOUNT_ADMIN;
    }
    List<UserMetadataDTO> scopeAdmins = listUsersHavingRole(scope, roleIdentifier);
    return scopeAdmins.stream().allMatch(userMetadata -> userId.equals(userMetadata.getUuid()));
  }

  @Override
  public boolean isUserPasswordSet(String accountIdentifier, String email) {
    return RestClientUtils.getResponse(userClient.isUserPasswordSet(accountIdentifier, email));
  }

  @Override
  public List<String> listUserAccountIds(String userId) {
    Optional<UserInfo> userInfoOptional = getUserById(userId);
    if (userInfoOptional.isPresent()) {
      return userInfoOptional.get()
          .getAccounts()
          .stream()
          .map(GatewayAccountRequestDTO::getUuid)
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }
}
