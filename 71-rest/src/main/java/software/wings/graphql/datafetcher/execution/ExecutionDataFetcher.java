package software.wings.graphql.datafetcher.execution;

import com.google.inject.Inject;

import io.harness.beans.WorkflowType;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.query.QLExecutionQueryParameters;
import software.wings.graphql.schema.type.QLExecution;
import software.wings.graphql.schema.type.QLPipelineExecution;
import software.wings.graphql.schema.type.QLPipelineExecution.QLPipelineExecutionBuilder;
import software.wings.graphql.schema.type.QLWorkflowExecution;
import software.wings.graphql.schema.type.QLWorkflowExecution.QLWorkflowExecutionBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.security.auth.AuthHandler;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class ExecutionDataFetcher extends AbstractDataFetcher<QLExecution, QLExecutionQueryParameters> {
  @Inject protected WingsPersistence persistence;
  @Inject protected AuthHandler authHandler;
  @Inject private WorkflowExecutionController workflowExecutionController;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public QLExecution fetch(QLExecutionQueryParameters qlQuery) {
    WorkflowExecution execution =
        persistence.createAuthorizedQuery(WorkflowExecution.class).filter("_id", qlQuery.getExecutionId()).get();
    if (execution == null) {
      throw new InvalidRequestException("Execution does not exist or access is denied", WingsException.USER);
    }

    if (execution.getWorkflowType() == WorkflowType.ORCHESTRATION) {
      final QLWorkflowExecutionBuilder builder = QLWorkflowExecution.builder();
      workflowExecutionController.populateWorkflowExecution(execution, builder);
      return builder.build();
    }

    if (execution.getWorkflowType() == WorkflowType.PIPELINE) {
      final QLPipelineExecutionBuilder builder = QLPipelineExecution.builder();
      PipelineExecutionController.populatePipelineExecution(execution, builder);
      return builder.build();
    }

    throw new UnexpectedException();
  }
}
