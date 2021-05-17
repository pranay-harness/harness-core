package io.harness.steps.barriers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.SpecParameters;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PIPELINE)
@Value
@Builder
@TypeAlias("barrierSpecParameters")
@RecasterAlias("barrierSpecParameters")
public class BarrierSpecParameters implements SpecParameters {
  String barrierRef;
}
