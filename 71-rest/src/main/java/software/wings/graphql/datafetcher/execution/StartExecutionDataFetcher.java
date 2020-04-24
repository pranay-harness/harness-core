package software.wings.graphql.datafetcher.execution;

import static software.wings.graphql.datafetcher.DataFetcherUtils.GENERIC_EXCEPTION_MSG;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.execution.input.QLExecutionType;
import software.wings.graphql.schema.mutation.execution.input.QLStartExecutionInput;
import software.wings.graphql.schema.mutation.execution.payload.QLStartExecutionPayload;
import software.wings.graphql.schema.type.QLExecution;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.Collections;
import java.util.List;

@Slf4j
public class StartExecutionDataFetcher extends BaseMutatorDataFetcher<QLStartExecutionInput, QLStartExecutionPayload> {
  @Inject PipelineExecutionController pipelineExecutionController;
  @Inject WorkflowExecutionController workflowExecutionController;

  @Inject
  public StartExecutionDataFetcher(PipelineExecutionController pipelineExecutionController,
      WorkflowExecutionController workflowExecutionController) {
    super(QLStartExecutionInput.class, QLStartExecutionPayload.class);
    this.pipelineExecutionController = pipelineExecutionController;
    this.workflowExecutionController = workflowExecutionController;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLStartExecutionPayload mutateAndFetch(
      QLStartExecutionInput triggerExecutionInput, MutationContext mutationContext) {
    try (AutoLogContext ignore0 =
             new AccountLogContext(mutationContext.getAccountId(), AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
      PermissionAttribute permissionAttribute =
          new PermissionAttribute(PermissionAttribute.PermissionType.DEPLOYMENT, PermissionAttribute.Action.EXECUTE);
      List<PermissionAttribute> permissionAttributeList = Collections.singletonList(permissionAttribute);

      QLExecutionType executionType = triggerExecutionInput.getExecutionType();
      QLExecution execution = null;
      switch (executionType) {
        case PIPELINE:
          execution = pipelineExecutionController.startPipelineExecution(
              triggerExecutionInput, mutationContext, permissionAttributeList);
          break;
        case WORKFLOW:
          execution = workflowExecutionController.startWorkflowExecution(
              triggerExecutionInput, mutationContext, permissionAttributeList);
          break;
        default:
          throw new UnsupportedOperationException("Unsupported execution type: " + executionType);
      }

      if (execution == null) {
        throw new InvalidRequestException(GENERIC_EXCEPTION_MSG);
      }
      return QLStartExecutionPayload.builder()
          .execution(execution)
          .clientMutationId(triggerExecutionInput.getClientMutationId())
          .build();
    }
  }
}
