package io.harness.connector.mappers.secretmanagermapper;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsConnector;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsConnector.AwsKmsConnectorBuilder;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsManualCredential;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorCredentialDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialSpecManualConfigDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialType;
import io.harness.exception.InvalidRequestException;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
public class AwsKmsDTOToEntity implements ConnectorDTOToEntityMapper<AwsKmsConnectorDTO, AwsKmsConnector> {
  @Override
  public AwsKmsConnector toConnectorEntity(AwsKmsConnectorDTO connectorDTO) {
    AwsKmsConnectorBuilder builder = AwsKmsConnector.builder();
    AwsKmsConnectorCredentialDTO credential = connectorDTO.getCredential();
    AwsKmsCredentialType credentialType = credential.getCredentialType();
    switch (credentialType){
      case MANUAL_CONFIG:
        builder = buildManualConfig(credential);
        break;
        //TODO: Shashank: Implement for IAM and STS
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }

    return builder
        .kmsArn(connectorDTO.getKmsArn())
        .region(connectorDTO.getRegion())
        .isDefault(connectorDTO.isDefault())
        .build();

  }

  private AwsKmsConnectorBuilder buildManualConfig(AwsKmsConnectorCredentialDTO credential) {
    AwsKmsCredentialSpecManualConfigDTO credentialConfig = (AwsKmsCredentialSpecManualConfigDTO) credential.getConfig();
    AwsKmsManualCredential build = AwsKmsManualCredential.builder()
                                                      .secretKey(credentialConfig.getSecretKey())
                                                      .accessKey(credentialConfig.getAccessKey())
                                                      .build();
    return AwsKmsConnector.builder().credentialSpec(build);
  }
}
