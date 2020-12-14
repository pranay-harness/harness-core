package io.harness.pms.sdk.core.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.data.StepOutcomeRef;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class AdvisingEvent {
  @NonNull Ambiance ambiance;
  List<StepOutcomeRef> stepOutcomeRef;
  byte[] adviserParameters;
  Status toStatus;
  Status fromStatus;
  FailureInfo failureInfo;
}
