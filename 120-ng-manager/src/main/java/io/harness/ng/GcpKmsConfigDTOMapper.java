package io.harness.ng;

import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.secretmanagerclient.dto.GcpKmsConfigDTO;
import io.harness.secretmanagerclient.dto.GcpKmsConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.GcpKmsConnectorDTO;
import io.harness.security.encryption.EncryptionType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GcpKmsConfigDTOMapper {
  public static GcpKmsConfigDTO getGcpKmsConfigDTO(
      String accountIdentifier, ConnectorRequestDTO connectorRequestDTO, GcpKmsConnectorDTO gcpKmsConnectorDTO) {
    return GcpKmsConfigDTO.builder()
        .region(gcpKmsConnectorDTO.getRegion())
        .keyName(gcpKmsConnectorDTO.getKeyName())
        .keyRing(gcpKmsConnectorDTO.getKeyRing())
        .credentials(gcpKmsConnectorDTO.getCredentials())
        .projectId(gcpKmsConnectorDTO.getProjectId())
        .isDefault(gcpKmsConnectorDTO.isDefault())
        .encryptionType(EncryptionType.GCP_KMS)

        .name(connectorRequestDTO.getName())
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(connectorRequestDTO.getOrgIdentifier())
        .projectIdentifier(connectorRequestDTO.getProjectIdentifier())
        .tags(connectorRequestDTO.getTags())
        .identifier(connectorRequestDTO.getIdentifier())
        .description(connectorRequestDTO.getDescription())
        .build();
  }

  public static GcpKmsConfigUpdateDTO getGcpKmsConfigUpdateDTO(
      ConnectorRequestDTO connectorRequestDTO, GcpKmsConnectorDTO gcpKmsConnectorDTO) {
    return GcpKmsConfigUpdateDTO.builder()
        .region(gcpKmsConnectorDTO.getRegion())
        .keyName(gcpKmsConnectorDTO.getKeyName())
        .keyRing(gcpKmsConnectorDTO.getKeyRing())
        .credentials(gcpKmsConnectorDTO.getCredentials())
        .projectId(gcpKmsConnectorDTO.getProjectId())
        .isDefault(gcpKmsConnectorDTO.isDefault())
        .encryptionType(EncryptionType.GCP_KMS)

        .tags(connectorRequestDTO.getTags())
        .description(connectorRequestDTO.getDescription())
        .build();
  }
}
