package io.harness.steps.resourcerestraint;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.state.io.StepParameters;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.beans.HoldingScope;
import io.harness.timeout.TimeoutObtainment;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutParameters;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutTrackerFactory;

import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class ResourceRestraintStepParameters implements StepParameters {
  @NotNull String resourceRestraintId;
  @NotNull String resourceUnit;
  @NotNull String claimantId;
  @NotNull AcquireMode acquireMode;
  @NotNull int permits;
  @NotNull HoldingScope holdingScope;

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
