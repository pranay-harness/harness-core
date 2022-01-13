/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.user;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.type.user.QLDeleteUserInput;
import software.wings.graphql.schema.type.user.QLDeleteUserPayload;
import software.wings.graphql.schema.type.user.QLDeleteUserPayload.QLDeleteUserPayloadBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserService;

import com.google.inject.Inject;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class DeleteUserDataFetcher extends BaseMutatorDataFetcher<QLDeleteUserInput, QLDeleteUserPayload> {
  @Inject private UserService userService;

  @Inject
  public DeleteUserDataFetcher(UserService userService) {
    super(QLDeleteUserInput.class, QLDeleteUserPayload.class);
    this.userService = userService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT)
  protected QLDeleteUserPayload mutateAndFetch(QLDeleteUserInput qlDeleteUserInput, MutationContext mutationContext) {
    QLDeleteUserPayloadBuilder qlDeleteUserPayloadBuilder =
        QLDeleteUserPayload.builder().clientMutationId(qlDeleteUserInput.getClientMutationId());
    final String userId = qlDeleteUserInput.getId();
    try {
      userService.delete(mutationContext.getAccountId(), userId);
      return qlDeleteUserPayloadBuilder.build();
    } catch (Exception ex) {
      throw new InvalidRequestException("User not found");
    }
  }
}
