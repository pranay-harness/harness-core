/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.application;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_APPLICATIONS;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.input.QLUpdateApplicationInput;
import software.wings.graphql.schema.mutation.application.payload.QLDeleteApplicationPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class DeleteApplicationDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateApplicationInput, QLDeleteApplicationPayload> {
  private AppService appService;

  @Inject
  public DeleteApplicationDataFetcher(AppService appService) {
    super(QLUpdateApplicationInput.class, QLDeleteApplicationPayload.class);
    this.appService = appService;
  }

  @Override
  @AuthRule(permissionType = MANAGE_APPLICATIONS, action = PermissionAttribute.Action.DELETE)
  protected QLDeleteApplicationPayload mutateAndFetch(
      QLUpdateApplicationInput parameter, MutationContext mutationContext) {
    appService.delete(parameter.getApplicationId());
    return QLDeleteApplicationPayload.builder().clientMutationId(parameter.getClientMutationId()).build();
  }
}
