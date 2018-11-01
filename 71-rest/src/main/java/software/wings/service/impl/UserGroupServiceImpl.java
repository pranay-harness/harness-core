package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.InvalidRequestException;
import io.harness.scheduler.PersistentScheduler;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SSOType;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.LdapGroupSyncJob;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.utils.Validator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by rishi
 */
@ValidateOnExecution
@Singleton
public class UserGroupServiceImpl implements UserGroupService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserService userService;
  @Inject private AccountService accountService;
  @Inject private AuthService authService;
  @Inject private SSOSettingService ssoSettingService;
  @Inject private AlertService alertService;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Override
  public UserGroup save(UserGroup userGroup) {
    Validator.notNullCheck("accountId", userGroup.getAccountId());
    UserGroup savedUserGroup = Validator.duplicateCheck(
        () -> wingsPersistence.saveAndGet(UserGroup.class, userGroup), "name", userGroup.getName());
    Account account = accountService.get(userGroup.getAccountId());
    Validator.notNullCheck("account", account);
    loadUsers(savedUserGroup, account);
    evictUserPermissionInfoCacheForUserGroup(savedUserGroup);
    return savedUserGroup;
  }

  @Override
  public PageResponse<UserGroup> list(String accountId, PageRequest<UserGroup> req, boolean loadUsers) {
    Validator.notNullCheck("accountId", accountId, USER);
    Account account = accountService.get(accountId);
    Validator.notNullCheck("account", account, USER);
    req.addFilter("accountId", Operator.EQ, accountId);
    PageResponse<UserGroup> res = wingsPersistence.query(UserGroup.class, req);
    List<UserGroup> userGroupList = res.getResponse();
    // Using a custom comparator since our mongo apis don't support alphabetical sorting with case insensitivity.
    // Currently, it only supports ASC and DSC.
    Collections.sort(userGroupList, new UserGroupComparator());
    if (loadUsers) {
      userGroupList.forEach(userGroup -> loadUsers(userGroup, account));
    }
    return res;
  }

  private static class UserGroupComparator implements Comparator<UserGroup>, Serializable {
    @Override
    public int compare(UserGroup lhs, UserGroup rhs) {
      return lhs.getName().compareToIgnoreCase(rhs.getName());
    }
  }

  @Override
  public UserGroup get(String accountId, String userGroupId) {
    return get(accountId, userGroupId, true);
  }

  @Override
  public UserGroup get(String accountId, String userGroupId, boolean loadUsers) {
    PageRequest<UserGroup> req = aPageRequest()
                                     .addFilter("accountId", Operator.EQ, accountId)
                                     .addFilter(ID_KEY, Operator.EQ, userGroupId)
                                     .build();
    UserGroup userGroup = wingsPersistence.get(UserGroup.class, req);
    if (loadUsers && userGroup != null) {
      Account account = accountService.get(accountId);
      loadUsers(userGroup, account);
    }

    return userGroup;
  }

  private void loadUsers(UserGroup userGroup, Account account) {
    if (userGroup.getMemberIds() != null) {
      PageRequest<User> req = aPageRequest()
                                  .addFilter(ID_KEY, Operator.IN, userGroup.getMemberIds().toArray())
                                  .addFilter("accounts", Operator.IN, account)
                                  .build();
      PageResponse<User> res = userService.list(req);
      userGroup.setMembers(res.getResponse());
    } else {
      userGroup.setMembers(new ArrayList<>());
    }
  }

  @Override
  public UserGroup updateOverview(UserGroup userGroup) {
    Validator.notNullCheck("name", userGroup.getName());
    UpdateOperations<UserGroup> operations =
        wingsPersistence.createUpdateOperations(UserGroup.class).set("name", userGroup.getName());
    setUnset(operations, "description", userGroup.getDescription());
    return update(userGroup, operations);
  }

  @Override
  public UserGroup updateMembers(UserGroup userGroup, boolean sendNotification) {
    Set<String> newMemberIds = Sets.newHashSet();
    if (isNotEmpty(userGroup.getMembers())) {
      newMemberIds = userGroup.getMembers()
                         .stream()
                         .filter(user -> StringUtils.isNotBlank(user.getUuid()))
                         .map(User::getUuid)
                         .collect(Collectors.toSet());
    }
    UserGroup existingUserGroup = get(userGroup.getAccountId(), userGroup.getUuid());
    Set<String> existingMemberIds = isEmpty(existingUserGroup.getMemberIds())
        ? Sets.newHashSet()
        : Sets.newHashSet(existingUserGroup.getMemberIds());

    UpdateOperations<UserGroup> operations = wingsPersistence.createUpdateOperations(UserGroup.class);
    setUnset(operations, "memberIds", newMemberIds);
    UserGroup updatedUserGroup = update(userGroup, operations);
    if (isNotEmpty(existingUserGroup.getMemberIds())) {
      newMemberIds.addAll(existingUserGroup.getMemberIds());
    }

    if (isNotEmpty(newMemberIds)) {
      evictUserPermissionInfoCacheForUserGroup(
          userGroup.getAccountId(), newMemberIds.stream().distinct().collect(toList()));
    }

    if (sendNotification) {
      // Send added role email for all newly added users to the group
      Set newlyAddedMemberIds = Sets.difference(newMemberIds, existingMemberIds);
      Account account = accountService.get(updatedUserGroup.getAccountId());
      updatedUserGroup.getMembers().forEach(member -> {
        if (newlyAddedMemberIds.contains(member.getUuid())) {
          userService.sendAddedRoleEmail(member, account);
        }
      });
    }
    return updatedUserGroup;
  }

  @Override
  public UserGroup removeMembers(UserGroup userGroup, Collection<User> members, boolean sendNotification) {
    if (isEmpty(members)) {
      return userGroup;
    }

    List<User> groupMembers = userGroup.getMembers();
    if (isEmpty(groupMembers)) {
      return userGroup;
    }

    members.forEach(groupMembers::remove);
    return updateMembers(userGroup, sendNotification);
  }

  @Override
  public UserGroup updatePermissions(UserGroup userGroup) {
    UpdateOperations<UserGroup> operations = wingsPersistence.createUpdateOperations(UserGroup.class);
    setUnset(operations, "appPermissions", userGroup.getAppPermissions());
    setUnset(operations, "accountPermissions", userGroup.getAccountPermissions());
    UserGroup updatedUserGroup = update(userGroup, operations);
    evictUserPermissionInfoCacheForUserGroup(updatedUserGroup);
    return updatedUserGroup;
  }

  @Override
  public boolean existsLinkedUserGroup(String ssoId) {
    return 0
        != wingsPersistence.getCount(
               UserGroup.class, aPageRequest().addFilter(UserGroup.LINKED_SSO_ID_KEY, Operator.EQ, ssoId).build());
  }

  private UserGroup update(UserGroup userGroup, UpdateOperations<UserGroup> operations) {
    Validator.notNullCheck("uuid", userGroup.getUuid());
    Validator.notNullCheck("accountId", userGroup.getAccountId());
    Query<UserGroup> query = wingsPersistence.createQuery(UserGroup.class)
                                 .filter(ID_KEY, userGroup.getUuid())
                                 .filter("accountId", userGroup.getAccountId());
    wingsPersistence.update(query, operations);
    return get(userGroup.getAccountId(), userGroup.getUuid());
  }

  @Override
  public boolean delete(String accountId, String userGroupId) {
    UserGroup userGroup = get(accountId, userGroupId, false);
    Validator.notNullCheck("userGroup", userGroup);
    Query<UserGroup> userGroupQuery = wingsPersistence.createQuery(UserGroup.class)
                                          .filter(UserGroup.ACCOUNT_ID_KEY, accountId)
                                          .filter(ID_KEY, userGroupId);
    boolean deleted = wingsPersistence.delete(userGroupQuery);
    if (deleted) {
      evictUserPermissionInfoCacheForUserGroup(userGroup);
    }
    return deleted;
  }

  @Override
  public UserGroup cloneUserGroup(
      final String accountId, final String uuid, final String newName, final String newDescription) {
    UserGroup existingGroup = get(accountId, uuid, true);
    Validator.notNullCheck("userGroup", existingGroup);
    Validator.unEqualCheck(existingGroup.getName(), newName);
    UserGroup newClonedGroup = existingGroup.cloneWithNewName(newName, newDescription);
    return save(newClonedGroup);
  }

  private void evictUserPermissionInfoCacheForUserGroup(UserGroup userGroup) {
    authService.evictAccountUserPermissionInfoCache(userGroup.getAccountId(), userGroup.getMemberIds());
  }

  private void evictUserPermissionInfoCacheForUserGroup(String accountId, List<String> memberIds) {
    authService.evictAccountUserPermissionInfoCache(accountId, memberIds);
  }

  @Override
  public List<UserGroup> getUserGroupsByAccountId(String accountId, User user) {
    PageRequest<UserGroup> pageRequest = aPageRequest()
                                             .addFilter("accountId", Operator.EQ, accountId)
                                             .addFilter("memberIds", Operator.HAS, user.getUuid())
                                             .build();
    PageResponse<UserGroup> pageResponse = list(accountId, pageRequest, true);
    return pageResponse.getResponse();
  }

  @Override
  public List<String> fetchUserGroupsMemberIds(String accountId, List<String> userGroupIds) {
    if (isEmpty(userGroupIds)) {
      return asList();
    }

    List<UserGroup> userGroupList;
    if (isNotBlank(accountId)) {
      userGroupList = wingsPersistence.createQuery(UserGroup.class)
                          .filter(UserGroup.ACCOUNT_ID_KEY, accountId)
                          .field(UserGroup.ID_KEY)
                          .in(userGroupIds)
                          .project(UserGroup.MEMBER_IDS_KEY, true)
                          .asList();
    } else {
      userGroupList = wingsPersistence.createQuery(UserGroup.class, excludeAuthority)
                          .field(UserGroup.ID_KEY)
                          .in(userGroupIds)
                          .project(UserGroup.MEMBER_IDS_KEY, true)
                          .asList();
    }

    return userGroupList.stream()
        .filter(userGroup -> !isEmpty(userGroup.getMemberIds()))
        .flatMap(userGroup -> userGroup.getMemberIds().stream())
        .collect(toList());
  }

  @Override
  public List<UserGroup> fetchUserGroupNamesFromIds(List<String> userGroupIds) {
    if (isEmpty(userGroupIds)) {
      return asList();
    }

    return wingsPersistence.createQuery(UserGroup.class, excludeAuthority)
        .field(UserGroup.ID_KEY)
        .in(userGroupIds)
        .project(UserGroup.NAME_KEY, true)
        .project(UserGroup.ID_KEY, true)
        .asList()
        .stream()
        .filter(userGroup -> !isEmpty(userGroup.getName()))
        .collect(toList());
  }

  @Override
  public boolean verifyUserAuthorizedToAcceptOrRejectApproval(String accountId, List<String> userGroupIds) {
    if (isEmpty(userGroupIds)) {
      return false;
    }

    User user = UserThreadLocal.get();
    List<String> userGroupMembers = fetchUserGroupsMemberIds(accountId, userGroupIds);

    return user.isEmailVerified() && isNotEmpty(userGroupMembers) && userGroupMembers.contains(user.getUuid());
  }

  @Override
  public UserGroup linkToSsoGroup(@NotBlank String accountId, @NotBlank String userGroupId, @NotNull SSOType ssoType,
      @NotBlank String ssoId, @NotBlank String ssoGroupId, @NotBlank String ssoGroupName) {
    UserGroup group = get(accountId, userGroupId, false);

    if (null == group) {
      throw new InvalidRequestException("Invalid UserGroup ID.");
    }
    if (group.isSsoLinked()) {
      throw new InvalidRequestException("SSO Provider already linked to the group. Try unlinking first.");
    }

    SSOSettings ssoSettings = ssoSettingService.getSsoSettings(ssoId);

    if (null == ssoSettings) {
      throw new InvalidRequestException("Invalid ssoId");
    }

    group.setSsoLinked(true);
    group.setLinkedSsoType(ssoType);
    group.setLinkedSsoId(ssoId);
    group.setLinkedSsoDisplayName(ssoSettings.getDisplayName());
    group.setSsoGroupId(ssoGroupId);
    group.setSsoGroupName(ssoGroupName);
    UserGroup savedGroup = save(group);

    LdapGroupSyncJob.add(jobScheduler, accountId, ssoId);

    return savedGroup;
  }

  @Override
  public UserGroup unlinkSsoGroup(@NotBlank String accountId, @NotBlank String userGroupId, boolean retainMembers) {
    UserGroup group = get(accountId, userGroupId, false);

    if (null == group) {
      throw new InvalidRequestException("Invalid UserGroup ID.");
    }
    if (!group.isSsoLinked()) {
      throw new InvalidRequestException("Group is not linked to any SSO group.");
    }

    if (!retainMembers) {
      group.setMembers(Collections.emptyList());
      group = updateMembers(group, false);
    }

    group.setSsoLinked(false);
    group.setLinkedSsoType(null);
    group.setLinkedSsoId(null);
    group.setSsoGroupId(null);

    return save(group);
  }

  @Override
  public List<UserGroup> getUserGroupsBySsoId(String accountId, String ssoId) {
    PageRequest<UserGroup> pageRequest = aPageRequest()
                                             .addFilter("accountId", Operator.EQ, accountId)
                                             .addFilter("isSsoLinked", Operator.EQ, true)
                                             .addFilter("linkedSsoId", Operator.EQ, ssoId)
                                             .build();
    PageResponse<UserGroup> pageResponse = list(accountId, pageRequest, true);
    return pageResponse.getResponse();
  }
}
