package io.harness.ng;

import io.harness.delegate.beans.connector.apis.dto.ConnectorDTO;
import io.harness.delegate.beans.connector.apis.dto.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;
import io.harness.secretmanagerclient.dto.GcpKmsConfigDTO;
import io.harness.secretmanagerclient.dto.GcpKmsConfigUpdateDTO;
import io.harness.security.encryption.EncryptionType;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GcpKmsConfigDTOMapper {
  public static GcpKmsConfigDTO getGcpKmsConfigDTO(
      String accountIdentifier, ConnectorDTO connectorRequestDTO, GcpKmsConnectorDTO gcpKmsConnectorDTO) {
    ConnectorInfoDTO connector = connectorRequestDTO.getConnectorInfo();
    return GcpKmsConfigDTO.builder()
        .region(gcpKmsConnectorDTO.getRegion())
        .keyName(gcpKmsConnectorDTO.getKeyName())
        .keyRing(gcpKmsConnectorDTO.getKeyRing())
        .credentials(gcpKmsConnectorDTO.getCredentials())
        .projectId(gcpKmsConnectorDTO.getProjectId())
        .isDefault(false)
        .encryptionType(EncryptionType.GCP_KMS)

        .name(connector.getName())
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(connector.getOrgIdentifier())
        .projectIdentifier(connector.getProjectIdentifier())
        .tags(connector.getTags())
        .identifier(connector.getIdentifier())
        .description(connector.getDescription())
        .harnessManaged(gcpKmsConnectorDTO.isHarnessManaged())
        .build();
  }

  public static GcpKmsConfigUpdateDTO getGcpKmsConfigUpdateDTO(
      ConnectorDTO connectorRequestDTO, GcpKmsConnectorDTO gcpKmsConnectorDTO) {
    ConnectorInfoDTO connector = connectorRequestDTO.getConnectorInfo();
    return GcpKmsConfigUpdateDTO.builder()
        .region(gcpKmsConnectorDTO.getRegion())
        .keyName(gcpKmsConnectorDTO.getKeyName())
        .keyRing(gcpKmsConnectorDTO.getKeyRing())
        .credentials(gcpKmsConnectorDTO.getCredentials())
        .projectId(gcpKmsConnectorDTO.getProjectId())
        .isDefault(false)
        .encryptionType(EncryptionType.GCP_KMS)

        .tags(connector.getTags())
        .description(connector.getDescription())
        .build();
  }
}
