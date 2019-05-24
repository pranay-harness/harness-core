package software.wings.service.impl;

import static software.wings.beans.FeatureViolation.Category.RESTRICTED_FEATURE_USAGE;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.APPLICATION_CREATE_DELETE;
import static software.wings.security.PermissionAttribute.PermissionType.TEMPLATE_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_READ;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.hazelcast.util.CollectionUtil;
import io.harness.rest.RestResponse;
import io.harness.scheduler.PersistentScheduler;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureViolation;
import software.wings.beans.LicenseInfo;
import software.wings.beans.User;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.licensing.LicenseService;
import software.wings.licensing.violations.FeatureViolationsService;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.scheduler.DelegateDeletionJobForCommunityAccount;
import software.wings.scheduler.ServiceInstanceUsageCheckerJob;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.security.SecretManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TransitionToCommunityAccountService {
  @Inject private LicenseService licenseService;
  @Inject private UserService userService;
  @Inject private UserGroupService userGroupService;
  @Inject private FeatureViolationsService featureViolationsService;
  @Inject private SecretManager secretManager;

  @Inject @Named("BackgroundJobScheduler") private transient PersistentScheduler jobScheduler;

  public boolean transition(String accountId, Map<String, List<String>> properties) {
    UserGroup adminUserGroup = userGroupService.getAdminUserGroup(accountId);
    AccountPermissions accountPermissions =
        AccountPermissions.builder()
            .permissions(Sets.newHashSet(USER_PERMISSION_MANAGEMENT, ACCOUNT_MANAGEMENT, APPLICATION_CREATE_DELETE,
                TEMPLATE_MANAGEMENT, USER_PERMISSION_READ))
            .build();

    Set<AppPermission> appPermissions = Sets.newHashSet();
    AppPermission appPermission =
        AppPermission.builder()
            .actions(Sets.newHashSet(Action.CREATE, Action.READ, Action.UPDATE, Action.DELETE, Action.EXECUTE))
            .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
            .permissionType(PermissionType.ALL_APP_ENTITIES)
            .build();
    appPermissions.add(appPermission);

    adminUserGroup.setAccountPermissions(accountPermissions);
    adminUserGroup.setAppPermissions(appPermissions);

    userGroupService.updatePermissions(adminUserGroup);

    // Assign all users membership of 'Administrator' user group.
    List<User> usersOfAccount = userService.getUsersOfAccount(accountId);
    usersOfAccount.forEach(user
        -> userService.updateUserGroupsOfUser(
            user.getUuid(), Collections.singletonList(adminUserGroup), accountId, false));

    RestResponse<Boolean> result = new RestResponse<>(licenseService.updateAccountLicense(accountId,
        LicenseInfo.builder().accountType(AccountType.COMMUNITY).accountStatus(AccountStatus.ACTIVE).build()));

    boolean licenseUpdated = result.getResource() != null && result.getResource();
    if (licenseUpdated) {
      if (properties.containsKey("delegatesToRetain")) {
        List<String> delegatesToRetain = properties.get("delegatesToRetain");
        if (CollectionUtil.isNotEmpty(delegatesToRetain)) {
          DelegateDeletionJobForCommunityAccount.addWithDelay(jobScheduler, accountId, delegatesToRetain.get(0), 30);
        }
      }
      ServiceInstanceUsageCheckerJob.add(jobScheduler, accountId);

      secretManager.clearDefaultFlagOfSecretManagers(accountId);
      secretManager.transitionAllSecretsToHarnessSecretManager(accountId);
    }

    return licenseUpdated;
  }

  public boolean canTransition(String accountId) {
    List<FeatureViolation> violationsToBeFixedBeforeAccountTransition =
        featureViolationsService.getViolations(accountId, AccountType.COMMUNITY)
            .stream()
            .filter(violation
                -> violation.getViolationCategory() != RESTRICTED_FEATURE_USAGE
                    && violation.getRestrictedFeature() != RestrictedFeature.DELEGATE
                    && violation.getRestrictedFeature() != RestrictedFeature.SECRET_MANAGEMENT)
            .collect(Collectors.toList());

    return violationsToBeFixedBeforeAccountTransition.isEmpty();
  }
}
