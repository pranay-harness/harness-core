package io.harness.ng;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.AwsKmsConfigDTOMapper.getAwsKmsConfigDTO;
import static io.harness.ng.GcpKmsConfigDTOMapper.getGcpKmsConfigDTO;
import static io.harness.ng.LocalConfigDTOMapper.getLocalConfigDTO;
import static io.harness.ng.VaultConfigDTOMapper.getVaultConfigDTO;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorDTO;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;
import io.harness.delegate.beans.connector.localconnector.LocalConnectorDTO;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class SecretManagerConfigDTOMapper {
  public static SecretManagerConfigDTO fromConnectorDTO(
      String accountIdentifier, ConnectorDTO connectorRequestDTO, ConnectorConfigDTO connectorConfigDTO) {
    ConnectorInfoDTO connector = connectorRequestDTO.getConnectorInfo();
    switch (connector.getConnectorType()) {
      case VAULT:
        return getVaultConfigDTO(accountIdentifier, connectorRequestDTO, (VaultConnectorDTO) connectorConfigDTO);
      case GCP_KMS:
        return getGcpKmsConfigDTO(accountIdentifier, connectorRequestDTO, (GcpKmsConnectorDTO) connectorConfigDTO);
      case AWS_KMS:
        return getAwsKmsConfigDTO(accountIdentifier, connectorRequestDTO, (AwsKmsConnectorDTO) connectorConfigDTO);
      case LOCAL:
        return getLocalConfigDTO(accountIdentifier, connectorRequestDTO, (LocalConnectorDTO) connectorConfigDTO);
      default:
        throw new IllegalArgumentException("This is not a valid secret manager type: " + connector.getConnectorType());
    }
  }
}
