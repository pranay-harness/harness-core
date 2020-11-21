package io.harness.steps.fork;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.state.io.StepParameters;

import java.util.List;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class ForkStepParameters implements StepParameters {
  @Singular @Size(min = 2) List<String> parallelNodeIds;
}
