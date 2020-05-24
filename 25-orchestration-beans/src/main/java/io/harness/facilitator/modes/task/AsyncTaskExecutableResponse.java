package io.harness.facilitator.modes.task;

import io.harness.facilitator.modes.ExecutableResponse;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AsyncTaskExecutableResponse implements ExecutableResponse {
  String taskId;
  String taskIdentifier;
}
