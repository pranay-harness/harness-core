/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.models;

import io.harness.encryption.SecretRefData;
import io.harness.ng.core.dto.secrets.SSHCredentialSpecDTO;
import io.harness.ng.core.dto.secrets.SSHPasswordCredentialDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("Password")
public class SSHPasswordCredential extends SSHCredentialSpec {
  private String userName;
  private SecretRefData password;

  @Override
  public SSHCredentialSpecDTO toDTO() {
    return SSHPasswordCredentialDTO.builder().userName(getUserName()).password(getPassword()).build();
  }
}
