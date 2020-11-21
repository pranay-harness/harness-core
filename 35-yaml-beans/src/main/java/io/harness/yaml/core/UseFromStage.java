package io.harness.yaml.core;

import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UseFromStage implements Serializable {
  // Stage identifier of the stage to select from.
  @NotNull String stage;
}
