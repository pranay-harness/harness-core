/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.models;

import io.harness.encryption.SecretRefData;
import io.harness.ng.core.dto.secrets.SSHCredentialSpecDTO;
import io.harness.ng.core.dto.secrets.SSHKeyPathCredentialDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("KeyPath")
public class SSHKeyPathCredential extends SSHCredentialSpec {
  private String userName;
  private String keyPath;
  private SecretRefData encryptedPassphrase;

  @Override
  public SSHCredentialSpecDTO toDTO() {
    return SSHKeyPathCredentialDTO.builder()
        .userName(getUserName())
        .keyPath(getKeyPath())
        .encryptedPassphrase(getEncryptedPassphrase())
        .build();
  }
}
