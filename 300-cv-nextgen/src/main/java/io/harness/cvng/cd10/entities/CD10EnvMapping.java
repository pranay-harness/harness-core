package io.harness.cvng.cd10.entities;

import io.harness.cvng.cd10.beans.CD10EnvMappingDTO;

import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
@FieldNameConstants(innerTypeName = "CD10EnvMappingKeys")
@SuperBuilder
public class CD10EnvMapping extends CD10Mapping {
  private String envId;
  private String envIdentifier;

  public CD10EnvMappingDTO toDTO() {
    return CD10EnvMappingDTO.builder().appId(getAppId()).envId(envId).envIdentifier(envIdentifier).build();
  }
}
