package software.wings.graphql.datafetcher.workflow.adapater;

import com.google.inject.Singleton;

import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.graphql.schema.type.WorkflowExecutionInfo;
import software.wings.graphql.schema.type.WorkflowInfo;

import javax.validation.constraints.NotNull;

/**
 * Deliberately having a single class to adapt both
 * workflow and workflow execution.
 * Ideally, we should have two separate adapters.
 */
@Singleton
public class WorkflowAdapter {
  public WorkflowInfo getWorkflow(@NotNull Workflow workflow) {
    return WorkflowInfo.builder()
        .id(workflow.getUuid())
        .name(workflow.getName())
        .description(workflow.getDescription())
        .workflowType(workflow.getWorkflowType())
        .templatized(workflow.isTemplatized())
        .services(workflow.getServices())
        .envId(workflow.getEnvId())
        .appId(workflow.getAppId())
        .build();
  }

  public WorkflowExecutionInfo getWorkflowExecution(@NotNull WorkflowExecution workflowExecution) {
    return WorkflowExecutionInfo.builder()
        .id(workflowExecution.getUuid())
        .name(workflowExecution.getName())
        .status(workflowExecution.getStatus())
        .startTime(workflowExecution.getStartTs())
        .endTime(workflowExecution.getEndTs())
        .build();
  }
}
