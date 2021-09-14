/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.dto.secrets;

import io.harness.ng.core.models.SSHAuth;
import io.harness.secretmanagerclient.SSHAuthScheme;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SSHAuthDTO {
  @JsonProperty("type") @NotNull private SSHAuthScheme authScheme;

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true)
  @NotNull
  @Valid
  private BaseSSHSpecDTO spec;

  @Builder
  public SSHAuthDTO(SSHAuthScheme type, BaseSSHSpecDTO spec) {
    this.authScheme = type;
    this.spec = spec;
  }

  public SSHAuth toEntity() {
    return SSHAuth.builder().type(authScheme).sshSpec(spec.toEntity()).build();
  }
}
