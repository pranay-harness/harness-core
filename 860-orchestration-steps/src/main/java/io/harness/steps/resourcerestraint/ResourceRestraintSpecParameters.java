package io.harness.steps.resourcerestraint;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.beans.HoldingScope;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@TypeAlias("resourceRestraintSpecParameters")
public class ResourceRestraintSpecParameters implements SpecParameters {
  String name;
  @NotNull String resourceUnit;
  @NotNull AcquireMode acquireMode;
  @NotNull int permits;
  @NotNull HoldingScope holdingScope;
}
