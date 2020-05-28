package software.wings.graphql.datafetcher.workflow.batch;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dataloader.DataLoader;
import software.wings.graphql.datafetcher.AbstractBatchDataFetcher;
import software.wings.graphql.schema.query.QLWorkflowQueryParameters;
import software.wings.graphql.schema.type.QLWorkflow;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.concurrent.CompletionStage;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkflowBatchDataFetcher extends AbstractBatchDataFetcher<QLWorkflow, QLWorkflowQueryParameters, String> {
  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected CompletionStage<QLWorkflow> load(
      QLWorkflowQueryParameters parameters, DataLoader<String, QLWorkflow> dataLoader) {
    final String workflowId;
    if (StringUtils.isNotBlank(parameters.getWorkflowId())) {
      workflowId = parameters.getWorkflowId();
    } else {
      throw new InvalidRequestException("Workflow Id not present in query", WingsException.USER);
    }
    return dataLoader.load(workflowId);
  }
}
