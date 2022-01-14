package io.harness.pms.sdk.core.supporter.children;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.List;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class TestChildrenStepParameters implements StepParameters {
  @Singular @Size(min = 2) List<String> parallelNodeIds;
}
