package software.wings.delegatetasks.spotinst.taskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static java.lang.String.format;

import com.google.inject.Singleton;

import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.command.ExecutionLogCallback;

@Slf4j
@Singleton
@NoArgsConstructor
public class SpotInstSetupTaskHandler extends SpotInstTaskHandler {
  protected SpotInstTaskExecutionResponse executeTaskInternal(
      SpotInstTaskParameters spotInstTaskParameters, ExecutionLogCallback logCallback) {
    if (!(spotInstTaskParameters instanceof SpotInstSetupTaskParameters)) {
      String message =
          format("Parameters of unrecognized class: [%s] found while executing setup step. Workflow execution: [%s]",
              spotInstTaskParameters.getClass().getSimpleName(), spotInstTaskParameters.getWorkflowExecutionId());
      logger.error(message);
      return SpotInstTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
    }

    SpotInstSetupTaskParameters setupTaskParameters = (SpotInstSetupTaskParameters) spotInstTaskParameters;

    return null;
  }
}