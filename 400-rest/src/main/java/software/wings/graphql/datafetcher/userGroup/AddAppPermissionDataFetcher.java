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

import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.userGroup.input.QLAddAppPermissionInput;
import software.wings.graphql.schema.mutation.userGroup.payload.QLAddAppPermissionPayload;
import software.wings.graphql.schema.type.permissions.QLAppPermission;
import software.wings.graphql.schema.type.usergroup.QLUserGroup;
import software.wings.graphql.schema.type.usergroup.QLUserGroup.QLUserGroupBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserGroupService;

import com.google.inject.Inject;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(DX)
@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class AddAppPermissionDataFetcher
    extends BaseMutatorDataFetcher<QLAddAppPermissionInput, QLAddAppPermissionPayload> {
  @Inject UserGroupService userGroupService;
  @Inject UserGroupController userGroupController;
  @Inject UserGroupPermissionValidator userGroupPermissionValidator;
  @Inject UserGroupPermissionsController userGroupPermissionsController;

  public AddAppPermissionDataFetcher() {
    super(QLAddAppPermissionInput.class, QLAddAppPermissionPayload.class);
  }

  public QLAddAppPermissionPayload populateAddAppPermissionPayload(UserGroup userGroup, String requestId) {
    final QLUserGroupBuilder builder = QLUserGroup.builder();
    QLUserGroup qlUserGroup = userGroupController.populateUserGroupOutput(userGroup, builder).build();
    return QLAddAppPermissionPayload.builder().clientMutationId(requestId).userGroup(qlUserGroup).build();
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT)
  protected QLAddAppPermissionPayload mutateAndFetch(QLAddAppPermissionInput input, MutationContext mutationContext) {
    String accountId = mutationContext.getAccountId();
    // Validate that the user group exists
    String userGroupId = input.getUserGroupId();
    UserGroup userGroup = userGroupController.validateAndGetUserGroup(accountId, userGroupId);

    QLAppPermission appPermission = input.getAppPermission();
    userGroupPermissionValidator.validateAppPermission(accountId, Collections.singletonList(appPermission));
    UserGroup updatedUserGroup =
        userGroupPermissionsController.addAppPermissionToUserGroupObject(userGroup, appPermission);
    log.info("Testing: Setting app permissions {} of user group {} for account {} from graphql",
        updatedUserGroup.getAppPermissions(), userGroup.getUuid(), userGroup.getAccountId());
    // Updating this new permissions
    UserGroup savedUserGroup = userGroupService.updatePermissions(updatedUserGroup);
    return populateAddAppPermissionPayload(savedUserGroup, input.getClientMutationId());
  }
}
