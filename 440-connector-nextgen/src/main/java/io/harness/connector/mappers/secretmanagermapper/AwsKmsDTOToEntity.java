package io.harness.connector.mappers.secretmanagermapper;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awskmsconnector.*;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsConnector.AwsKmsConnectorBuilder;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.awskmsconnector.*;
import io.harness.exception.InvalidRequestException;

import javax.inject.Inject;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
public class AwsKmsDTOToEntity implements ConnectorDTOToEntityMapper<AwsKmsConnectorDTO, AwsKmsConnector> {
  @Inject
  private AwsKmsMappingHelper helper;

  @Override
  public AwsKmsConnector toConnectorEntity(AwsKmsConnectorDTO connectorDTO) {
    AwsKmsConnectorBuilder builder;
    AwsKmsConnectorCredentialDTO credential = connectorDTO.getCredential();
    AwsKmsCredentialType credentialType = credential.getCredentialType();
    switch (credentialType){
      case MANUAL_CONFIG:
        builder = helper.buildManualConfig((AwsKmsCredentialSpecManualConfigDTO) credential.getConfig());
        break;
      case ASSUME_IAM_ROLE:
        builder = helper.buildIAMConfig((AwsKmsCredentialSpecAssumeIAMDTO)credential.getConfig());
        break;
      case ASSUME_STS_ROLE:
        builder = helper.buildSTSConfig((AwsKmsCredentialSpecAssumeSTSDTO)credential.getConfig());
        break;

      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }

    return builder
        .kmsArn(connectorDTO.getKmsArn())
        .region(connectorDTO.getRegion())
        .isDefault(connectorDTO.isDefault())
        .build();

  }
}
