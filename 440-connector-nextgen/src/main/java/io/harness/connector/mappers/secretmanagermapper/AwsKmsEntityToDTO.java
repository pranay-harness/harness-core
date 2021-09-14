/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.secretmanagermapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsConnector;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsIamCredential;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsManualCredential;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsStsCredential;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorDTO.AwsKmsConnectorDTOBuilder;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialType;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.InvalidRequestException;

@OwnedBy(PL)
public class AwsKmsEntityToDTO implements ConnectorEntityToDTOMapper<AwsKmsConnectorDTO, AwsKmsConnector> {
  @Override
  public AwsKmsConnectorDTO createConnectorDTO(AwsKmsConnector connector) {
    AwsKmsConnectorDTOBuilder builder;
    AwsKmsCredentialType credentialType = connector.getCredentialType();
    switch (credentialType) {
      case MANUAL_CONFIG:
        builder = AwsKmsMappingHelper.buildFromManualConfig((AwsKmsManualCredential) connector.getCredentialSpec());
        break;
      case ASSUME_IAM_ROLE:
        builder = AwsKmsMappingHelper.buildFromIAMConfig((AwsKmsIamCredential) connector.getCredentialSpec());
        break;
      case ASSUME_STS_ROLE:
        builder = AwsKmsMappingHelper.buildFromSTSConfig((AwsKmsStsCredential) connector.getCredentialSpec());
        break;
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }

    SecretRefData kmsArn = SecretRefHelper.createSecretRef(connector.getKmsArn());
    AwsKmsConnectorDTO awsKmsConnectorDTO = builder.kmsArn(kmsArn)
                                                .region(connector.getRegion())
                                                .isDefault(connector.isDefault())
                                                .delegateSelectors(connector.getDelegateSelectors())
                                                .build();
    awsKmsConnectorDTO.setHarnessManaged(Boolean.TRUE.equals(connector.getHarnessManaged()));

    return awsKmsConnectorDTO;
  }
}
