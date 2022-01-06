/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.validation.Validator.notBlankCheck;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLExecutionInputsToResumePipelineQueryParams;
import software.wings.graphql.schema.type.execution.QLExecutionInputs;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class RuntimeExecutionInputsToResumePipelineDataFetcher
    extends AbstractObjectDataFetcher<QLExecutionInputs, QLExecutionInputsToResumePipelineQueryParams> {
  private static final String APPLICATION_DOES_NOT_EXIST_MSG = "Application does not exist";

  @Inject AppService appService;
  @Inject RuntimeInputExecutionInputsController runtimeInputExecutionInputsController;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  public QLExecutionInputs fetch(QLExecutionInputsToResumePipelineQueryParams parameters, String accountId) {
    validateAppBelongsToAccount(parameters, accountId);
    return runtimeInputExecutionInputsController.fetch(parameters, accountId);
  }

  private void validateAppBelongsToAccount(QLExecutionInputsToResumePipelineQueryParams params, String accountId) {
    String accountIdFromApp = appService.getAccountIdByAppId(params.getApplicationId());
    notBlankCheck("No account for the given application " + params.getApplicationId(), accountIdFromApp);
    if (!accountIdFromApp.equals(accountId)) {
      throw new InvalidRequestException(APPLICATION_DOES_NOT_EXIST_MSG, WingsException.USER);
    }
  }
}
