package io.harness.delegate.task.executioncapability;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.TaskParameters;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._920_DELEGATE_SERVICE_BEANS)
public class BatchCapabilityCheckTaskParameters implements TaskParameters {
  private List<CapabilityCheckDetails> capabilityCheckDetailsList;
}
