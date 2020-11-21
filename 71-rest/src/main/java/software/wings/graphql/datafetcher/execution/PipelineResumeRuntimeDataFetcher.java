package software.wings.graphql.datafetcher.execution;

import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.pipeline.input.QLRuntimeExecutionInputs;
import software.wings.graphql.schema.mutation.pipeline.payload.QLContinueExecutionPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;

import com.google.inject.Inject;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PipelineResumeRuntimeDataFetcher
    extends BaseMutatorDataFetcher<QLRuntimeExecutionInputs, QLContinueExecutionPayload> {
  @Inject WorkflowExecutionService workflowExecutionService;
  @Inject WingsPersistence persistence;
  @Inject PipelineExecutionController pipelineExecutionController;
  @Inject PipelineService pipelineService;

  public PipelineResumeRuntimeDataFetcher() {
    super(QLRuntimeExecutionInputs.class, QLContinueExecutionPayload.class);
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLContinueExecutionPayload mutateAndFetch(
      QLRuntimeExecutionInputs parameter, MutationContext mutationContext) {
    // TODO Add NULLS checks
    String appId = parameter.getApplicationId();
    WorkflowExecution execution =
        workflowExecutionService.getWorkflowExecution(appId, parameter.getPipelineExecutionId());
    String pipelineId = execution.getWorkflowId();
    Pipeline pipeline = pipelineService.readPipeline(appId, pipelineId, true);
    // Map Names to ID
    ExecutionArgs executionArgs = ExecutionArgs.builder()
                                      .stageName(execution.getStageName())
                                      .workflowType(execution.getWorkflowType())
                                      .workflowVariables(pipelineExecutionController.resolvePipelineVariables(
                                          pipeline, parameter.getVariableInputs(), null, new ArrayList<>(), false))
                                      .artifactVariables(null)
                                      .artifacts(null)
                                      .build();
    boolean status = workflowExecutionService.continuePipelineStage(
        execution.getAppId(), parameter.getPipelineExecutionId(), parameter.getPipelineStageElementId(), executionArgs);

    // TODO triggered continue pipeline with insufficient info and it went ahead and failed later.
    return QLContinueExecutionPayload.builder()
        .clientMutationId(parameter.getClientMutationId())
        .status(status)
        .build();
  }
}
