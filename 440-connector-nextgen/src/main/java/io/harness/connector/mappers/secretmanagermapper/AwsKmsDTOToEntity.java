/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.secretmanagermapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsConnector;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsConnector.AwsKmsConnectorBuilder;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorCredentialDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialSpecAssumeIAMDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialSpecAssumeSTSDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialSpecManualConfigDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialType;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.InvalidRequestException;

@OwnedBy(PL)
public class AwsKmsDTOToEntity implements ConnectorDTOToEntityMapper<AwsKmsConnectorDTO, AwsKmsConnector> {
  @Override
  public AwsKmsConnector toConnectorEntity(AwsKmsConnectorDTO connectorDTO) {
    AwsKmsConnectorBuilder builder;
    AwsKmsConnectorCredentialDTO credential = connectorDTO.getCredential();
    AwsKmsCredentialType credentialType = credential.getCredentialType();
    switch (credentialType) {
      case MANUAL_CONFIG:
        builder = AwsKmsMappingHelper.buildManualConfig((AwsKmsCredentialSpecManualConfigDTO) credential.getConfig());
        break;
      case ASSUME_IAM_ROLE:
        builder = AwsKmsMappingHelper.buildIAMConfig((AwsKmsCredentialSpecAssumeIAMDTO) credential.getConfig());
        break;
      case ASSUME_STS_ROLE:
        builder = AwsKmsMappingHelper.buildSTSConfig((AwsKmsCredentialSpecAssumeSTSDTO) credential.getConfig());
        break;
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }
    final String kmsArn = SecretRefHelper.getSecretConfigString(connectorDTO.getKmsArn());
    return builder.kmsArn(kmsArn).region(connectorDTO.getRegion()).isDefault(connectorDTO.isDefault()).build();
  }
}
