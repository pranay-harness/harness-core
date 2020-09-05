package io.harness.state.io;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.execution.status.Status;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
public class StepResponseNotifyData implements DelegateResponseData {
  String identifier;
  String nodeUuid;
  String group;
  List<StepOutcomeRef> stepOutcomeRefs;
  FailureInfo failureInfo;
  Status status;
  String description;
}