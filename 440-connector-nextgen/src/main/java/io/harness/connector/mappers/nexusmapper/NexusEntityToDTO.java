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
import io.harness.connector.entities.embedded.nexusconnector.NexusUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthType;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthenticationDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusUsernamePasswordAuthDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.CDC)
public class NexusEntityToDTO implements ConnectorEntityToDTOMapper<NexusConnectorDTO, NexusConnector> {
  @Override
  public NexusConnectorDTO createConnectorDTO(NexusConnector nexusConnector) {
    NexusAuthenticationDTO nexusAuthenticationDTO = null;
    if (nexusConnector.getAuthType() != NexusAuthType.ANONYMOUS || nexusConnector.getNexusAuthentication() != null) {
      NexusUserNamePasswordAuthentication nexusCredentials =
          (NexusUserNamePasswordAuthentication) nexusConnector.getNexusAuthentication();
      NexusUsernamePasswordAuthDTO nexusUsernamePasswordAuthDTO =
          NexusUsernamePasswordAuthDTO.builder()
              .username(nexusCredentials.getUsername())
              .usernameRef(SecretRefHelper.createSecretRef(nexusCredentials.getUsernameRef()))
              .passwordRef(SecretRefHelper.createSecretRef(nexusCredentials.getPasswordRef()))
              .build();
      nexusAuthenticationDTO = NexusAuthenticationDTO.builder()
                                   .authType(nexusConnector.getAuthType())
                                   .credentials(nexusUsernamePasswordAuthDTO)
                                   .build();
    } else {
      nexusAuthenticationDTO = NexusAuthenticationDTO.builder().authType(nexusConnector.getAuthType()).build();
    }

    return NexusConnectorDTO.builder()
        .nexusServerUrl(nexusConnector.getUrl())
        .auth(nexusAuthenticationDTO)
        .version(nexusConnector.getNexusVersion())
        .build();
  }
}
