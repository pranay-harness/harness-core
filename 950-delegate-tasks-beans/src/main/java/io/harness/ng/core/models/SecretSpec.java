package io.harness.ng.core.models;

import io.harness.ng.core.dto.secrets.SecretSpecDTO;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true)
public abstract class SecretSpec {
  public abstract SecretSpecDTO toDTO();
}
