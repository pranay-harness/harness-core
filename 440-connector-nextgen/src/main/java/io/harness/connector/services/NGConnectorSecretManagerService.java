package io.harness.connector.services;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.ConnectorDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;

@OwnedBy(PL)
public interface NGConnectorSecretManagerService {
  SecretManagerConfigDTO getUsingIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, boolean maskSecrets);

  DecryptableEntity decryptUsingManager(DecryptableEntity decryptableEntity, List<EncryptedDataDetail> encryptedDataDetailList, String accountIdentifier);

  ConnectorDTO decrypt(
      String accountIdentifier, String projectIdentifier, String orgIdentifier, ConnectorDTO connectorConfig);
}
