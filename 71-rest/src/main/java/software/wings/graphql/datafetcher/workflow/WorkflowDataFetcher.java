package software.wings.graphql.datafetcher.workflow;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.query.QLWorkflowQueryParameters;
import software.wings.graphql.schema.type.QLWorkflow;
import software.wings.graphql.schema.type.QLWorkflow.QLWorkflowBuilder;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkflowDataFetcher extends AbstractDataFetcher<QLWorkflow, QLWorkflowQueryParameters> {
  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionType.WORKFLOW, action = Action.READ)
  public QLWorkflow fetch(QLWorkflowQueryParameters qlQuery) {
    Workflow workflow = null;

    if (qlQuery.getWorkflowId() != null) {
      workflow = persistence.get(Workflow.class, qlQuery.getWorkflowId());
    } else if (qlQuery.getExecutionId() != null) {
      // TODO: add this to in memory cache
      final String workflowId = persistence.createQuery(WorkflowExecution.class)
                                    .filter(WorkflowExecutionKeys.uuid, qlQuery.getExecutionId())
                                    .project(WorkflowExecutionKeys.workflowId, true)
                                    .get()
                                    .getWorkflowId();

      workflow = persistence.get(Workflow.class, workflowId);
    }

    if (workflow == null) {
      throw new InvalidRequestException("Workflow does not exist", WingsException.USER);
    }

    final QLWorkflowBuilder builder = QLWorkflow.builder();
    WorkflowController.populateWorkflow(workflow, builder);
    return builder.build();
  }
}
