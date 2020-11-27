package io.harness.cvng.cd10.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
@Data
@SuperBuilder
@JsonTypeName("ENV_MAPPING")
@NoArgsConstructor
public class CD10EnvMappingDTO extends CD10MappingDTO {
  private String envId;
  private String envIdentifier;
  @Override
  public MappingType getType() {
    return MappingType.ENV_MAPPING;
  }
}
