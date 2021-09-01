package io.harness.steps.barriers.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@TypeAlias("barrierOutcome")
@JsonTypeName("barrierOutcome")
public class BarrierOutcome implements Outcome {
  String message;
  String barrierRef;
}
