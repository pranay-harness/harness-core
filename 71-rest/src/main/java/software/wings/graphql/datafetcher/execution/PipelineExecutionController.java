package software.wings.graphql.datafetcher.execution;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import lombok.experimental.UtilityClass;
import software.wings.beans.WorkflowExecution;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.QLExecutedByUser;
import software.wings.graphql.schema.type.QLExecutedByUser.QLExecuteOptions;
import software.wings.graphql.schema.type.QLPipelineExecution.QLPipelineExecutionBuilder;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTag;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

/**
 * Deliberately having a single class to adapt both
 * workflow and workflow execution.
 * Ideally, we should have two separate adapters.
 */
@UtilityClass
public class PipelineExecutionController {
  public static void populatePipelineExecution(
      @NotNull WorkflowExecution workflowExecution, QLPipelineExecutionBuilder builder) {
    QLExecutedByUser cause = QLExecutedByUser.builder()
                                 .user(UserController.populateUser(workflowExecution.getTriggeredBy()))
                                 .using(QLExecuteOptions.WEB_UI)
                                 .build();

    List<QLDeploymentTag> tags = new ArrayList<>();
    if (isNotEmpty(workflowExecution.getTags())) {
      tags = workflowExecution.getTags()
                 .stream()
                 .map(tag -> QLDeploymentTag.builder().name(tag.getName()).value(tag.getValue()).build())
                 .collect(Collectors.toList());
    }

    builder.id(workflowExecution.getUuid())
        .appId(workflowExecution.getAppId())
        .createdAt(workflowExecution.getCreatedAt())
        .startedAt(workflowExecution.getStartTs())
        .endedAt(workflowExecution.getEndTs())
        .status(ExecutionController.convertStatus(workflowExecution.getStatus()))
        .cause(cause)
        .notes(workflowExecution.getExecutionArgs() == null ? null : workflowExecution.getExecutionArgs().getNotes())
        .tags(tags)
        .build();
  }
}
