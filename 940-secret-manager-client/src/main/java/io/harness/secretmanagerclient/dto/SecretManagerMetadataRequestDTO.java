/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.secretmanagerclient.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.security.encryption.EncryptionType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
public class SecretManagerMetadataRequestDTO implements DecryptableEntity {
  @NotNull private EncryptionType encryptionType;
  private String orgIdentifier;
  private String projectIdentifier;
  @NotNull private String identifier;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "encryptionType",
      visible = true)
  @JsonSubTypes(value = { @JsonSubTypes.Type(name = "VAULT", value = VaultMetadataRequestSpecDTO.class) })
  @Valid
  @NotNull
  private SecretManagerMetadataRequestSpecDTO spec;

  @Builder
  public SecretManagerMetadataRequestDTO(EncryptionType encryptionType, String orgIdentifier, String projectIdentifier,
      String identifier, SecretManagerMetadataRequestSpecDTO spec) {
    this.encryptionType = encryptionType;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.identifier = identifier;
    this.spec = spec;
  }
}
