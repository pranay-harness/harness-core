package io.harness.pms.sdk.core.steps.io;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.data.StepOutcomeRef;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.tasks.ResponseData;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class StepResponseNotifyData implements ResponseData {
  String identifier;
  String nodeUuid;
  String group;
  List<StepOutcomeRef> stepOutcomeRefs;
  FailureInfo failureInfo;
  Status status;
  String description;
}
