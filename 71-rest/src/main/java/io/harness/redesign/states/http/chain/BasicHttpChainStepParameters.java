package io.harness.redesign.states.http.chain;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.redesign.states.http.BasicHttpStepParameters;
import io.harness.state.io.StepParameters;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class BasicHttpChainStepParameters implements StepParameters {
  @Singular List<BasicHttpStepParameters> linkParameters;
}
