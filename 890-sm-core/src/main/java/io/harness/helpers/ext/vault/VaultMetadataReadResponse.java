/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.helpers.ext.vault;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * This DTO class is for reading Vault secret metadata, especially the 'versions' information for constructing the
 * secret change log to be presented in the UI.
 *
 * More details on Vault versions metadata can be found at:
 *  https://learn.hashicorp.com/vault/secrets-management/sm-versioned-kvß
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@OwnedBy(PL)
public class VaultMetadataReadResponse {
  private VaultSecretMetadata data;
}
