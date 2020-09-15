package io.harness.steps.barriers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.state.io.StepParameters;
import io.harness.timeout.TimeoutObtainment;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutParameters;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutTrackerFactory;
import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
public class BarrierStepParameters implements StepParameters {
  String identifier;
  long timeoutInMillis;

  @Override
  public List<TimeoutObtainment> fetchTimeouts() {
    return Collections.singletonList(
        TimeoutObtainment.builder()
            .type(AbsoluteTimeoutTrackerFactory.DIMENSION)
            .parameters(AbsoluteTimeoutParameters.builder().timeoutMillis(timeoutInMillis).build())
            .build());
  }
}
