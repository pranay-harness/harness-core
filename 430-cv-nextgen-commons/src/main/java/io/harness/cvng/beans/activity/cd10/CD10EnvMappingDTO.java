package io.harness.cvng.beans.activity.cd10;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
@Data
@SuperBuilder
@NoArgsConstructor
public class CD10EnvMappingDTO extends CD10MappingDTO {
  private String envId;
  private String envIdentifier;
}
