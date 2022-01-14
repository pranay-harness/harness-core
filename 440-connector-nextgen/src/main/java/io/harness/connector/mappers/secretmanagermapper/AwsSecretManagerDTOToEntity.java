package io.harness.connector.mappers.secretmanagermapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awssecretmanager.AwsSecretManagerConnector;
import io.harness.connector.entities.embedded.awssecretmanager.AwsSecretManagerConnector.AwsSecretManagerConnectorBuilder;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSMCredentialSpecAssumeIAMDTO;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSMCredentialSpecAssumeSTSDTO;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSMCredentialSpecManualConfigDTO;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerCredentialDTO;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerCredentialType;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerDTO;
import io.harness.exception.InvalidRequestException;

@OwnedBy(PL)
public class AwsSecretManagerDTOToEntity
    implements ConnectorDTOToEntityMapper<AwsSecretManagerDTO, AwsSecretManagerConnector> {
  @Override
  public AwsSecretManagerConnector toConnectorEntity(AwsSecretManagerDTO connectorDTO) {
    AwsSecretManagerConnectorBuilder builder;
    AwsSecretManagerCredentialDTO credential = connectorDTO.getCredential();
    AwsSecretManagerCredentialType credentialType = credential.getCredentialType();
    switch (credentialType) {
      case MANUAL_CONFIG:
        builder =
            AwsSecretManagerMapperHelper.buildManualConfig((AwsSMCredentialSpecManualConfigDTO) credential.getConfig());
        break;
      case ASSUME_IAM_ROLE:
        builder = AwsSecretManagerMapperHelper.buildIAMConfig((AwsSMCredentialSpecAssumeIAMDTO) credential.getConfig());
        break;
      case ASSUME_STS_ROLE:
        builder = AwsSecretManagerMapperHelper.buildSTSConfig((AwsSMCredentialSpecAssumeSTSDTO) credential.getConfig());
        break;
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }

    return builder.region(connectorDTO.getRegion())
        .secretNamePrefix(connectorDTO.getSecretNamePrefix())
        .isDefault(connectorDTO.isDefault())
        .build();
  }
}