/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.userGroup;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.userGroup.input.QLAddAccountPermissionInput;
import software.wings.graphql.schema.mutation.userGroup.payload.QLAddAccountPermissionPayload;
import software.wings.graphql.schema.type.permissions.QLAccountPermissionType;
import software.wings.graphql.schema.type.usergroup.QLUserGroup;
import software.wings.graphql.schema.type.usergroup.QLUserGroup.QLUserGroupBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserGroupService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class AddAccountPermissionDataFetcher
    extends BaseMutatorDataFetcher<QLAddAccountPermissionInput, QLAddAccountPermissionPayload> {
  @Inject UserGroupService userGroupService;
  @Inject UserGroupController userGroupController;
  @Inject UserGroupPermissionsController userGroupPermissionsController;

  public AddAccountPermissionDataFetcher() {
    super(QLAddAccountPermissionInput.class, QLAddAccountPermissionPayload.class);
  }

  public QLAddAccountPermissionPayload populateAddAccountPermissionPayload(UserGroup userGroup, String requestId) {
    final QLUserGroupBuilder builder = QLUserGroup.builder();
    QLUserGroup qlUserGroup = userGroupController.populateUserGroupOutput(userGroup, builder).build();
    return QLAddAccountPermissionPayload.builder().clientMutationId(requestId).userGroup(qlUserGroup).build();
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT)
  protected QLAddAccountPermissionPayload mutateAndFetch(
      QLAddAccountPermissionInput input, MutationContext mutationContext) {
    String accountId = mutationContext.getAccountId();
    // Validate that the user group exists
    String userGroupId = input.getUserGroupId();
    UserGroup userGroup = userGroupController.validateAndGetUserGroup(accountId, userGroupId);

    QLAccountPermissionType accountPermission = input.getAccountPermission();
    UserGroup updatedUserGroup =
        userGroupPermissionsController.addAccountPermissionToUserGroupObject(userGroup, accountPermission);
    // Updating this new permissions
    UserGroup savedUserGroup = userGroupService.updatePermissions(updatedUserGroup);
    return populateAddAccountPermissionPayload(savedUserGroup, input.getClientMutationId());
  }
}
