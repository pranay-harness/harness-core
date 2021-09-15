/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorValidationResult;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataRequestDTO;

@OwnedBy(PL)
public interface NGSecretManagerService {
  SecretManagerConfigDTO createSecretManager(SecretManagerConfigDTO secretManagerConfigDTO);

  SecretManagerConfigDTO updateSecretManager(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, SecretManagerConfigUpdateDTO secretManagerConfigUpdateDTO);

  boolean deleteSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  SecretManagerConfigDTO getSecretManager(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, boolean maskSecrets);

  ConnectorValidationResult validate(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  SecretManagerConfigDTO getGlobalSecretManager(String accountIdentifier);

  SecretManagerMetadataDTO getMetadata(String accountIdentifier, SecretManagerMetadataRequestDTO requestDTO);
}
