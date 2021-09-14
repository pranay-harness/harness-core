/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.secretmanagerclient.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@OwnedBy(PL)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(exclude = {"authToken", "secretId", "sinkPath"})
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VaultConfigDTO extends SecretManagerConfigDTO {
  private String authToken;
  private String basePath;
  private String namespace;
  private String sinkPath;
  private boolean useVaultAgent;
  private String vaultUrl;
  @JsonProperty("readOnly") private boolean isReadOnly;
  private long renewalIntervalMinutes;
  private String secretEngineName;
  private String appRoleId;
  private String secretId;
  private int secretEngineVersion;
  private boolean engineManuallyEntered;
  private Set<String> delegateSelectors;
}
