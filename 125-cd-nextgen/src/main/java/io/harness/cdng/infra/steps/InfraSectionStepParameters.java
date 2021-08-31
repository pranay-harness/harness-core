package io.harness.cdng.infra.steps;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.beans.InfraUseFromStage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
@TypeAlias("infraSectionStepParameters")
@RecasterAlias("io.harness.cdng.infra.steps.InfraSectionStepParameters")
public class InfraSectionStepParameters implements StepParameters {
  String childNodeID;
  private EnvironmentYaml environment;
  private ParameterField<String> environmentRef;
  private InfraUseFromStage useFromStage;
}
