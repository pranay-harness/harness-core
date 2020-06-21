package io.harness.state.io;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.execution.status.Status;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
public class StepResponseNotifyData implements ResponseData {
  String identifier;
  String nodeUuid;
  String group;
  List<StepOutcomeRef> stepOutcomesRefs;
  FailureInfo failureInfo;
  Status status;
}
