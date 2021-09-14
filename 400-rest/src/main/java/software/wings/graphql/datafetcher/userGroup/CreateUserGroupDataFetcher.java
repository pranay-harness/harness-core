/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.userGroup;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.userGroup.input.QLCreateUserGroupInput;
import software.wings.graphql.schema.mutation.userGroup.payload.QLCreateUserGroupPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserGroupService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(DX)
@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class CreateUserGroupDataFetcher
    extends BaseMutatorDataFetcher<QLCreateUserGroupInput, QLCreateUserGroupPayload> {
  @Inject UserGroupService userGroupService;
  @Inject UserGroupController userGroupController;

  @Inject
  public CreateUserGroupDataFetcher(UserGroupService userGroupService) {
    super(QLCreateUserGroupInput.class, QLCreateUserGroupPayload.class);
    this.userGroupService = userGroupService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT)
  protected QLCreateUserGroupPayload mutateAndFetch(QLCreateUserGroupInput parameter, MutationContext mutationContext) {
    if (userGroupService.getByName(mutationContext.getAccountId(), parameter.getName()) != null) {
      throw new InvalidRequestException(
          String.format("A user group already exists with the name %s", parameter.getName()));
    }
    UserGroup userGroup = userGroupController.populateUserGroupEntity(parameter, mutationContext.getAccountId());
    userGroup.setAccountId(mutationContext.getAccountId());
    userGroupService.save(userGroup);
    log.info("Creating user group wth name {} in account {} from graphql", parameter.getName(),
        mutationContext.getAccountId());
    return userGroupController.populateCreateUserGroupPayload(userGroup, parameter.getClientMutationId());
  }
}
