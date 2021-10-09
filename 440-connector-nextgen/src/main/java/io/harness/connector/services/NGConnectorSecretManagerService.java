package io.harness.connector.services;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;

@OwnedBy(PL)
public interface NGConnectorSecretManagerService {
  SecretManagerConfigDTO getUsingIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, boolean maskSecrets);

  ConnectorDTO decrypt(
      String accountIdentifier, String projectIdentifier, String orgIdentifier, ConnectorDTO connectorConfig);
}
