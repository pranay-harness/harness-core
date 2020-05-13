package software.wings.graphql.datafetcher.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.execution.input.QLExecutionType;
import software.wings.graphql.schema.mutation.execution.input.QLStartExecutionInput;
import software.wings.graphql.schema.mutation.execution.payload.QLStartExecutionPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;

import java.util.Collections;
import java.util.List;

@OwnedBy(CDC)
@Slf4j
public class StartExecutionDataFetcher extends BaseMutatorDataFetcher<QLStartExecutionInput, QLStartExecutionPayload> {
  @Inject PipelineExecutionController pipelineExecutionController;
  @Inject WorkflowExecutionController workflowExecutionController;
  @Inject AppService appService;

  public static final String APPLICATION_DOES_NOT_EXIST_MSG = "Application does not exist";

  @Inject
  public StartExecutionDataFetcher(PipelineExecutionController pipelineExecutionController,
      WorkflowExecutionController workflowExecutionController, AppService appService) {
    super(QLStartExecutionInput.class, QLStartExecutionPayload.class);
    this.pipelineExecutionController = pipelineExecutionController;
    this.workflowExecutionController = workflowExecutionController;
    this.appService = appService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLStartExecutionPayload mutateAndFetch(
      QLStartExecutionInput triggerExecutionInput, MutationContext mutationContext) {
    try (AutoLogContext ignore0 =
             new AccountLogContext(mutationContext.getAccountId(), AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
      if (isEmpty(triggerExecutionInput.getEntityId())) {
        throw new InvalidRequestException("Entity Id cannot be empty", WingsException.USER);
      }

      validateAppBelongsToAccount(triggerExecutionInput, mutationContext);

      PermissionAttribute permissionAttribute =
          new PermissionAttribute(PermissionAttribute.PermissionType.DEPLOYMENT, PermissionAttribute.Action.EXECUTE);
      List<PermissionAttribute> permissionAttributeList = Collections.singletonList(permissionAttribute);

      QLExecutionType executionType = triggerExecutionInput.getExecutionType();
      QLStartExecutionPayload response;
      switch (executionType) {
        case PIPELINE:
          response = pipelineExecutionController.startPipelineExecution(
              triggerExecutionInput, mutationContext, permissionAttributeList);
          break;
        case WORKFLOW:
          response = workflowExecutionController.startWorkflowExecution(
              triggerExecutionInput, mutationContext, permissionAttributeList);
          break;
        default:
          throw new UnsupportedOperationException("Unsupported execution type: " + executionType);
      }
      return response;
    }
  }

  private void validateAppBelongsToAccount(
      QLStartExecutionInput triggerExecutionInput, MutationContext mutationContext) {
    String appId = triggerExecutionInput.getApplicationId();
    if (isEmpty(appId)) {
      throw new InvalidRequestException("Application Id cannot be empty", WingsException.USER);
    }
    String accountIdFromApp = appService.getAccountIdByAppId(appId);
    if (!accountIdFromApp.equals(mutationContext.getAccountId())) {
      throw new InvalidRequestException(APPLICATION_DOES_NOT_EXIST_MSG, WingsException.USER);
    }
  }
}
