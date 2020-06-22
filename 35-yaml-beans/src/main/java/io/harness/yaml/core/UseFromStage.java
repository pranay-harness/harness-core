package io.harness.yaml.core;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class UseFromStage implements Serializable {
  // Stage identifier of the stage to select from.
  @NotNull String stage;
}
