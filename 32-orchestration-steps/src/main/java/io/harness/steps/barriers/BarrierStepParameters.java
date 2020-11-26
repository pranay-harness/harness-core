package io.harness.steps.barriers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.state.io.StepParameters;
import io.harness.timeout.TimeoutObtainment;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutParameters;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutTrackerFactory;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@TypeAlias("barrierStepParameters")
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
