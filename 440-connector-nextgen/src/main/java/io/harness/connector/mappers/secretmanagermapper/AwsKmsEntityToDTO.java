package io.harness.connector.mappers.secretmanagermapper;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsConnector;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsIamCredential;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsManualCredential;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsStsCredential;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.awskmsconnector.*;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorDTO.AwsKmsConnectorDTOBuilder;
import io.harness.exception.InvalidRequestException;

import javax.inject.Inject;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
public class AwsKmsEntityToDTO implements ConnectorEntityToDTOMapper<AwsKmsConnectorDTO, AwsKmsConnector> {
    @Inject private AwsKmsMappingHelper helper;
    @Override
    public AwsKmsConnectorDTO createConnectorDTO(AwsKmsConnector connector) {
        AwsKmsConnectorDTOBuilder builder;
        AwsKmsCredentialType credentialType = connector.getCredentialType();
        switch (credentialType) {
            case MANUAL_CONFIG:
                builder = helper.buildFromManualConfig((AwsKmsManualCredential) connector.getCredentialSpec());
                break;
            case ASSUME_IAM_ROLE:
                builder = helper.buildFromIAMConfig((AwsKmsIamCredential) connector.getCredentialSpec());
                break;
            case ASSUME_STS_ROLE:
                builder = helper.buildFromSTSConfig((AwsKmsStsCredential) connector.getCredentialSpec());
                break;
            default:
                throw new InvalidRequestException("Invalid Credential type.");
        }

        AwsKmsConnectorDTO awsKmsConnectorDTO = builder
                .name(connector.getName())
                .kmsArn(connector.getKmsArn())
                .region(connector.getRegion())
                .isDefault(connector.isDefault())
                .build();
        awsKmsConnectorDTO.setHarnessManaged(Boolean.TRUE.equals(connector.getHarnessManaged()));

        return awsKmsConnectorDTO;
    }
}
