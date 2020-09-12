package io.harness.delegate.task.stepstatus;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.Map;

@Data
@Builder
public class StepMapOutput implements StepOutput {
  @Singular("output") Map<String, String> map;
}
