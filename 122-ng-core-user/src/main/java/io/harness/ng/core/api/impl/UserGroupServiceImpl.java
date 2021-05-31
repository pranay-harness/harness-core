package io.harness.ng.core.api.impl;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.core.utils.UserGroupMapper.toDTO;
import static io.harness.ng.core.utils.UserGroupMapper.toEntity;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.remote.client.RestClientUtils.getResponse;
import static io.harness.springdata.TransactionUtils.DEFAULT_TRANSACTION_RETRY_POLICY;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.authenticationsettings.remote.AuthSettingsManagerClient;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.dto.UserGroupDTO;
import io.harness.ng.core.dto.UserGroupFilterDTO;
import io.harness.ng.core.entities.NotificationSettingConfig;
import io.harness.ng.core.events.UserGroupCreateEvent;
import io.harness.ng.core.events.UserGroupDeleteEvent;
import io.harness.ng.core.events.UserGroupUpdateEvent;
import io.harness.ng.core.invites.dto.UserMetadataDTO;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.entities.UserGroup;
import io.harness.ng.core.user.entities.UserGroup.UserGroupKeys;
import io.harness.ng.core.user.remote.dto.UserFilter;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.ng.userprofile.services.api.UserInfoService;
import io.harness.notification.NotificationChannelType;
import io.harness.outbox.api.OutboxService;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.repositories.ng.core.spring.UserGroupRepository;
import io.harness.user.remote.UserClient;
import io.harness.user.remote.UserFilterNG;
import io.harness.utils.RetryUtils;
import io.harness.utils.ScopeUtils;

import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SSOType;
import software.wings.security.authentication.SSOConfig;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(PL)
@Singleton
@Slf4j
public class UserGroupServiceImpl implements UserGroupService {
  private static final String ACCOUNT_ADMIN = "_account_admin";
  private final UserGroupRepository userGroupRepository;
  private final UserClient userClient;
  private final OutboxService outboxService;
  private final AccessControlAdminClient accessControlAdminClient;
  private final TransactionTemplate transactionTemplate;
  private final NgUserService ngUserService;
  private final AuthSettingsManagerClient managerClient;
  private static final RetryPolicy<Object> retryPolicy =
      RetryUtils.getRetryPolicy("Could not find the user with the given identifier on attempt %s",
          "Could not find the user with the given identifier", Lists.newArrayList(InvalidRequestException.class),
          Duration.ofSeconds(5), 3, log);

  private final RetryPolicy<Object> transactionRetryPolicy = DEFAULT_TRANSACTION_RETRY_POLICY;

  @Inject
  public UserGroupServiceImpl(UserGroupRepository userGroupRepository, UserClient userClient,
      OutboxService outboxService, AccessControlAdminClient accessControlAdminClient,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate, UserInfoService userInfoService,
      NgUserService ngUserService, AuthSettingsManagerClient managerClient) {
    this.userGroupRepository = userGroupRepository;
    this.userClient = userClient;
    this.outboxService = outboxService;
    this.accessControlAdminClient = accessControlAdminClient;
    this.transactionTemplate = transactionTemplate;
    this.ngUserService = ngUserService;
    this.managerClient = managerClient;
  }

