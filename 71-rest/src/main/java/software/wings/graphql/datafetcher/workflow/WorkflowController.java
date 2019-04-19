package software.wings.graphql.datafetcher.workflow;

import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.graphql.schema.type.QLWorkflow.QLWorkflowBuilder;
import software.wings.graphql.schema.type.QLWorkflowExecution;

import javax.validation.constraints.NotNull;

/**
 * Deliberately having a single class to adapt both
 * workflow and workflow execution.
 * Ideally, we should have two separate adapters.
 */
public class WorkflowController {
  public static void populateWorkflow(@NotNull Workflow workflow, QLWorkflowBuilder builder) {
    builder.id(workflow.getUuid()).name(workflow.getName()).description(workflow.getDescription());
  }

  public static QLWorkflowExecution getWorkflowExecution(@NotNull WorkflowExecution workflowExecution) {
    return QLWorkflowExecution.builder()
        .id(workflowExecution.getUuid())
        .name(workflowExecution.getName())
        .status(workflowExecution.getStatus())
        .startTime(workflowExecution.getStartTs())
        .endTime(workflowExecution.getEndTs())
        .build();
  }
}
