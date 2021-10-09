package io.harness.connector.services;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.entities.embedded.vaultconnector.VaultConnector;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.helpers.ext.vault.SecretEngineSummary;
import io.harness.helpers.ext.vault.VaultAppRoleLoginResult;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataRequestDTO;

import software.wings.beans.BaseVaultConfig;

import java.util.List;

@OwnedBy(PL)
public interface NGVaultService {
  void renewToken(VaultConnector vaultConnector);

  List<SecretEngineSummary> listSecretEngines(BaseVaultConfig baseVaultConfig);

  void renewAppRoleClientToken(VaultConnector vaultConnector);

  VaultAppRoleLoginResult appRoleLogin(BaseVaultConfig vaultConfig);

  SecretManagerMetadataDTO getListOfEngines(String accountIdentifier, SecretManagerMetadataRequestDTO requestDTO);

  void processAppRole(ConnectorDTO connectorDTO, ConnectorConfigDTO existingConnectorConfigDTO,
      String accountIdentifier, boolean create);
}
