package io.harness.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.Status;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepOutcomeRef;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class AdvisingEvent {
  @NonNull Ambiance ambiance;
  List<StepOutcomeRef> stepOutcomeRef;
  byte[] adviserParameters;
  Status toStatus;
  Status fromStatus;
  FailureInfo failureInfo;
}
