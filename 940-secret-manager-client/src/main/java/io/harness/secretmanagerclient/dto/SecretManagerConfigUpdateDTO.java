/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.secretmanagerclient.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsConfigUpdateDTO;
import io.harness.security.encryption.EncryptionType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@OwnedBy(PL)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "encryptionType",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(name = "VAULT", value = VaultConfigUpdateDTO.class)
  , @JsonSubTypes.Type(name = "GCP_KMS", value = GcpKmsConfigUpdateDTO.class),
      @JsonSubTypes.Type(name = "AWS_KMS", value = AwsKmsConfigUpdateDTO.class)
})
public class SecretManagerConfigUpdateDTO {
  private String name;
  private Map<String, String> tags;
  private EncryptionType encryptionType;
  private boolean isDefault;
  private String description;
}
