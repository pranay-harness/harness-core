package software.wings.graphql.datafetcher.workflow;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLWorkflow;
import software.wings.graphql.schema.type.QLWorkflow.QLWorkflowBuilder;
import software.wings.graphql.schema.type.QLWorkflowConnection;
import software.wings.graphql.schema.type.QLWorkflowConnection.QLWorkflowConnectionBuilder;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.workflow.QLWorkflowFilter;
import software.wings.graphql.schema.type.aggregation.workflow.QLWorkflowFilterType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.util.List;

@Slf4j
public class WorkflowConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLWorkflowFilter, QLNoOpSortCriteria, QLWorkflowConnection> {
  @Override
  @AuthRule(permissionType = PermissionType.WORKFLOW, action = Action.READ)
  public QLWorkflowConnection fetchConnection(List<QLWorkflowFilter> serviceFilters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<Workflow> query = populateFilters(wingsPersistence, serviceFilters, Workflow.class);
    query.order(Sort.descending(WorkflowKeys.createdAt));
    QLWorkflowConnectionBuilder connectionBuilder = QLWorkflowConnection.builder();
    connectionBuilder.pageInfo(utils.populate(pageQueryParameters, query, workflow -> {
      QLWorkflowBuilder builder = QLWorkflow.builder();
      WorkflowController.populateWorkflow(workflow, builder);
      connectionBuilder.node(builder.build());
    }));

    return connectionBuilder.build();
  }

  protected String getFilterFieldName(String filterType) {
    QLWorkflowFilterType workflowType = QLWorkflowFilterType.valueOf(filterType);
    switch (workflowType) {
      case Application:
        return WorkflowKeys.appId;
      case Workflow:
        return WorkflowKeys.uuid;
      default:
        throw new WingsException("Unknown filter type" + filterType);
    }
  }
}
