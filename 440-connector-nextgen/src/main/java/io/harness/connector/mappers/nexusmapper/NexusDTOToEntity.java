/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.nexusmapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.nexusconnector.NexusConnector;
import io.harness.connector.entities.embedded.nexusconnector.NexusConnector.NexusConnectorBuilder;
import io.harness.connector.entities.embedded.nexusconnector.NexusUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthType;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusUsernamePasswordAuthDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.CDC)
public class NexusDTOToEntity implements ConnectorDTOToEntityMapper<NexusConnectorDTO, NexusConnector> {
  @Override
  public NexusConnector toConnectorEntity(NexusConnectorDTO configDTO) {
    NexusAuthType nexusAuthType = configDTO.getAuth().getAuthType();
    NexusConnectorBuilder nexusConnectorBuilder = NexusConnector.builder()
                                                      .url(configDTO.getNexusServerUrl())
                                                      .nexusVersion(configDTO.getVersion())
                                                      .authType(nexusAuthType);
    if (nexusAuthType == NexusAuthType.USER_PASSWORD) {
      NexusUsernamePasswordAuthDTO nexusUsernamePasswordAuthDTO =
          (NexusUsernamePasswordAuthDTO) configDTO.getAuth().getCredentials();
      nexusConnectorBuilder.nexusAuthentication(createNexusAuthentication(nexusUsernamePasswordAuthDTO));
    }
    return nexusConnectorBuilder.build();
  }

  private NexusUserNamePasswordAuthentication createNexusAuthentication(
      NexusUsernamePasswordAuthDTO nexusUsernamePasswordAuthDTO) {
    return NexusUserNamePasswordAuthentication.builder()
        .username(nexusUsernamePasswordAuthDTO.getUsername())
        .usernameRef(SecretRefHelper.getSecretConfigString(nexusUsernamePasswordAuthDTO.getUsernameRef()))
        .passwordRef(SecretRefHelper.getSecretConfigString(nexusUsernamePasswordAuthDTO.getPasswordRef()))
        .build();
  }
}
