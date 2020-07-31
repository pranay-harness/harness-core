package io.harness.steps.resourcerestraint;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;

@OwnedBy(CDC)
@Value
@Builder
public class ResourceRestraintStepParameters implements StepParameters {
  String resourceConstraintId;
  int requiredResourceUsage;
  String resourceUnit;

  long timeoutInMillis;

  @Override
  public Duration timeout() {
    return Duration.ofMillis(timeoutInMillis);
  }
}
