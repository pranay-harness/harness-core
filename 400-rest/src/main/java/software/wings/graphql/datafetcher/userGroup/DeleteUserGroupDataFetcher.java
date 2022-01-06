/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.userGroup;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.userGroup.input.QLDeleteUserGroupInput;
import software.wings.graphql.schema.mutation.userGroup.payload.QLDeleteUserGroupPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserGroupService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class DeleteUserGroupDataFetcher
    extends BaseMutatorDataFetcher<QLDeleteUserGroupInput, QLDeleteUserGroupPayload> {
  @Inject private UserGroupService userGroupService;

  @Inject
  public DeleteUserGroupDataFetcher(UserGroupService userGroupService) {
    super(QLDeleteUserGroupInput.class, QLDeleteUserGroupPayload.class);

    this.userGroupService = userGroupService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT)
  protected QLDeleteUserGroupPayload mutateAndFetch(QLDeleteUserGroupInput parameter, MutationContext mutationContext) {
    String userGroupId = parameter.getUserGroupId();
    UserGroup userGroup = userGroupService.get(mutationContext.getAccountId(), userGroupId);
    if (userGroup == null) {
      throw new InvalidRequestException(
          String.format("No user group exists with the id %s", parameter.getUserGroupId()));
    }
    userGroupService.delete(mutationContext.getAccountId(), userGroupId, false);
    return QLDeleteUserGroupPayload.builder().clientMutationId(parameter.getClientMutationId()).build();
  }
}
