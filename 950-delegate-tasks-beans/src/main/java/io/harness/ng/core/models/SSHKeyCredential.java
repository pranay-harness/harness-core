/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.models;

import io.harness.encryption.SecretRefData;
import io.harness.ng.core.dto.secrets.SSHCredentialSpecDTO;
import io.harness.ng.core.dto.secrets.SSHKeyReferenceCredentialDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("KeyReference")
public class SSHKeyCredential extends SSHCredentialSpec {
  private String userName;
  private SecretRefData encryptedPassphrase;
  private SecretRefData key;

  @Override
  public SSHCredentialSpecDTO toDTO() {
    return SSHKeyReferenceCredentialDTO.builder()
        .userName(getUserName())
        .encryptedPassphrase(getEncryptedPassphrase())
        .key(getKey())
        .build();
  }
}
