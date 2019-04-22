package software.wings.graphql.datafetcher.execution;

import software.wings.beans.WorkflowExecution;
import software.wings.graphql.schema.type.QLWorkflowExecution.QLWorkflowExecutionBuilder;

import javax.validation.constraints.NotNull;

/**
 * Deliberately having a single class to adapt both
 * workflow and workflow execution.
 * Ideally, we should have two separate adapters.
 */
public class WorkflowExecutionController {
  public static void populateWorkflowExecution(
      @NotNull WorkflowExecution workflowExecution, QLWorkflowExecutionBuilder builder) {
    builder.id(workflowExecution.getUuid())
        .queuedTime(workflowExecution.getCreatedAt())
        .startTime(workflowExecution.getStartTs())
        .endTime(workflowExecution.getEndTs())
        .status(workflowExecution.getStatus());
  }
}
