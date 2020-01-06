package software.wings.graphql.datafetcher.execution;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

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

import java.util.Collection;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Deliberately having a single class to adapt both
 * workflow and workflow execution.
 * Ideally, we should have two separate adapters.
 */
@Singleton
public class ExecutionController {
  @Inject WorkflowExecutionController workflowExecutionController;

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

  public static List<ExecutionStatus> convertStatus(List<QLExecutionStatus> statuses) {
    return statuses.stream().map(ExecutionController::convertStatus).flatMap(Collection::stream).collect(toList());
  }

  public static List<ExecutionStatus> convertStatus(QLExecutionStatus status) {
    switch (status) {
      case ABORTED:
        return asList(ExecutionStatus.ABORTED, ExecutionStatus.ABORTING);
      case RUNNING:
        return asList(ExecutionStatus.RUNNING, ExecutionStatus.DISCONTINUING);
      case ERROR:
        return asList(ExecutionStatus.ERROR);
      case FAILED:
        return asList(ExecutionStatus.FAILED);
      case PAUSED:
        return asList(ExecutionStatus.PAUSED, ExecutionStatus.PAUSING);
      case QUEUED:
        return asList(ExecutionStatus.QUEUED, ExecutionStatus.STARTING, ExecutionStatus.SCHEDULED, ExecutionStatus.NEW);
      case RESUMED:
        return asList(ExecutionStatus.RESUMED);
      case SUCCESS:
        return asList(ExecutionStatus.SUCCESS);
      case WAITING:
        return asList(ExecutionStatus.WAITING);
      case SKIPPED:
        return asList(ExecutionStatus.SKIPPED);
      case REJECTED:
        return asList(ExecutionStatus.REJECTED);
      case EXPIRED:
        return asList(ExecutionStatus.EXPIRED);
      default:
        Switch.unhandled(status);
    }
    return null;
  }

  public QLExecution populateExecution(@NotNull WorkflowExecution execution) {
    if (execution.getWorkflowType() == WorkflowType.ORCHESTRATION) {
      final QLWorkflowExecutionBuilder builder = QLWorkflowExecution.builder();
      workflowExecutionController.populateWorkflowExecution(execution, builder);
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
