/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.models;

import io.harness.ng.core.dto.secrets.SSHAuthDTO;
import io.harness.secretmanagerclient.SSHAuthScheme;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SSHAuth {
  private SSHAuthScheme type;
  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
  BaseSSHSpec sshSpec;

  public SSHAuthDTO toDTO() {
    return SSHAuthDTO.builder().type(type).spec(sshSpec.toDTO()).build();
  }

  @Builder
  public SSHAuth(SSHAuthScheme type, BaseSSHSpec sshSpec) {
    this.type = type;
    this.sshSpec = sshSpec;
  }
}
