/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.userGroup;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

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

import com.google.inject.Inject;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(DX)
@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class UpdateUserGroupPermissionsDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateUserGroupPermissionsInput, QLUpdateUserGroupPermissionsPayload> {
  @Inject private UserGroupService userGroupService;
  @Inject private UserGroupPermissionValidator userGroupPermissionValidator;
  @Inject private UserGroupPermissionsController userGroupPermissionsController;

  @Inject
  public UpdateUserGroupPermissionsDataFetcher() {
    super(QLUpdateUserGroupPermissionsInput.class, QLUpdateUserGroupPermissionsPayload.class);
  }

  private UserGroup updateUserGroupPermissions(QLUpdateUserGroupPermissionsInput parameters, String accountId) {
    userGroupPermissionValidator.validatePermission(parameters.getPermissions(), accountId);
    AccountPermissions accountPermissions =
        userGroupPermissionsController.populateUserGroupAccountPermissionEntity(parameters.getPermissions());
    Set<AppPermission> appPermissions =
        userGroupPermissionsController.populateUserGroupAppPermissionEntity(parameters.getPermissions());
    log.info("Testing: Setting app permissions {} of user group {} for account {} from graphql", appPermissions,
        parameters.getUserGroupId(), accountId);
    log.info("Testing: Setting account permissions {} of user group {} for account {} from graphql", accountPermissions,
        parameters.getUserGroupId(), accountId);
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
    QLGroupPermissions permissions = userGroupPermissionsController.populateUserGroupPermissions(userGroup);
    return QLUpdateUserGroupPermissionsPayload.builder()
        .clientMutationId(parameters.getClientMutationId())
        .permissions(permissions)
        .build();
  }
}
