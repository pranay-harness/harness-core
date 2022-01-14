package io.harness.cf.pipeline;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CF)
@Value
@Builder
@TypeAlias("featureFlagStageStepParameters")
@RecasterAlias("io.harness.cf.pipeline.FeatureFlagStageStepParameters")
public class FeatureFlagStageStepParameters implements StepParameters {
  String identifier;
  String name;
  String type;
  ParameterField<String> description;
}
