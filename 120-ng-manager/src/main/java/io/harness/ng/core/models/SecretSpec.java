package io.harness.ng.core.models;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.ng.core.dto.secrets.SecretSpecDTO;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true)
public abstract class SecretSpec {
  public abstract SecretSpecDTO toDTO();
}
