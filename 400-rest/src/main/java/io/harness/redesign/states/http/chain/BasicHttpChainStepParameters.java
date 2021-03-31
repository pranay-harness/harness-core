package io.harness.redesign.states.http.chain;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.redesign.states.http.BasicHttpStepParameters;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@TargetModule(HarnessModule._860_ORCHESTRATION_STEPS)
public class BasicHttpChainStepParameters implements StepParameters {
  @Singular List<BasicHttpStepParameters> linkParameters;
}
