/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.models;

import io.harness.ng.core.dto.secrets.BaseSSHSpecDTO;
import io.harness.ng.core.dto.secrets.SSHConfigDTO;
import io.harness.ng.core.dto.secrets.SSHCredentialType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SSH")
public class SSHConfig extends BaseSSHSpec {
  private SSHCredentialType credentialType;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "credentialType",
      visible = true)
  private SSHCredentialSpec spec;

  @Override
  public BaseSSHSpecDTO toDTO() {
    return SSHConfigDTO.builder().credentialType(getCredentialType()).spec(getSpec().toDTO()).build();
  }
}
