package io.harness.ng.core.models;

import io.harness.ng.core.dto.secrets.BaseSSHSpecDTO;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
public abstract class BaseSSHSpec {
  public abstract BaseSSHSpecDTO toDTO();
}
