package software.wings.graphql.datafetcher.execution;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.WorkflowExecution;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.query.QLExecutionParameters;
import software.wings.graphql.schema.type.QLExecution;
import software.wings.service.impl.security.auth.AuthHandler;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class ExecutionDataFetcher extends AbstractDataFetcher<QLExecution> {
  @Inject protected HPersistence persistence;

  @Inject
  public ExecutionDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  @Override
  public QLExecution fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    QLExecutionParameters qlQuery = fetchParameters(QLExecutionParameters.class, dataFetchingEnvironment);

    WorkflowExecution execution = persistence.get(WorkflowExecution.class, qlQuery.getExecutionId());
    if (execution == null) {
      throw new InvalidRequestException("Execution does not exist", WingsException.USER);
    }

    return ExecutionController.populateExecution(execution);
  }
}
