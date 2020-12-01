package io.harness.steps.fork;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.List;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
@TypeAlias("forkStepParameters")
public class ForkStepParameters implements StepParameters {
  @Singular @Size(min = 2) List<String> parallelNodeIds;
}
