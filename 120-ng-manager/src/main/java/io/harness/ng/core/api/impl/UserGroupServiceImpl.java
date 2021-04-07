package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.core.utils.UserGroupMapper.toEntity;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.dto.UserGroupDTO;
import io.harness.ng.core.dto.UserGroupFilterDTO;
import io.harness.ng.core.entities.NotificationSettingConfig;
import io.harness.ng.core.entities.UserGroup;
import io.harness.ng.core.entities.UserGroup.UserGroupKeys;
import io.harness.ng.core.user.UserInfo;
import io.harness.notification.NotificationChannelType;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.repositories.ng.core.spring.UserGroupRepository;
import io.harness.user.remote.UserClient;
import io.harness.user.remote.UserSearchFilter;
import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.StringValue;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
@Singleton
@Slf4j
public class UserGroupServiceImpl implements UserGroupService {
  private final UserGroupRepository userGroupRepository;
  private final UserClient userClient;
  private final Producer eventProducer;
  private final AccessControlAdminClient accessControlAdminClient;

  private static final RetryPolicy<Object> retryPolicy =
      RetryUtils.getRetryPolicy("Could not find the user with the given identifier on attempt %s",
          "Could not find the user with the given identifier", Lists.newArrayList(InvalidRequestException.class),
          Duration.ofSeconds(5), 3, log);

  @Inject
  public UserGroupServiceImpl(UserGroupRepository userGroupRepository, UserClient userClient,
      @Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer,
      AccessControlAdminClient accessControlAdminClient) {
    this.userGroupRepository = userGroupRepository;
    this.userClient = userClient;
    this.eventProducer = eventProducer;
    this.accessControlAdminClient = accessControlAdminClient;
  }

  @Override
  public UserGroup create(UserGroupDTO userGroupDTO) {
    try {
      UserGroup userGroup = toEntity(userGroupDTO);
      validate(userGroup);
      UserGroup savedUserGroup = userGroupRepository.save(userGroup);
      publishEvent(savedUserGroup, EventsFrameworkMetadataConstants.CREATE_ACTION);
      return savedUserGroup;
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
    return updateInternal(userGroup);
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

  @Override
  public UserGroup delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Criteria criteria = createUserGroupFetchCriteria(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    RoleAssignmentFilterDTO roleAssignmentFilterDTO =
        RoleAssignmentFilterDTO.builder()
            .principalFilter(Collections.singleton(
                PrincipalDTO.builder().type(PrincipalType.USER_GROUP).identifier(identifier).build()))
            .build();
    PageResponse<RoleAssignmentResponseDTO> pageResponse =
        NGRestUtils.getResponse(accessControlAdminClient.getFilteredRoleAssignments(
            accountIdentifier, orgIdentifier, projectIdentifier, 0, 10, roleAssignmentFilterDTO));
    if (pageResponse.getTotalItems() > 0) {
      throw new InvalidRequestException(String.format(
          "There exists %s role assignments with this user group. Please delete them first and then try again",
          pageResponse.getTotalItems()));
    }
    UserGroup userGroup = userGroupRepository.delete(criteria);
    publishEvent(userGroup, EventsFrameworkMetadataConstants.DELETE_ACTION);
    return userGroup;
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
    existingUserGroup.getUsers().add(userIdentifier);
    return updateInternal(existingUserGroup);
  }

  @Override
  public UserGroup removeMember(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String userGroupIdentifier, String userIdentifier) {
    UserGroup existingUserGroup = getOrThrow(accountIdentifier, orgIdentifier, projectIdentifier, userGroupIdentifier);
    existingUserGroup.getUsers().remove(userIdentifier);
    return updateInternal(existingUserGroup);
  }

  private UserGroup getOrThrow(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Optional<UserGroup> userGroupOptional = get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (!userGroupOptional.isPresent()) {
      throw new InvalidArgumentsException("User Group in the given scope does not exist");
    }
    return userGroupOptional.get();
  }

  private UserGroup updateInternal(UserGroup userGroupUpdate) {
    validate(userGroupUpdate);
    try {
      UserGroup updatedUserGroup = userGroupRepository.save(userGroupUpdate);
      publishEvent(updatedUserGroup, EventsFrameworkMetadataConstants.UPDATE_ACTION);
      return updatedUserGroup;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(String.format("Try using different user group identifier, [%s] cannot be used",
                                            userGroupUpdate.getIdentifier()),
          USER_SRE, ex);
    }
  }

  private void validate(UserGroup userGroup) {
    if (userGroup.getNotificationConfigs() != null) {
      validateNotificationSettings(userGroup.getNotificationConfigs());
    }
    if (userGroup.getUsers() != null) {
      validateUsers(userGroup.getUsers(), userGroup.getAccountIdentifier());
    }
  }

  private void publishEvent(UserGroup userGroup, String action) {
    try {
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", userGroup.getAccountIdentifier(),
                  EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.USER_GROUP,
                  EventsFrameworkMetadataConstants.ACTION, action))
              .setData(getUserGroupPayload(userGroup))
              .build());
    } catch (ProducerShutdownException e) {
      log.error("Failed to send event to events framework for user group identifier {}", userGroup.getIdentifier(), e);
    }
  }

  private ByteString getUserGroupPayload(UserGroup userGroup) {
    EntityChangeDTO.Builder builder = EntityChangeDTO.newBuilder()
                                          .setAccountIdentifier(StringValue.of(userGroup.getAccountIdentifier()))
                                          .setIdentifier(StringValue.of(userGroup.getIdentifier()));
    if (isNotEmpty(userGroup.getOrgIdentifier())) {
      builder.setOrgIdentifier(StringValue.of(userGroup.getOrgIdentifier()));
    }
    if (isNotEmpty(userGroup.getProjectIdentifier())) {
      builder.setProjectIdentifier(StringValue.of(userGroup.getProjectIdentifier()));
    }
    return builder.build().toByteString();
  }

  private void validateFilter(UserGroupFilterDTO filter) {
    if (isNotEmpty(filter.getIdentifierFilter()) && isNotEmpty(filter.getDatabaseIdFilter())) {
      throw new InvalidArgumentsException("Both the database id filter and identifier filter cannot be provided");
    }
  }

  private void validateUsers(Set<String> usersIds, String accountId) {
    Failsafe.with(retryPolicy).run(() -> {
      Set<String> returnedUsersIds =
          RestClientUtils
              .getResponse(userClient.listUsers(
                  UserSearchFilter.builder().userIds(new ArrayList<>(usersIds)).build(), accountId))
              .stream()
              .map(UserInfo::getUuid)
              .collect(Collectors.toSet());
      Set<String> invalidUserIds = Sets.difference(usersIds, returnedUsersIds);
      if (!invalidUserIds.isEmpty()) {
        throw new InvalidArgumentsException(
            String.format("The following users are not valid %s", String.join(",", invalidUserIds)));
      }
    });
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
}
