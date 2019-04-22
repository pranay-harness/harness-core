package software.wings.graphql.datafetcher.execution;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.PagedData;
import software.wings.graphql.schema.type.QLPipelineExecution;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.WorkflowExecutionService;

@Slf4j
public class WorkflowExecutionsDataFetcher extends AbstractDataFetcher<PagedData<QLPipelineExecution>> {
  WorkflowExecutionService workflowExecutionService;

  @Inject
  public WorkflowExecutionsDataFetcher(WorkflowExecutionService workflowExecutionService, AuthHandler authHandler) {
    super(authHandler);
    this.workflowExecutionService = workflowExecutionService;
  }

  @Override
  public PagedData<QLPipelineExecution> fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }
}
