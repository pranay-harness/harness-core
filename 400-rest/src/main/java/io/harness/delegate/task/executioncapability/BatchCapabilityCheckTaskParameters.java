package io.harness.delegate.task.executioncapability;

import io.harness.delegate.task.TaskParameters;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BatchCapabilityCheckTaskParameters implements TaskParameters {
  private List<CapabilityCheckDetails> capabilityCheckDetailsList;
}
