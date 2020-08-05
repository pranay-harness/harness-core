package io.harness.steps.resourcerestraint;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.state.io.StepParameters;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.beans.HoldingScope;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import javax.validation.constraints.NotNull;

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
  public Duration timeout() {
    return Duration.ofMillis(timeoutInMillis);
  }
}
