/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.ng.core.models;

import io.harness.ng.core.dto.secrets.SecretFileSpecDTO;
import io.harness.ng.core.dto.secrets.SecretSpecDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SecretFile")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecretFileSpec extends SecretSpec {
  private String secretManagerIdentifier;

  @Override
  public SecretSpecDTO toDTO() {
    return SecretFileSpecDTO.builder().secretManagerIdentifier(getSecretManagerIdentifier()).build();
  }
}
