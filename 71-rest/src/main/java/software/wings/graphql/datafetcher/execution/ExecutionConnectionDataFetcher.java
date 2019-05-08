package software.wings.graphql.datafetcher.execution;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.schema.query.QLExecutionsQueryParameters;
import software.wings.graphql.schema.type.QLExecution;
import software.wings.graphql.schema.type.QLExecutionConnection;
import software.wings.graphql.schema.type.QLExecutionConnection.QLExecutionConnectionBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class ExecutionConnectionDataFetcher
    extends AbstractConnectionDataFetcher<QLExecutionConnection, QLExecutionsQueryParameters> {
  @Inject private ExecutionController executionController;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public QLExecutionConnection fetchConnection(QLExecutionsQueryParameters qlQuery) {
    final Query<WorkflowExecution> query = persistence.createAuthorizedQuery(WorkflowExecution.class)
                                               .order(Sort.descending(WorkflowExecutionKeys.createdAt));

    if (qlQuery.getApplicationId() != null) {
      query.filter(WorkflowExecutionKeys.appId, qlQuery.getApplicationId());
    }

    if (qlQuery.getWorkflowId() != null) {
      query.filter(WorkflowExecutionKeys.workflowId, qlQuery.getWorkflowId());
    }

    if (qlQuery.getPipelineId() != null) {
      query.filter(WorkflowExecutionKeys.workflowId, qlQuery.getPipelineId());
    }

    if (qlQuery.getServiceId() != null) {
      query.filter(WorkflowExecutionKeys.serviceIds, qlQuery.getServiceId());
    }
    if (isNotEmpty(qlQuery.getStatuses())) {
      query.field(WorkflowExecutionKeys.status).in(ExecutionController.convertStatus(qlQuery.getStatuses()));
    }

    filterDatetimeRange(query, WorkflowExecutionKeys.createdAt, qlQuery.getFrom(), qlQuery.getTo());

    QLExecutionConnectionBuilder connectionBuilder = QLExecutionConnection.builder();
    connectionBuilder.pageInfo(populate(qlQuery, query, execution -> {
      final QLExecution qlExecution = executionController.populateExecution(execution);
      connectionBuilder.node(qlExecution);
    }));
    return connectionBuilder.build();
  }
}
