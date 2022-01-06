/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.application;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.input.QLUpdateApplicationGitSyncConfigInput;
import software.wings.graphql.schema.mutation.application.input.QLUpdateApplicationGitSyncConfigInput.QLUpdateApplicationGitSyncConfigInputKeys;
import software.wings.graphql.schema.mutation.application.payload.QLRemoveApplicationGitSyncConfigPayload;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.yaml.YamlGitService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class RemoveApplicationGitSyncConfigDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateApplicationGitSyncConfigInput, QLRemoveApplicationGitSyncConfigPayload> {
  private final AppService appService;
  private final YamlGitService yamlGitService;
  private final AuthService authService;

  @Inject
  public RemoveApplicationGitSyncConfigDataFetcher(
      AppService appService, YamlGitService yamlGitService, AuthService authService) {
    super(QLUpdateApplicationGitSyncConfigInput.class, QLRemoveApplicationGitSyncConfigPayload.class);
    this.appService = appService;
    this.yamlGitService = yamlGitService;
    this.authService = authService;
  }

  @Override
  @AuthRule(permissionType = PermissionType.MANAGE_CONFIG_AS_CODE)
  protected QLRemoveApplicationGitSyncConfigPayload mutateAndFetch(
      QLUpdateApplicationGitSyncConfigInput input, MutationContext mutationContext) {
    validate(input);
    final User user = UserThreadLocal.get();
    if (user != null) {
      authService.authorizeAppAccess(
          mutationContext.getAccountId(), input.getApplicationId(), user, PermissionAttribute.Action.UPDATE);
    }
    final Application application = getApplication(input.getApplicationId());
    yamlGitService.delete(mutationContext.getAccountId(), application.getUuid(), EntityType.APPLICATION);

    return QLRemoveApplicationGitSyncConfigPayload.builder()
        .clientMutationId(input.getClientMutationId())
        .application(ApplicationController.populateQLApplication(application, QLApplication.builder()).build())
        .build();
  }

  private void validate(QLUpdateApplicationGitSyncConfigInput input) {
    utils.ensureNotBlankField(input.getApplicationId(), QLUpdateApplicationGitSyncConfigInputKeys.applicationId);
  }

  private Application getApplication(String applicationId) {
    return appService.get(applicationId);
  }
}
