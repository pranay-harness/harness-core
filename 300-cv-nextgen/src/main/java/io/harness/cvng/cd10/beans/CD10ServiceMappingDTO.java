package io.harness.cvng.cd10.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@JsonTypeName("SERVICE_MAPPING")
@NoArgsConstructor
public class CD10ServiceMappingDTO extends CD10MappingDTO {
  private String serviceId;
  private String serviceIdentifier;
  @Override
  public MappingType getType() {
    return MappingType.SERVICE_MAPPING;
  }
}
