package software.wings.graphql.datafetcher.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
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
    if (!accountIdFromApp.equals(accountId)) {
      throw new InvalidRequestException(APPLICATION_DOES_NOT_EXIST_MSG, WingsException.USER);
    }
  }
}
