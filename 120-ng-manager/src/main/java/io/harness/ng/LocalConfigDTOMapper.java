package io.harness.ng;

import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.secretmanagerclient.dto.LocalConfigDTO;
import io.harness.secretmanagerclient.dto.LocalConnectorDTO;
import io.harness.security.encryption.EncryptionType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LocalConfigDTOMapper {
  public static LocalConfigDTO getLocalConfigDTO(
      String accountIdentifier, ConnectorRequestDTO connectorRequestDTO, LocalConnectorDTO localConnectorDTO) {
    return LocalConfigDTO.builder()
        .isDefault(localConnectorDTO.isDefault())
        .encryptionType(EncryptionType.LOCAL)

        .name(connectorRequestDTO.getName())
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(connectorRequestDTO.getOrgIdentifier())
        .projectIdentifier(connectorRequestDTO.getProjectIdentifier())
        .tags(connectorRequestDTO.getTags())
        .identifier(connectorRequestDTO.getIdentifier())
        .description(connectorRequestDTO.getDescription())
        .build();
  }
}
