package io.harness.cvng.servicelevelobjective.beans;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceLevelIndicatorDTO {
  String name;
  String identifier;
  @NotNull ServiceLevelIndicatorType type;
  @NotNull ServiceLevelIndicatorSpec spec;
}
