package io.harness.connector.mappers.secretmanagermapper;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorDTO;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
public class AwsKmsEntityToDTO implements ConnectorEntityToDTOMapper<AwsKmsConnectorDTO, AwsKmsConnector> {
  @Override
  public AwsKmsConnectorDTO createConnectorDTO(AwsKmsConnector connector) {
    AwsKmsConnectorDTO awsKmsConnectorDTO = AwsKmsConnectorDTO.builder().build();/*
                                                .name(connector.getName())
                                                .accessKey(connector.getAccessKey())
                                                .secretKey(connector.getSecretKey())
                                                .kmsArn(connector.getKmsArn())
                                                .region(connector.getRegion())
                                                .isDefault(connector.isDefault())
                                                .build();
    awsKmsConnectorDTO.setHarnessManaged(Boolean.TRUE.equals(connector.getHarnessManaged()));*/
    return awsKmsConnectorDTO;
  }
}
