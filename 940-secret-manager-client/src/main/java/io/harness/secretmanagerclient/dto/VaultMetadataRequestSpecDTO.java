/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.secretmanagerclient.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.AccessType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("VAULT")
public class VaultMetadataRequestSpecDTO extends SecretManagerMetadataRequestSpecDTO {
  @NotNull private String url;
  @NotNull private AccessType accessType;
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "accessType", visible = true)
  @JsonSubTypes(value =
      {
        @JsonSubTypes.Type(name = "APP_ROLE", value = VaultAppRoleCredentialDTO.class)
        , @JsonSubTypes.Type(name = "TOKEN", value = VaultAuthTokenCredentialDTO.class),
            @JsonSubTypes.Type(name = "VAULT_AGENT", value = VaultAgentCredentialDTO.class)
      })
  @Valid
  private VaultCredentialDTO spec;
  private Set<String> delegateSelectors;
  private String namespace;

  @Builder
  public VaultMetadataRequestSpecDTO(
      String url, AccessType accessType, VaultCredentialDTO spec, Set<String> delegateSelectors) {
    this.url = url;
    this.accessType = accessType;
    this.spec = spec;
    this.delegateSelectors = delegateSelectors;
  }
}
