package software.wings.graphql.datafetcher.workflow;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Workflow;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.query.QLWorkflowQueryParameters;
import software.wings.graphql.schema.type.QLWorkflow;
import software.wings.graphql.schema.type.QLWorkflow.QLWorkflowBuilder;
import software.wings.service.impl.security.auth.AuthHandler;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkflowDataFetcher extends AbstractDataFetcher<QLWorkflow> {
  @Inject HPersistence persistence;

  @Inject
  public WorkflowDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  @Override
  public QLWorkflow fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    QLWorkflowQueryParameters qlQuery = fetchParameters(QLWorkflowQueryParameters.class, dataFetchingEnvironment);
    Workflow workflow = persistence.get(Workflow.class, qlQuery.getWorkflowId());
    if (workflow == null) {
      throw new InvalidRequestException("Workflow does not exist", WingsException.USER);
    }

    final QLWorkflowBuilder builder = QLWorkflow.builder();
    WorkflowController.populateWorkflow(workflow, builder);
    return builder.build();
  }
}
