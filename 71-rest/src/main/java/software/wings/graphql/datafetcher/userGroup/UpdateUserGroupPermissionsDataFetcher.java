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
import software.wings.graphql.schema.mutation.userGroup.input.QLUpdateUserGroupPermissionsInput;
import software.wings.graphql.schema.mutation.userGroup.payload.QLUpdateUserGroupPermissionsPayload;
import software.wings.graphql.schema.type.permissions.QLGroupPermissions;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserGroupService;

import java.util.Set;

@Slf4j
public class UpdateUserGroupPermissionsDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateUserGroupPermissionsInput, QLUpdateUserGroupPermissionsPayload> {
  @Inject private UserGroupService userGroupService;
  @Inject private UserGroupPermissionValidator userGroupPermissionValidator;

  @Inject
  public UpdateUserGroupPermissionsDataFetcher() {
    super(QLUpdateUserGroupPermissionsInput.class, QLUpdateUserGroupPermissionsPayload.class);
  }

  private UserGroup updateUserGroupPermissions(QLUpdateUserGroupPermissionsInput parameters, String accountId) {
    userGroupPermissionValidator.validatePermission(parameters.getPermissions(), accountId);
    AccountPermissions accountPermissions = populateUserGroupAccountPermissionEntity(parameters.getPermissions());
    Set<AppPermission> appPermissions = populateUserGroupAppPermissionEntity(parameters.getPermissions());
    return userGroupService.setUserGroupPermissions(
        accountId, parameters.getUserGroupId(), accountPermissions, appPermissions);
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT)
  protected QLUpdateUserGroupPermissionsPayload mutateAndFetch(
      QLUpdateUserGroupPermissionsInput parameters, MutationContext mutationContext) {
    if (userGroupService.get(mutationContext.getAccountId(), parameters.getUserGroupId()) == null) {
      throw new InvalidRequestException("No userGroup Exists with id " + parameters.getUserGroupId());
    }
    final UserGroup userGroup = updateUserGroupPermissions(parameters, mutationContext.getAccountId());
    QLGroupPermissions permissions = populateUserGroupPermissions(userGroup);
    return QLUpdateUserGroupPermissionsPayload.builder()
        .requestId(parameters.getRequestId())
        .permissions(permissions)
        .build();
  }
}
