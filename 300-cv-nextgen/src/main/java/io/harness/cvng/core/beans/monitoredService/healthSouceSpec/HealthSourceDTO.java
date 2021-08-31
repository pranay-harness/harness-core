package io.harness.cvng.core.beans.monitoredService.healthSouceSpec;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.monitoredService.HealthSource;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Data
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HealthSourceDTO {
  String identifier;
  String name;
  DataSourceType type;

  public static HealthSourceDTO toHealthSourceDTO(HealthSource healthSource) {
    return HealthSourceDTO.builder()
        .name(healthSource.getName())
        .identifier(healthSource.getIdentifier())
        .type(healthSource.getSpec().getType())
        .build();
  }
}
