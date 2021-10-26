package io.harness.cvng.servicelevelobjective.beans;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class ServiceLevelIndicatorDTO {
  String name;
  String identifier;
  @NonNull ServiceLevelIndicatorType type;
  @NonNull ServiceLevelIndicatorSpec spec;
}
