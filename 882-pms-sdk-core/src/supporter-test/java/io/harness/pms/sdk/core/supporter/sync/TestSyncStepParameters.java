package io.harness.pms.sdk.core.supporter.sync;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@TypeAlias("dummyStepParameters")
public class TestSyncStepParameters implements StepParameters {
  String logExpression;
}
