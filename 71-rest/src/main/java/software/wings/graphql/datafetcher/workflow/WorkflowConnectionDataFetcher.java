package software.wings.graphql.datafetcher.workflow;

import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.schema.query.QLWorkflowsQueryParameters;
import software.wings.graphql.schema.type.QLWorkflow;
import software.wings.graphql.schema.type.QLWorkflow.QLWorkflowBuilder;
import software.wings.graphql.schema.type.QLWorkflowConnection;
import software.wings.graphql.schema.type.QLWorkflowConnection.QLWorkflowConnectionBuilder;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class WorkflowConnectionDataFetcher
    extends AbstractConnectionDataFetcher<QLWorkflowConnection, QLWorkflowsQueryParameters> {
  @Override
  @AuthRule(permissionType = PermissionType.WORKFLOW, action = Action.READ)
  public QLWorkflowConnection fetchConnection(QLWorkflowsQueryParameters qlQuery) {
    final Query<Workflow> query = persistence.createAuthorizedQuery(Workflow.class)
                                      .filter(WorkflowKeys.appId, qlQuery.getApplicationId())
                                      .order(Sort.descending(WorkflowKeys.createdAt));

    QLWorkflowConnectionBuilder connectionBuilder = QLWorkflowConnection.builder();
    connectionBuilder.pageInfo(populate(qlQuery, query, workflow -> {
      QLWorkflowBuilder builder = QLWorkflow.builder();
      WorkflowController.populateWorkflow(workflow, builder);
      connectionBuilder.node(builder.build());
    }));

    return connectionBuilder.build();
  }
}