  @Override
  public UserGroup create(UserGroupDTO userGroupDTO) {
    try {
      UserGroup userGroup = toEntity(userGroupDTO);
      validate(userGroup);
      return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        UserGroup savedUserGroup = userGroupRepository.save(userGroup);
        outboxService.save(new UserGroupCreateEvent(userGroupDTO.getAccountIdentifier(), userGroupDTO));
        return savedUserGroup;
      }));
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("Try using different user group identifier, [%s] cannot be used", userGroupDTO.getIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Optional<UserGroup> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Criteria criteria = createUserGroupFetchCriteria(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return userGroupRepository.find(criteria);
  }

  @Override
  public UserGroup update(UserGroupDTO userGroupDTO) {
    UserGroup savedUserGroup = getOrThrow(userGroupDTO.getAccountIdentifier(), userGroupDTO.getOrgIdentifier(),
        userGroupDTO.getProjectIdentifier(), userGroupDTO.getIdentifier());
    UserGroup userGroup = toEntity(userGroupDTO);
    userGroup.setId(savedUserGroup.getId());
    userGroup.setVersion(savedUserGroup.getVersion());
    return updateInternal(userGroup, toDTO(savedUserGroup));
  }

  @Override
  public Page<UserGroup> list(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String searchTerm, Pageable pageable) {
    return userGroupRepository.findAll(
        createUserGroupFilterCriteria(accountIdentifier, orgIdentifier, projectIdentifier, searchTerm), pageable);
  }

  @Override
  public List<UserGroup> list(UserGroupFilterDTO userGroupFilterDTO) {
    validateFilter(userGroupFilterDTO);
    Criteria criteria = createScopeCriteria(userGroupFilterDTO.getAccountIdentifier(),
        userGroupFilterDTO.getOrgIdentifier(), userGroupFilterDTO.getProjectIdentifier());
    if (isNotEmpty(userGroupFilterDTO.getDatabaseIdFilter())) {
      criteria.and(UserGroupKeys.id).in(userGroupFilterDTO.getDatabaseIdFilter());
    } else if (isNotEmpty(userGroupFilterDTO.getIdentifierFilter())) {
      criteria.and(UserGroupKeys.identifier).in(userGroupFilterDTO.getIdentifierFilter());
    }
    if (isNotEmpty(userGroupFilterDTO.getUserIdentifierFilter())) {
      criteria.and(UserGroupKeys.users).in(userGroupFilterDTO.getUserIdentifierFilter());
    }
    return userGroupRepository.findAll(criteria, Pageable.unpaged()).getContent();
  }

  public PageResponse<UserMetadataDTO> listUsersInUserGroup(
      Scope scope, String userGroupIdentifier, UserFilter userFilter, PageRequest pageRequest) {
    Optional<UserGroup> userGroupOptional =
        get(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier(), userGroupIdentifier);
    if (!userGroupOptional.isPresent()) {
      return PageResponse.getEmptyPageResponse(pageRequest);
    }
    Set<String> userGroupMemberIds = new HashSet<>(userGroupOptional.get().getUsers());
    if (isEmpty(userFilter.getIdentifiers())) {
      userFilter.setIdentifiers(userGroupMemberIds);
    } else {
      userFilter.setIdentifiers(Sets.intersection(userFilter.getIdentifiers(), userGroupMemberIds));
    }
    return ngUserService.listUsers(scope, pageRequest, userFilter);
  }

  @Override
  public UserGroup delete(Scope scope, String identifier) {
    Criteria criteria = createUserGroupFetchCriteria(
        scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier(), identifier);
    RoleAssignmentFilterDTO roleAssignmentFilterDTO =
        RoleAssignmentFilterDTO.builder()
            .principalFilter(Collections.singleton(
                PrincipalDTO.builder().type(PrincipalType.USER_GROUP).identifier(identifier).build()))
            .build();
    PageResponse<RoleAssignmentResponseDTO> pageResponse =
        NGRestUtils.getResponse(accessControlAdminClient.getFilteredRoleAssignments(scope.getAccountIdentifier(),
            scope.getOrgIdentifier(), scope.getProjectIdentifier(), 0, 10, roleAssignmentFilterDTO));
    if (pageResponse.getTotalItems() > 0) {
      throw new InvalidRequestException(String.format(
          "There exists %s role assignments with this user group. Please delete them first and then try again",
          pageResponse.getTotalItems()));
    }
    validateAtleastOneAdminExistIfUserGroupRemoved(scope, identifier);
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      UserGroup userGroup = userGroupRepository.delete(criteria);
      outboxService.save(new UserGroupDeleteEvent(userGroup.getAccountIdentifier(), toDTO(userGroup)));
      return userGroup;
    }));
  }

  @Override
  public boolean checkMember(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String userGroupIdentifier, String userIdentifier) {
    UserGroup existingUserGroup = getOrThrow(accountIdentifier, orgIdentifier, projectIdentifier, userGroupIdentifier);
    return existingUserGroup.getUsers().contains(userIdentifier);
  }

  @Override
  public UserGroup addMember(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String userGroupIdentifier, String userIdentifier) {
    UserGroup existingUserGroup = getOrThrow(accountIdentifier, orgIdentifier, projectIdentifier, userGroupIdentifier);
    UserGroupDTO oldUserGroup = (UserGroupDTO) NGObjectMapperHelper.clone(toDTO(existingUserGroup));

    if (existingUserGroup.getUsers().stream().noneMatch(userIdentifier::equals)) {
      existingUserGroup.getUsers().add(userIdentifier);
    }
    return updateInternal(existingUserGroup, oldUserGroup);
  }

  @Override
  public void addUserToUserGroups(String accountIdentifier, UserInfo userInfo, List<UserGroup> userGroups) {
    if (isEmpty(userGroups)) {
      return;
    }

    for (UserGroup userGroup : userGroups) {
      if (!checkMember(accountIdentifier, null, null, userGroup.getIdentifier(), userInfo.getUuid())) {
        addMember(accountIdentifier, null, null, userGroup.getIdentifier(), userInfo.getUuid());
      }
    }
  }

  @Override
  public UserGroup removeMember(Scope scope, String userGroupIdentifier, String userIdentifier) {
    UserGroup existingUserGroup = getOrThrow(
        scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier(), userGroupIdentifier);
    UserGroupDTO oldUserGroup = (UserGroupDTO) NGObjectMapperHelper.clone(toDTO(existingUserGroup));
    validateAtleastOneAdminExistIfUserRemoved(scope, userGroupIdentifier, userIdentifier);
    existingUserGroup.getUsers().remove(userIdentifier);
    return updateInternal(existingUserGroup, oldUserGroup);
  }

  private void validateAtleastOneAdminExistIfUserRemoved(
      Scope scope, String userGroupIdentifier, String userIdentifier) {
    if (!ScopeUtils.isAccountScope(scope)) {
      return;
    }
    List<PrincipalDTO> admins = getAdmins(Scope.builder().accountIdentifier(scope.getAccountIdentifier()).build());
    boolean doesOtherAdminUsersExist = admins.stream()
                                           .filter(admin -> admin.getType().equals(USER))
                                           .map(PrincipalDTO::getIdentifier)
                                           .findFirst()
                                           .isPresent();
    if (doesOtherAdminUsersExist) {
      return;
    }
    List<String> adminUserGroupIds = admins.stream()
                                         .filter(admin -> admin.getType().equals(USER_GROUP))
                                         .map(PrincipalDTO::getIdentifier)
                                         .distinct()
                                         .collect(toList());
    List<UserGroup> adminUserGroups =
        list(UserGroupFilterDTO.builder().identifierFilter(new HashSet<>(adminUserGroupIds)).build());
    doesOtherAdminUsersExist = adminUserGroups.stream().anyMatch(userGroup -> {
      if (userGroup.getIdentifier().equals(userGroupIdentifier)) {
        return userGroup.getUsers().size() > (userGroup.getUsers().contains(userIdentifier) ? 1 : 0);
      } else {
        return !userGroup.getUsers().isEmpty();
      }
    });
    if (doesOtherAdminUsersExist) {
      return;
    }
    throw new InvalidRequestException(String.format(
        "%s is the last account admin for %s. Can't remove it", userIdentifier, scope.getAccountIdentifier()));
  }

  private void validateAtleastOneAdminExistIfUserGroupRemoved(Scope scope, String userGroupIdentifier) {
    if (!ScopeUtils.isAccountScope(scope)) {
      return;
    }
    List<PrincipalDTO> admins = getAdmins(Scope.builder().accountIdentifier(scope.getAccountIdentifier()).build());
    boolean doesOtherAdminUsersExist = admins.stream()
                                           .filter(admin -> admin.getType().equals(USER))
                                           .map(PrincipalDTO::getIdentifier)
                                           .findFirst()
                                           .isPresent();
    if (doesOtherAdminUsersExist) {
      return;
    }
    boolean doesOtherAdminUserGroupsExist =
        admins.stream()
            .filter(admin -> admin.getType().equals(USER_GROUP) && !admin.getIdentifier().equals(userGroupIdentifier))
            .map(PrincipalDTO::getIdentifier)
            .findFirst()
            .isPresent();
    if (doesOtherAdminUserGroupsExist) {
      return;
    }
    throw new InvalidRequestException(String.format("%s has the last account admins for account %s. Can't remove it",
        userGroupIdentifier, scope.getAccountIdentifier()));
  }

  @NotNull
  private List<PrincipalDTO> getAdmins(Scope scope) {
    PageResponse<RoleAssignmentResponseDTO> response =
        NGRestUtils.getResponse(accessControlAdminClient.getFilteredRoleAssignments(scope.getAccountIdentifier(),
            scope.getOrgIdentifier(), scope.getProjectIdentifier(), 0, 100,
            RoleAssignmentFilterDTO.builder().roleFilter(Collections.singleton(ACCOUNT_ADMIN)).build()));
    return response.getContent()
        .stream()
        .map(dto -> dto.getRoleAssignment().getPrincipal())
        .distinct()
        .collect(toList());
  }

  @Override
  public void removeMemberAll(@NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier,
      @NotNull String userIdentifier) {
    Criteria criteria =
        createCriteriaByScopeAndUsersIn(accountIdentifier, orgIdentifier, projectIdentifier, userIdentifier);
    List<UserGroup> userGroups = userGroupRepository.findAll(criteria);
    Scope scope = Scope.builder()
                      .accountIdentifier(accountIdentifier)
                      .orgIdentifier(orgIdentifier)
                      .projectIdentifier(projectIdentifier)
                      .build();
    List<String> userGroupIdentifiers = userGroups.stream().map(UserGroup::getIdentifier).collect(Collectors.toList());
    userGroupIdentifiers.forEach(userGroupIdentifier -> removeMember(scope, userGroupIdentifier, userIdentifier));
  }

  private Criteria createCriteriaByScopeAndUsersIn(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, @NotNull String userIdentifier) {
    Criteria criteria = createScopeCriteria(accountIdentifier, orgIdentifier, projectIdentifier);
    criteria.and(UserGroupKeys.users).in(userIdentifier);
    return criteria;
  }

  private UserGroup getOrThrow(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Optional<UserGroup> userGroupOptional = get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (!userGroupOptional.isPresent()) {
      throw new InvalidArgumentsException("User Group in the given scope does not exist");
    }
    return userGroupOptional.get();
  }

  private UserGroup updateInternal(UserGroup newUserGroup, UserGroupDTO oldUserGroup) {
    validate(newUserGroup);
    try {
      return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        UserGroup updatedUserGroup = userGroupRepository.save(newUserGroup);
        outboxService.save(
            new UserGroupUpdateEvent(updatedUserGroup.getAccountIdentifier(), toDTO(updatedUserGroup), oldUserGroup));
        return updatedUserGroup;
      }));
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("Try using different user group identifier, [%s] cannot be used", newUserGroup.getIdentifier()),
          USER_SRE, ex);
    }
  }

  private void validate(UserGroup userGroup) {
    if (userGroup.getNotificationConfigs() != null) {
      validateNotificationSettings(userGroup.getNotificationConfigs());
    }
    if (userGroup.getUsers() != null) {
      validateUsers(userGroup.getUsers(), userGroup.getAccountIdentifier());
      validateScopeMembership(userGroup);
    }
  }

  private void validateScopeMembership(UserGroup userGroup) {
    Set<String> filteredUserIds = ngUserService.filterUsersWithScopeMembership(userGroup.getUsers(),
        userGroup.getAccountIdentifier(), userGroup.getOrgIdentifier(), userGroup.getProjectIdentifier());
    Sets.SetView<String> invalidUserIds = Sets.difference(new HashSet<>(userGroup.getUsers()), filteredUserIds);
    if (isNotEmpty(invalidUserIds)) {
      throw new InvalidArgumentsException(getInvalidUserMessage(invalidUserIds));
    }
  }

  private void validateFilter(UserGroupFilterDTO filter) {
    if (isNotEmpty(filter.getIdentifierFilter()) && isNotEmpty(filter.getDatabaseIdFilter())) {
      throw new InvalidArgumentsException("Both the database id filter and identifier filter cannot be provided");
    }
  }

  private void validateUsers(List<String> usersIds, String accountId) {
    Failsafe.with(retryPolicy).run(() -> {
      Set<String> returnedUsersIds =
          RestClientUtils.getResponse(userClient.listUsers(accountId, UserFilterNG.builder().userIds(usersIds).build()))
              .stream()
              .map(UserInfo::getUuid)
              .collect(Collectors.toSet());
      Set<String> invalidUserIds = Sets.difference(new HashSet<>(usersIds), returnedUsersIds);
      if (!invalidUserIds.isEmpty()) {
        throw new InvalidArgumentsException(getInvalidUserMessage(invalidUserIds));
      }
    });
    if (hasDuplicate(usersIds)) {
      throw new InvalidArgumentsException("Duplicate users provided");
    }
  }

  private static <T> boolean hasDuplicate(Iterable<T> elements) {
    Set<T> set = new HashSet<>();
    for (T element : elements) {
      if (!set.add(element)) {
        return true;
      }
    }
    return false;
  }

  private String getInvalidUserMessage(Set<String> invalidUserIds) {
    return String.format("The following user%s not valid: [%s]", invalidUserIds.size() > 1 ? "s are" : " is",
        String.join(", ", invalidUserIds));
  }

  private void validateNotificationSettings(List<NotificationSettingConfig> notificationSettingConfigs) {
    Set<NotificationChannelType> typeSet = new HashSet<>();
    for (NotificationSettingConfig config : notificationSettingConfigs) {
      if (typeSet.contains(config.getType())) {
        throw new InvalidArgumentsException(
            "Not allowed to create multiple notification setting of type: " + config.getType());
      }
      typeSet.add(config.getType());
    }
  }

  private Criteria createScopeCriteria(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Criteria criteria = new Criteria();
    criteria.and(UserGroupKeys.accountIdentifier).is(accountIdentifier);
    criteria.and(UserGroupKeys.orgIdentifier).is(orgIdentifier);
    criteria.and(UserGroupKeys.projectIdentifier).is(projectIdentifier);
    return criteria;
  }

  private Criteria createUserGroupFilterCriteria(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String searchTerm) {
    Criteria criteria = createScopeCriteria(accountIdentifier, orgIdentifier, projectIdentifier);
    if (isNotBlank(searchTerm)) {
      criteria.orOperator(Criteria.where(UserGroupKeys.name).regex(searchTerm, "i"),
          Criteria.where(UserGroupKeys.tags).regex(searchTerm, "i"));
    }
    return criteria;
  }

  private Criteria createUserGroupFetchCriteria(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Criteria criteria = createScopeCriteria(accountIdentifier, orgIdentifier, projectIdentifier);
    criteria.and(UserGroupKeys.identifier).is(identifier);
    return criteria;
  }

  private Criteria getUserGroupbySsoIdCriteria(String accountIdentifier, String ssoId) {
    Criteria criteria = createScopeCriteria(accountIdentifier, null, null);
    criteria.and(UserGroupKeys.isSsoLinked).is(true);
    criteria.and(UserGroupKeys.linkedSsoId).is(ssoId);
    return criteria;
  }

  @Override
  public List<UserGroup> getUserGroupsBySsoId(String accountIdentifier, String ssoId) {
    Criteria criteria = getUserGroupbySsoIdCriteria(accountIdentifier, ssoId);
    return userGroupRepository.findAll(criteria);
  }

  @Override
  public UserGroup linkToSsoGroup(@NotBlank String accountIdentifier, @NotBlank String userGroupIdentifier,
      @NotNull SSOType ssoType, @NotBlank String ssoId, @NotBlank String ssoGroupId, @NotBlank String ssoGroupName) {
    UserGroup existingUserGroup = getOrThrow(accountIdentifier, null, null, userGroupIdentifier);
    UserGroupDTO oldUserGroup = (UserGroupDTO) NGObjectMapperHelper.clone(toDTO(existingUserGroup));

    if (existingUserGroup.isSsoLinked()) {
      throw new InvalidRequestException("SSO Provider already linked to the group. Try unlinking first.");
    }

    SSOConfig ssoConfig = getResponse(managerClient.getAccountAccessManagementSettings(accountIdentifier));

    List<SSOSettings> ssoSettingsList = ssoConfig.getSsoSettings();
    SSOSettings ssoSettings = null;
    for (SSOSettings ssoSetting : ssoSettingsList) {
      if (ssoSetting.getUuid().equals(ssoId)) {
        ssoSettings = ssoSetting;
        break;
      }
    }

    if (null == ssoSettings) {
      throw new InvalidRequestException("Invalid ssoId");
    }
    existingUserGroup.setSsoLinked(TRUE);
    existingUserGroup.setLinkedSsoType(ssoType);
    existingUserGroup.setLinkedSsoId(ssoId);
    existingUserGroup.setLinkedSsoDisplayName(ssoSettings.getDisplayName());
    existingUserGroup.setSsoGroupId(ssoGroupId);
    existingUserGroup.setSsoGroupName(ssoGroupName);

    // auditing TBD
    //    auditServiceHelper.reportForAuditingUsingAccountId(accountId, group, updatedGroup, Event.Type.LINK_SSO);

    return updateInternal(existingUserGroup, oldUserGroup);
  }

  @Override
  public UserGroup unlinkSsoGroup(
      @NotBlank String accountIdentifier, @NotBlank String userGroupIdentifier, boolean retainMembers) {
    UserGroup existingUserGroup = getOrThrow(accountIdentifier, null, null, userGroupIdentifier);
    UserGroupDTO oldUserGroup = (UserGroupDTO) NGObjectMapperHelper.clone(toDTO(existingUserGroup));

    if (!existingUserGroup.isSsoLinked()) {
      throw new InvalidRequestException("Group is not linked to any SSO group.");
    }

    if (!retainMembers) {
      existingUserGroup.setUsers(emptyList());
      existingUserGroup = updateInternal(existingUserGroup, oldUserGroup);
    }

    existingUserGroup.setSsoGroupId(null);
    existingUserGroup.setSsoLinked(false);
    existingUserGroup.setSsoGroupName(null);
    existingUserGroup.setLinkedSsoId(null);
    existingUserGroup.setLinkedSsoType(null);
    existingUserGroup.setLinkedSsoDisplayName(null);

    //    auditServiceHelper.reportForAuditingUsingAccountId(accountId, null, group, Event.Type.UNLINK_SSO);
    //    log.info("Auditing unlink from SSO Group for groupId={}", group.getUuid());
    return updateInternal(existingUserGroup, oldUserGroup);
  }
}
