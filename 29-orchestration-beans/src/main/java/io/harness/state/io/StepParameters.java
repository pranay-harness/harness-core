package io.harness.state.io;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.timeout.TimeoutObtainment;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutParameters;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutTrackerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@OwnedBy(CDC)
@Redesign
public interface StepParameters {
  Duration DEFAULT_TIMEOUT = Duration.ofDays(10);

  default List<TimeoutObtainment> fetchTimeouts() {
    return Collections.singletonList(
        TimeoutObtainment.builder()
            .type(AbsoluteTimeoutTrackerFactory.DIMENSION)
            .parameters(AbsoluteTimeoutParameters.builder().timeoutMillis(DEFAULT_TIMEOUT.toMillis()).build())
            .build());
  }
}
