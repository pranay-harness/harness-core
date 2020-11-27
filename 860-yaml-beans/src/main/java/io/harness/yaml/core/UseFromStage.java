package io.harness.yaml.core;

import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.yaml.core.useFromStage")
public class UseFromStage implements Serializable {
  // Stage identifier of the stage to select from.
  @NotNull String stage;
}
