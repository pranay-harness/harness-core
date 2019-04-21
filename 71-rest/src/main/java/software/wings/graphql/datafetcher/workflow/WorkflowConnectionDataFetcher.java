package software.wings.graphql.datafetcher.workflow;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
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
import software.wings.service.impl.security.auth.AuthHandler;

@Slf4j
public class WorkflowConnectionDataFetcher extends AbstractConnectionDataFetcher<QLWorkflowConnection> {
  @Inject
  public WorkflowConnectionDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  @Override
  public QLWorkflowConnection fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    QLWorkflowsQueryParameters qlQuery = fetchParameters(QLWorkflowsQueryParameters.class, dataFetchingEnvironment);

    final Query<Workflow> query = persistence.createQuery(Workflow.class)
                                      .filter(WorkflowKeys.appId, qlQuery.getAppId())
                                      .order(Sort.descending(WorkflowKeys.createdAt));

    QLWorkflowConnectionBuilder connectionBuilder = QLWorkflowConnection.builder();
    connectionBuilder.pageInfo(populate(qlQuery, query, dataFetchingEnvironment, workflow -> {
      QLWorkflowBuilder builder = QLWorkflow.builder();
      WorkflowController.populateWorkflow(workflow, builder);
      connectionBuilder.node(builder.build());
    }));

    return connectionBuilder.build();
  }
}
