/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.secretmanagerclient.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.secretmanagerclient.dto.azurekeyvault.AzureKeyVaultMetadataSpecDTO;
import io.harness.security.encryption.EncryptionType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
public class SecretManagerMetadataDTO {
  private EncryptionType encryptionType;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "encryptionType",
      visible = true)
  @JsonSubTypes(value =
      {
        @JsonSubTypes.Type(name = "VAULT", value = VaultMetadataSpecDTO.class)
        , @JsonSubTypes.Type(name = "AZURE_VAULT", value = AzureKeyVaultMetadataSpecDTO.class)
      })
  private SecretManagerMetadataSpecDTO spec;

  @Builder
  public SecretManagerMetadataDTO(EncryptionType encryptionType, SecretManagerMetadataSpecDTO spec) {
    this.encryptionType = encryptionType;
    this.spec = spec;
  }
}
