package software.wings.graphql.datafetcher.userGroup;

import static software.wings.graphql.datafetcher.userGroup.UserGroupPermissionsController.populateUserGroupAccountPermissionEntity;
import static software.wings.graphql.datafetcher.userGroup.UserGroupPermissionsController.populateUserGroupAppPermissionEntity;
import static software.wings.graphql.datafetcher.userGroup.UserGroupPermissionsController.populateUserGroupPermissions;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.userGroup.QLSetUserGroupPermissionsParameters;
import software.wings.graphql.schema.type.permissions.QLGroupPermissions;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserGroupService;

import java.util.Set;

@Slf4j
public class UserGroupUpdatePermissionsDataFetcher
    extends BaseMutatorDataFetcher<QLSetUserGroupPermissionsParameters, QLGroupPermissions> {
  @Inject private UserGroupService userGroupService;
  @Inject private UserGroupPermissionValidator userGroupPermissionValidator;

  @Inject
  public UserGroupUpdatePermissionsDataFetcher() {
    super(QLSetUserGroupPermissionsParameters.class, QLGroupPermissions.class);
  }

  private UserGroup updateUserGroupPermissions(QLSetUserGroupPermissionsParameters parameters, String accountId) {
    userGroupPermissionValidator.validatePermission(parameters.getPermissions());
    AccountPermissions accountPermissions = populateUserGroupAccountPermissionEntity(parameters.getPermissions());
    Set<AppPermission> appPermissions = populateUserGroupAppPermissionEntity(parameters.getPermissions());
    return userGroupService.setUserGroupPermissions(
        accountId, parameters.getUserGroupId(), accountPermissions, appPermissions);
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT)
  protected QLGroupPermissions mutateAndFetch(
      QLSetUserGroupPermissionsParameters parameters, MutationContext mutationContext) {
    if (userGroupService.get(mutationContext.getAccountId(), parameters.getUserGroupId()) == null) {
      throw new InvalidRequestException("No userGroup Exists with id " + parameters.getUserGroupId());
    }
    final UserGroup userGroup = updateUserGroupPermissions(parameters, mutationContext.getAccountId());
    return populateUserGroupPermissions(userGroup);
  }
}
