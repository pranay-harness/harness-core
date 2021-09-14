/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.models;

import io.harness.ng.core.dto.secrets.BaseSSHSpecDTO;
import io.harness.ng.core.dto.secrets.KerberosConfigDTO;
import io.harness.ng.core.dto.secrets.TGTGenerationMethod;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("Kerberos")
public class KerberosConfig extends BaseSSHSpec {
  private String principal;
  private String realm;
  private TGTGenerationMethod tgtGenerationMethod;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      property = "tgtGenerationMethod", visible = true)
  private TGTGenerationSpec spec;

  @Override
  public BaseSSHSpecDTO toDTO() {
    return KerberosConfigDTO.builder()
        .principal(getPrincipal())
        .realm(getRealm())
        .tgtGenerationMethod(getTgtGenerationMethod())
        .spec(Optional.ofNullable(getSpec()).map(TGTGenerationSpec::toDTO).orElse(null))
        .build();
  }

  @Builder
  public KerberosConfig(
      String principal, String realm, TGTGenerationMethod tgtGenerationMethod, TGTGenerationSpec spec) {
    this.principal = principal;
    this.realm = realm;
    this.tgtGenerationMethod = tgtGenerationMethod;
    this.spec = spec;
  }
}
