package io.harness.delegate.task.stepstatus;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class StepMapOutput implements StepOutput {
  @Singular("output") Map<String, String> map;
}
