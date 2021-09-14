/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.dto.secrets;

import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.ng.core.models.SSHCredentialSpec;
import io.harness.ng.core.models.SSHKeyCredential;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("KeyReference")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SSHKeyReferenceCredentialDTO extends SSHCredentialSpecDTO implements DecryptableEntity {
  @NotNull private String userName;
  @ApiModelProperty(dataType = "string") @SecretReference private SecretRefData key;
  @ApiModelProperty(dataType = "string") @SecretReference private SecretRefData encryptedPassphrase;

  @Override
  public SSHCredentialSpec toEntity() {
    return SSHKeyCredential.builder()
        .userName(getUserName())
        .encryptedPassphrase(getEncryptedPassphrase())
        .key(getKey())
        .build();
  }
}
