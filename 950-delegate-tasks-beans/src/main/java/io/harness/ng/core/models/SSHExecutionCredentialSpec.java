/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.models;

import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SecretSpecDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SSHKey")
public class SSHExecutionCredentialSpec extends SecretSpec {
  int port;
  SSHAuth auth;

  @Override
  public SecretSpecDTO toDTO() {
    return SSHKeySpecDTO.builder()
        .port(getPort())
        .auth(Optional.ofNullable(auth).map(SSHAuth::toDTO).orElse(null))
        .build();
  }
}
