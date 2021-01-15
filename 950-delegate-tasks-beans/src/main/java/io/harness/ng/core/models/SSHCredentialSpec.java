package io.harness.ng.core.models;

import io.harness.ng.core.dto.secrets.SSHCredentialSpecDTO;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "credentialType",
    visible = true)
public abstract class SSHCredentialSpec {
  public abstract SSHCredentialSpecDTO toDTO();
}
