package software.wings.delegatetasks.spotinst.taskhandler;

import com.google.inject.Singleton;

import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.command.ExecutionLogCallback;

@Slf4j
@Singleton
@NoArgsConstructor
public class SpotInstDeployTaskHandler extends SpotInstTaskHandler {
  protected SpotInstTaskExecutionResponse executeTaskInternal(
      SpotInstTaskParameters spotInstTaskParameters, ExecutionLogCallback logCallback) {
    return null;
  }
}