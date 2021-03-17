package io.harness.delegate.task.terraform;

import io.harness.delegate.task.TaskParameters;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TerraformTaskNGParameters implements TaskParameters {
  TFTaskType taskType;
}
