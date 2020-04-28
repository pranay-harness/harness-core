package software.wings.graphql.datafetcher.workflow;

import lombok.experimental.UtilityClass;
import software.wings.beans.Workflow;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.QLWorkflow.QLWorkflowBuilder;

import javax.validation.constraints.NotNull;

/**
 * Deliberately having a single class to adapt both
 * workflow and workflow execution.
 * Ideally, we should have two separate adapters.
 */
@UtilityClass
public class WorkflowController {
  public static void populateWorkflow(@NotNull Workflow workflow, QLWorkflowBuilder builder) {
    builder.id(workflow.getUuid())
        .name(workflow.getName())
        .applicationId(workflow.getAppId())
        .description(workflow.getDescription())
        .createdAt(workflow.getCreatedAt())
        .createdBy(UserController.populateUser(workflow.getCreatedBy()));
  }
}
