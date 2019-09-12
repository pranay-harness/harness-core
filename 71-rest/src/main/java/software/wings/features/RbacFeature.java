package software.wings.features;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.APPLICATION_CREATE_DELETE;
import static software.wings.security.PermissionAttribute.PermissionType.AUDIT_VIEWER;
import static software.wings.security.PermissionAttribute.PermissionType.TAG_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.TEMPLATE_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_READ;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.User;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.features.api.AbstractUsageLimitedFeature;
import software.wings.features.api.ComplianceByLimitingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class RbacFeature extends AbstractUsageLimitedFeature implements ComplianceByLimitingUsage {
  public static final String FEATURE_NAME = "RBAC";

  private final UserService userService;
  private final UserGroupService userGroupService;

  @Inject
  public RbacFeature(AccountService accountService, FeatureRestrictions featureRestrictions, UserService userService,
      UserGroupService userGroupService) {
    super(accountService, featureRestrictions);
    this.userService = userService;
    this.userGroupService = userGroupService;
  }

  @Override
  public int getMaxUsageAllowed(String accountType) {
    return (int) getRestrictions(accountType).getOrDefault("maxUserGroupsAllowed", Integer.MAX_VALUE);
  }

  @Override
  public int getUsage(String accountId) {
    return getUserGroupsCount(accountId);
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  @Override
  public boolean limitUsageForCompliance(
      String accountId, String targetAccountType, Map<String, Object> requiredInfoToLimitUsage) {
    if (isUsageCompliantWithRestrictions(accountId, targetAccountType)) {
      return true;
    }

    if (getMaxUsageAllowed(targetAccountType) == 1) {
      userGroupService.deleteNonAdminUserGroups(accountId);

      UserGroup adminUserGroup = userGroupService.getAdminUserGroup(accountId);

      assignAllPermissionsToUserGroup(adminUserGroup);
      assignAllUsersMembershipToUserGroup(adminUserGroup);
    } else {
      @SuppressWarnings("unchecked")
      List<String> userGroupsToRetain = (List<String>) requiredInfoToLimitUsage.get("userGroupsToRetain");
      if (!isEmpty(userGroupsToRetain)) {
        userGroupService.deleteUserGroupsByName(accountId, userGroupsToRetain);
      }
    }

    return isUsageCompliantWithRestrictions(accountId, targetAccountType);
  }

  private void assignAllUsersMembershipToUserGroup(UserGroup adminUserGroup) {
    String accountId = adminUserGroup.getAccountId();

    List<User> usersOfAccount = userService.getUsersOfAccount(accountId);
    usersOfAccount.forEach(user
        -> userService.updateUserGroupsOfUser(
            user.getUuid(), Collections.singletonList(adminUserGroup), accountId, false));
  }

  private void assignAllPermissionsToUserGroup(UserGroup userGroup) {
    AccountPermissions accountPermissions =
        AccountPermissions.builder()
            .permissions(Sets.newHashSet(USER_PERMISSION_MANAGEMENT, ACCOUNT_MANAGEMENT, APPLICATION_CREATE_DELETE,
                TEMPLATE_MANAGEMENT, USER_PERMISSION_READ, AUDIT_VIEWER, TAG_MANAGEMENT))
            .build();

    Set<AppPermission> appPermissions = Sets.newHashSet();
    AppPermission appPermission =
        AppPermission.builder()
            .actions(Sets.newHashSet(Action.CREATE, Action.READ, Action.UPDATE, Action.DELETE, Action.EXECUTE))
            .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
            .permissionType(PermissionType.ALL_APP_ENTITIES)
            .build();
    appPermissions.add(appPermission);

    userGroup.setAccountPermissions(accountPermissions);
    userGroup.setAppPermissions(appPermissions);

    userGroupService.updatePermissions(userGroup);
  }

  private int getUserGroupsCount(String accountId) {
    return userGroupService.list(accountId, aPageRequest().build(), false).getResponse().size();
  }
}
