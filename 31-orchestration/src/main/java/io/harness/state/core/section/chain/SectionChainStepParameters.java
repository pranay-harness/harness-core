package io.harness.state.core.section.chain;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
public class SectionChainStepParameters implements StepParameters {
  @Singular List<String> childNodeIds;
}
