package io.harness.cdng.infra.beans;

import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.InfrastructureDef;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class InfraUseFromStage implements Serializable {
  // Stage identifier of the stage to select from.
  @NotNull String stage;
  Overrides overrides;

  @Value
  @Builder
  @ApiModel(value = "InfraOverrides")
  public static class Overrides implements Serializable {
    EnvironmentYaml environment;
    InfrastructureDef infrastructureDef;
  }
}
