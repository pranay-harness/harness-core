/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.secretmanagermapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awssecretmanager.AwsSecretManagerConnector;
import io.harness.connector.entities.embedded.awssecretmanager.AwsSecretManagerIAMCredential;
import io.harness.connector.entities.embedded.awssecretmanager.AwsSecretManagerManualCredential;
import io.harness.connector.entities.embedded.awssecretmanager.AwsSecretManagerSTSCredential;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerCredentialType;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerDTO;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerDTO.AwsSecretManagerDTOBuilder;
import io.harness.exception.InvalidRequestException;

@OwnedBy(PL)
public class AwsSecretManagerEntityToDTO
    implements ConnectorEntityToDTOMapper<AwsSecretManagerDTO, AwsSecretManagerConnector> {
  @Override
  public AwsSecretManagerDTO createConnectorDTO(AwsSecretManagerConnector connector) {
    AwsSecretManagerDTOBuilder builder;
    AwsSecretManagerCredentialType credentialType = connector.getCredentialType();
    switch (credentialType) {
      case MANUAL_CONFIG:
        builder = AwsSecretManagerMapperHelper.buildFromManualConfig(
            (AwsSecretManagerManualCredential) connector.getCredentialSpec());
        break;
      case ASSUME_IAM_ROLE:
        builder = AwsSecretManagerMapperHelper.buildFromIAMConfig(
            (AwsSecretManagerIAMCredential) connector.getCredentialSpec());
        break;
      case ASSUME_STS_ROLE:
        builder = AwsSecretManagerMapperHelper.buildFromSTSConfig(
            (AwsSecretManagerSTSCredential) connector.getCredentialSpec());
        break;
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }

    return builder.region(connector.getRegion())
        .isDefault(connector.isDefault())
        .secretNamePrefix(connector.getSecretNamePrefix())
        .build();
  }
}
