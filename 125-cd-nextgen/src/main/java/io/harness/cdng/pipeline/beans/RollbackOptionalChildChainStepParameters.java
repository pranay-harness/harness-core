package io.harness.cdng.pipeline.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("cdng.pipeline.beans.RollbackOptionalChildChainStepParameters")
@RecasterAlias("io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters")
public class RollbackOptionalChildChainStepParameters implements StepParameters {
  @Singular List<RollbackNode> childNodes;
}
