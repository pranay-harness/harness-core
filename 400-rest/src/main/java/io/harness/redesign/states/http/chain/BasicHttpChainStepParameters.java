package io.harness.redesign.states.http.chain;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.redesign.states.http.BasicHttpStepParameters;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

import static io.harness.annotations.dev.HarnessTeam.CDC;

@OwnedBy(CDC)
@Value
@Builder
public class BasicHttpChainStepParameters implements StepParameters {
  @Singular List<BasicHttpStepParameters> linkParameters;
}
