package io.harness.ng.core.models;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.ng.core.dto.secrets.BaseSSHSpecDTO;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "authScheme", visible = true)
public abstract class BaseSSHSpec {
  public abstract BaseSSHSpecDTO toDTO();
}
