package io.harness.ng.core.models;

import io.harness.ng.core.dto.secrets.TGTGenerationSpecDTO;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "tgtGenerationMethod",
    visible = true)
public abstract class TGTGenerationSpec {
  public abstract TGTGenerationSpecDTO toDTO();
}
