package io.harness.cvng.cd10.entities;

import io.harness.cvng.cd10.beans.CD10ServiceMappingDTO;

import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@FieldNameConstants(innerTypeName = "CD10ServiceMappingKeys")
@SuperBuilder
public class CD10ServiceMapping extends CD10Mapping {
  private String serviceId;
  private String serviceIdentifier;

  public CD10ServiceMappingDTO toDTO() {
    return CD10ServiceMappingDTO.builder()
        .appId(getAppId())
        .serviceId(serviceId)
        .serviceIdentifier(serviceIdentifier)
        .build();
  }
}
