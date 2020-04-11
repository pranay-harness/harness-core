package software.wings.graphql.datafetcher.execution;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.HPersistence;
import software.wings.beans.WorkflowExecution;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.query.QLExecutionQueryParameters.QLExecutionQueryParametersKeys;
import software.wings.graphql.schema.query.QLTriggerQueryParameters.QLTriggerQueryParametersKeys;
import software.wings.graphql.schema.type.QLCause;
import software.wings.graphql.schema.type.QLExecutedAlongPipeline;
import software.wings.graphql.schema.type.QLExecutedByTrigger;
import software.wings.graphql.schema.type.QLExecutedByUser;
import software.wings.graphql.schema.type.QLExecutedByUser.QLExecuteOptions;
import software.wings.graphql.schema.type.QLWorkflowExecution.QLWorkflowExecutionBuilder;

import javax.validation.constraints.NotNull;

@Singleton
public class WorkflowExecutionController {
  @Inject private HPersistence persistence;

  public void populateWorkflowExecution(
      @NotNull WorkflowExecution workflowExecution, QLWorkflowExecutionBuilder builder) {
    QLCause cause = null;

    if (workflowExecution.getPipelineExecutionId() != null) {
      cause =
          QLExecutedAlongPipeline.builder()
              .context(ImmutableMap.<String, Object>builder()
                           .put(QLExecutionQueryParametersKeys.executionId, workflowExecution.getPipelineExecutionId())
                           .build())
              .build();
    } else {
      if (workflowExecution.getDeploymentTriggerId() != null) {
        cause =
            QLExecutedByTrigger.builder()
                .context(ImmutableMap.<String, Object>builder()
                             .put(QLTriggerQueryParametersKeys.triggerId, workflowExecution.getDeploymentTriggerId())
                             .build())
                .build();
      }

      if (workflowExecution.getTriggeredBy() != null) {
        cause = QLExecutedByUser.builder()
                    .user(UserController.populateUser(workflowExecution.getTriggeredBy()))
                    .using(QLExecuteOptions.WEB_UI)
                    .build();
      }
    }

    builder.id(workflowExecution.getUuid())
        .appId(workflowExecution.getAppId())
        .createdAt(workflowExecution.getCreatedAt())
        .startedAt(workflowExecution.getStartTs())
        .endedAt(workflowExecution.getEndTs())
        .status(ExecutionController.convertStatus(workflowExecution.getStatus()))
        .cause(cause)
        .notes(workflowExecution.getExecutionArgs() == null ? null : workflowExecution.getExecutionArgs().getNotes());
  }
}
