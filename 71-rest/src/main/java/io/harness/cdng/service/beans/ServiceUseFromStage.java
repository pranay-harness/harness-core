package io.harness.cdng.service.beans;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class ServiceUseFromStage implements Serializable {
  // Stage identifier of the stage to select from.
  @NotNull String stage;
  Overrides overrides;

  @Value
  @Builder
  public static class Overrides {
    String displayName;
    String description;
  }
}
