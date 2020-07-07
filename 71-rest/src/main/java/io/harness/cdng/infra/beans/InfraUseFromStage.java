package io.harness.cdng.infra.beans;

import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.InfrastructureSpec;
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
  public static class Overrides implements Serializable {
    EnvironmentYaml environment;
    InfrastructureSpec infrastructureSpec;
  }
}
