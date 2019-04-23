package software.wings.graphql.datafetcher.execution;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.schema.query.QLExecutionsParameters;
import software.wings.graphql.schema.type.QLExecution;
import software.wings.graphql.schema.type.QLExecutionConnection;
import software.wings.graphql.schema.type.QLExecutionConnection.QLExecutionConnectionBuilder;
import software.wings.service.impl.security.auth.AuthHandler;

@Slf4j
public class ExecutionConnectionDataFetcher extends AbstractConnectionDataFetcher<QLExecutionConnection> {
  @Inject
  public ExecutionConnectionDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  @Override
  public QLExecutionConnection fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    QLExecutionsParameters qlQuery = fetchParameters(QLExecutionsParameters.class, dataFetchingEnvironment);

    final Query<WorkflowExecution> query =
        persistence.createQuery(WorkflowExecution.class).order(Sort.descending(WorkflowExecutionKeys.createdAt));

    if (qlQuery.getAppId() != null) {
      query.filter(WorkflowExecutionKeys.appId, qlQuery.getAppId());
    }
    if (qlQuery.getWorkflowId() != null) {
      query.filter(WorkflowExecutionKeys.workflowId, qlQuery.getWorkflowId());
    }
    if (qlQuery.getPipelineId() != null) {
      query.filter(WorkflowExecutionKeys.workflowId, qlQuery.getPipelineId());
    }

    QLExecutionConnectionBuilder connectionBuilder = QLExecutionConnection.builder();
    connectionBuilder.pageInfo(populate(qlQuery, query, execution -> {
      final QLExecution qlExecution = ExecutionController.populateExecution(execution);
      connectionBuilder.node(qlExecution);
    }));
    return connectionBuilder.build();
  }
}
