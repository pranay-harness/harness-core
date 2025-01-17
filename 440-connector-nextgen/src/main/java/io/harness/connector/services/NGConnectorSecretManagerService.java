/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.services;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;

import software.wings.beans.CustomSecretNGManagerConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@OwnedBy(PL)
public interface NGConnectorSecretManagerService {
  SecretManagerConfigDTO getUsingIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, boolean maskSecrets);

  ConnectorDTO decrypt(
      String accountIdentifier, String projectIdentifier, String orgIdentifier, ConnectorDTO connectorConfig);

  default ConnectorDTO getConnectorDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    throw new RuntimeException("Can not find the connector dto. ");
  }

  void resolveSecretManagerScriptSecrets(String accountIdentifier, String path,
      CustomSecretNGManagerConfig encryptionConfig, SecretManagerConfigDTO secretManagerConfigDTO);

  String getPerpetualTaskId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  void resetHeartBeatTask(String accountId, String taskId);

  SecretManagerConfigDTO getLocalConfigDTO(String accountIdentifier);

  Map<String, SecretRefData> getSecretsForDecryptableEntities(List<DecryptableEntity> decryptableEntities);

  Optional<SecretResponseWrapper> getSecretOptionalFromSecretRef(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, SecretRefData secretRefData);

  void checkIfDecryptionIsPossible(
      String accountIdentifier, ConnectorInfoDTO connectorInfoDTO, boolean shouldBelongToHarnessSM);

  void validateSecretManagerCredentialsAreInHarnessSM(String accountIdentifier, ConnectorDTO connectorDTO,
      Set<String> credentialSecretIdentifiers, boolean validateSMCredentialsStoredInHarnessSM);
}
