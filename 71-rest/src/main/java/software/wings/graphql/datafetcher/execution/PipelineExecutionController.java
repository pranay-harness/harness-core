package software.wings.graphql.datafetcher.execution;

import software.wings.beans.WorkflowExecution;
import software.wings.graphql.schema.type.QLPipelineExecution.QLPipelineExecutionBuilder;

import javax.validation.constraints.NotNull;

/**
 * Deliberately having a single class to adapt both
 * workflow and workflow execution.
 * Ideally, we should have two separate adapters.
 */
public class PipelineExecutionController {
  public static void populatePipelineExecution(
      @NotNull WorkflowExecution workflowExecution, QLPipelineExecutionBuilder builder) {
    builder.id(workflowExecution.getUuid())
        .queuedTime(workflowExecution.getCreatedAt())
        .startTime(workflowExecution.getStartTs())
        .endTime(workflowExecution.getEndTs())
        .status(ExecutionController.convertStatus(workflowExecution.getStatus()));
  }
}
