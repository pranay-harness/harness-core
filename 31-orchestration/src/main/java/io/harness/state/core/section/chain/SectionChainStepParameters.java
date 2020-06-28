package io.harness.state.core.section.chain;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
@TypeAlias("sectionChainStepParameters")
public class SectionChainStepParameters implements StepParameters {
  @Singular List<String> childNodeIds;
}
