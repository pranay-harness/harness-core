package io.harness.cdng.infra.steps;

import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.InfrastructureDef;
import io.harness.cdng.infra.beans.InfraUseFromStage;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@TypeAlias("infraSectionStepParameters")
@EqualsAndHashCode(callSuper = true)
public class InfraSectionStepParameters extends PipelineInfrastructure implements StepParameters {
  String childNodeID;

  @Builder(builderMethodName = "newBuilder")
  public InfraSectionStepParameters(InfrastructureDef infrastructureDefinition, InfraUseFromStage useFromStage,
      EnvironmentYaml environment, String metadata, String childNodeID) {
    super(infrastructureDefinition, useFromStage, environment, metadata);
    this.childNodeID = childNodeID;
  }

  public static InfraSectionStepParameters getStepParameters(
      PipelineInfrastructure infrastructure, String childNodeID) {
    return new InfraSectionStepParameters(infrastructure.getInfrastructureDefinition(),
        infrastructure.getUseFromStage(), infrastructure.getEnvironment(), infrastructure.getMetadata(), childNodeID);
  }
}
