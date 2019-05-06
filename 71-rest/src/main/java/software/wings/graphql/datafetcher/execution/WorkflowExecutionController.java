package software.wings.graphql.datafetcher.execution;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.HPersistence;
import software.wings.beans.WorkflowExecution;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.QLCause;
import software.wings.graphql.schema.type.QLExecutedBy;
import software.wings.graphql.schema.type.QLExecutedBy.QLExecuteOptions;
import software.wings.graphql.schema.type.QLPipelineExecution;
import software.wings.graphql.schema.type.QLPipelineExecution.QLPipelineExecutionBuilder;
import software.wings.graphql.schema.type.QLWorkflowExecution.QLWorkflowExecutionBuilder;

import javax.validation.constraints.NotNull;

/**
 * Deliberately having a single class to adapt both
 * workflow and workflow execution.
 * Ideally, we should have two separate adapters.
 */
@Singleton
public class WorkflowExecutionController {
  @Inject private HPersistence persistence;

  public void populateWorkflowExecution(
      @NotNull WorkflowExecution workflowExecution, QLWorkflowExecutionBuilder builder) {
    QLCause cause = null;

    if (workflowExecution.getPipelineExecutionId() != null) {
      final WorkflowExecution pipelineExecution =
          persistence.get(WorkflowExecution.class, workflowExecution.getPipelineExecutionId());

      QLPipelineExecutionBuilder pipelineExecutionBuilder = QLPipelineExecution.builder();
      PipelineExecutionController.populatePipelineExecution(pipelineExecution, pipelineExecutionBuilder);
      cause = pipelineExecutionBuilder.build();

    } else if (workflowExecution.getTriggeredBy() != null) {
      cause = QLExecutedBy.builder()
                  .user(UserController.populateUser(workflowExecution.getTriggeredBy()))
                  .using(QLExecuteOptions.WEB_UI)
                  .build();
    }

    builder.id(workflowExecution.getUuid())
        .createdAt(GraphQLDateTimeScalar.convert(workflowExecution.getCreatedAt()))
        .startedAt(GraphQLDateTimeScalar.convert(workflowExecution.getStartTs()))
        .endedAt(GraphQLDateTimeScalar.convert(workflowExecution.getEndTs()))
        .status(ExecutionController.convertStatus(workflowExecution.getStatus()))
        .cause(cause);
  }
}
