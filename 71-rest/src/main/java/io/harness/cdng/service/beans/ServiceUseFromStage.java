package io.harness.cdng.service.beans;

import io.swagger.annotations.ApiModel;
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
  @ApiModel(value = "ServiceOverrides")
  public static class Overrides {
    String name;
    String description;
  }
}
