/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
