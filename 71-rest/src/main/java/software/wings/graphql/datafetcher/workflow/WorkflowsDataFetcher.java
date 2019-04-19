package software.wings.graphql.datafetcher.workflow;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.PagedData;
import software.wings.graphql.schema.type.QLWorkflow;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.WorkflowService;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkflowsDataFetcher extends AbstractDataFetcher<PagedData<QLWorkflow>> {
  WorkflowService workflowService;

  @Inject
  public WorkflowsDataFetcher(WorkflowService workflowService, AuthHandler authHandler) {
    super(authHandler);
    this.workflowService = workflowService;
  }

  @Override
  public PagedData<QLWorkflow> fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }
}
