/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.localconnector.LocalConnectorDTO;
import io.harness.secretmanagerclient.dto.LocalConfigDTO;
import io.harness.security.encryption.EncryptionType;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class LocalConfigDTOMapper {
  public static LocalConfigDTO getLocalConfigDTO(
      String accountIdentifier, ConnectorDTO connectorRequestDTO, LocalConnectorDTO localConnectorDTO) {
    ConnectorInfoDTO connector = connectorRequestDTO.getConnectorInfo();
    return LocalConfigDTO.builder()
        .isDefault(false)
        .encryptionType(EncryptionType.LOCAL)

        .name(connector.getName())
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(connector.getOrgIdentifier())
        .projectIdentifier(connector.getProjectIdentifier())
        .tags(connector.getTags())
        .identifier(connector.getIdentifier())
        .description(connector.getDescription())
        .harnessManaged(localConnectorDTO.isHarnessManaged())
        .build();
  }
}
