package io.harness.ng;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;
import io.harness.secretmanagerclient.dto.GcpKmsConfigDTO;
import io.harness.secretmanagerclient.dto.GcpKmsConfigUpdateDTO;
import io.harness.security.encryption.EncryptionType;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class GcpKmsConfigDTOMapper {
  public static GcpKmsConfigDTO getGcpKmsConfigDTO(
      String accountIdentifier, ConnectorDTO connectorRequestDTO, GcpKmsConnectorDTO gcpKmsConnectorDTO) {
    ConnectorInfoDTO connector = connectorRequestDTO.getConnectorInfo();
    return GcpKmsConfigDTO.builder()
        .region(gcpKmsConnectorDTO.getRegion())
        .keyName(gcpKmsConnectorDTO.getKeyName())
        .keyRing(gcpKmsConnectorDTO.getKeyRing())
        .credentials(gcpKmsConnectorDTO.getCredentials().getDecryptedValue())
        .projectId(gcpKmsConnectorDTO.getProjectId())
        .isDefault(false)
        .encryptionType(EncryptionType.GCP_KMS)
        .delegateSelectors(gcpKmsConnectorDTO.getDelegateSelectors())

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
        .credentials(gcpKmsConnectorDTO.getCredentials().getDecryptedValue())
        .projectId(gcpKmsConnectorDTO.getProjectId())
        .isDefault(false)
        .name(connector.getName())
        .encryptionType(EncryptionType.GCP_KMS)

        .tags(connector.getTags())
        .description(connector.getDescription())
        .build();
  }
}
