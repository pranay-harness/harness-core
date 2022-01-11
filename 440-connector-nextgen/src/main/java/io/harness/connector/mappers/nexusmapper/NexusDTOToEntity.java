/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
import org.apache.commons.lang3.StringUtils;

@Singleton
@OwnedBy(HarnessTeam.CDC)
public class NexusDTOToEntity implements ConnectorDTOToEntityMapper<NexusConnectorDTO, NexusConnector> {
  @Override
  public NexusConnector toConnectorEntity(NexusConnectorDTO configDTO) {
    NexusAuthType nexusAuthType = configDTO.getAuth().getAuthType();
    NexusConnectorBuilder nexusConnectorBuilder = NexusConnector.builder()
                                                      .url(StringUtils.trim(configDTO.getNexusServerUrl()))
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
