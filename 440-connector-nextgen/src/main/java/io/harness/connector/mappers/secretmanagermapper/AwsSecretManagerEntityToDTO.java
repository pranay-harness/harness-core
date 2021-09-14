/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
