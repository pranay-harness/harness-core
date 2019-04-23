package software.wings.graphql.datafetcher.execution;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.exception.UnexpectedException;
import io.harness.govern.Switch;
import software.wings.beans.WorkflowExecution;
import software.wings.graphql.schema.type.QLExecution;
import software.wings.graphql.schema.type.QLExecutionStatus;
import software.wings.graphql.schema.type.QLPipelineExecution;
import software.wings.graphql.schema.type.QLPipelineExecution.QLPipelineExecutionBuilder;
import software.wings.graphql.schema.type.QLWorkflowExecution;
import software.wings.graphql.schema.type.QLWorkflowExecution.QLWorkflowExecutionBuilder;

import javax.validation.constraints.NotNull;

/**
 * Deliberately having a single class to adapt both
 * workflow and workflow execution.
 * Ideally, we should have two separate adapters.
 */
public class ExecutionController {
  public static QLExecutionStatus convertStatus(ExecutionStatus status) {
    switch (status) {
      case ABORTED:
      case ABORTING:
        return QLExecutionStatus.ABORTED;
      case DISCONTINUING:
      case RUNNING:
        return QLExecutionStatus.RUNNING;
      case ERROR:
        return QLExecutionStatus.ERROR;
      case FAILED:
        return QLExecutionStatus.FAILED;
      case PAUSED:
      case PAUSING:
        return QLExecutionStatus.PAUSED;
      case QUEUED:
      case NEW:
      case SCHEDULED:
      case STARTING:
        return QLExecutionStatus.QUEUED;
      case RESUMED:
        return QLExecutionStatus.RESUMED;
      case SUCCESS:
        return QLExecutionStatus.SUCCESS;
      case WAITING:
        return QLExecutionStatus.WAITING;
      case SKIPPED:
        return QLExecutionStatus.SKIPPED;
      case REJECTED:
        return QLExecutionStatus.REJECTED;
      case EXPIRED:
        return QLExecutionStatus.EXPIRED;
      default:
        Switch.unhandled(status);
    }
    return null;
  }

  public static QLExecution populateExecution(@NotNull WorkflowExecution execution) {
    if (execution.getWorkflowType() == WorkflowType.ORCHESTRATION) {
      final QLWorkflowExecutionBuilder builder = QLWorkflowExecution.builder();
      WorkflowExecutionController.populateWorkflowExecution(execution, builder);
      return builder.build();
    }

    if (execution.getWorkflowType() == WorkflowType.PIPELINE) {
      final QLPipelineExecutionBuilder builder = QLPipelineExecution.builder();
      PipelineExecutionController.populatePipelineExecution(execution, builder);
      return builder.build();
    }

    throw new UnexpectedException();
  }
}
