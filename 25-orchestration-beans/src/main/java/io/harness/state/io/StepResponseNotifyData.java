package io.harness.state.io;

import io.harness.delegate.beans.ResponseData;
import io.harness.execution.status.Status;
import io.harness.state.io.StepResponse.StepOutcome;
import lombok.Builder;
import lombok.Value;

import java.util.Collection;

@Value
@Builder
public class StepResponseNotifyData implements ResponseData {
  String identifier;
  String nodeUuid;
  String group;
  Collection<StepOutcome> stepOutcomes;
  FailureInfo failureInfo;
  Status status;
}
