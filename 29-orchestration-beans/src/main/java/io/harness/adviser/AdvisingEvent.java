package io.harness.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.status.Status;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepOutcomeRef;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class AdvisingEvent<T extends AdviserParameters> {
  @NonNull Ambiance ambiance;
  List<StepOutcomeRef> stepOutcomeRef;
  T adviserParameters;
  Status toStatus;
  Status fromStatus;
  FailureInfo failureInfo;
}