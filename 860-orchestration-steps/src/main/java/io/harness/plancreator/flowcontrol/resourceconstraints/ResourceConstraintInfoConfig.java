package io.harness.plancreator.flowcontrol.resourceconstraints;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("resourceConstraintInfoConfig")
public class ResourceConstraintInfoConfig {
  @NotNull String identifier;
  @NotNull String name;
}
