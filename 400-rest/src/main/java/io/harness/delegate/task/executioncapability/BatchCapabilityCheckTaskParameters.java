package io.harness.delegate.task.executioncapability;

import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BatchCapabilityCheckTaskParameters implements TaskParameters {
  private List<CapabilityCheckDetails> capabilityCheckDetailsList;
}
