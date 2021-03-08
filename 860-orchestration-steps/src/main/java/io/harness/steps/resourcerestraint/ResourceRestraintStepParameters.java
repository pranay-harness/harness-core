package io.harness.steps.resourcerestraint;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.beans.HoldingScope;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@TypeAlias("resourceRestraintStepParameters")
public class ResourceRestraintStepParameters implements StepParameters {
  @NotNull String resourceRestraintId;
  @NotNull String resourceUnit;
  @NotNull String claimantId;
  @NotNull AcquireMode acquireMode;
  @NotNull int permits;
  @NotNull HoldingScope holdingScope;
}
