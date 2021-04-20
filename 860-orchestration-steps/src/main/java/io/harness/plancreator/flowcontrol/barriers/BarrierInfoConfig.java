package io.harness.plancreator.flowcontrol.barriers;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("barrierInfoConfig")
public class BarrierInfoConfig {
  @NotNull String identifier;
  @NotNull String name;
}
