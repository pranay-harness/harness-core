package software.wings.graphql.datafetcher.execution;

import software.wings.beans.WorkflowExecution;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.QLExecutedBy;
import software.wings.graphql.schema.type.QLExecutedBy.QLExecuteOptions;
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
    QLExecutedBy cause = QLExecutedBy.builder()
                             .user(UserController.populateUser(workflowExecution.getTriggeredBy()))
                             .using(QLExecuteOptions.WEB_UI)
                             .build();

    builder.id(workflowExecution.getUuid())
        .createdAt(GraphQLDateTimeScalar.convert(workflowExecution.getCreatedAt()))
        .startedAt(GraphQLDateTimeScalar.convert(workflowExecution.getStartTs()))
        .endedAt(GraphQLDateTimeScalar.convert(workflowExecution.getEndTs()))
        .status(ExecutionController.convertStatus(workflowExecution.getStatus()))
        .cause(cause);
  }
}
