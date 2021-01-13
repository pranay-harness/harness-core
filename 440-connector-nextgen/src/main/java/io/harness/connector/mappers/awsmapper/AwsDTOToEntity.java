package io.harness.connector.mappers.awsmapper;

import io.harness.connector.entities.embedded.awsconnector.AwsAccessKeyCredential;
import io.harness.connector.entities.embedded.awsconnector.AwsConfig;
import io.harness.connector.entities.embedded.awsconnector.AwsConfig.AwsConfigBuilder;
import io.harness.connector.entities.embedded.awsconnector.AwsIamCredential;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsInheritFromDelegateSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;

@Singleton
public class AwsDTOToEntity extends ConnectorDTOToEntityMapper<AwsConnectorDTO, AwsConfig> {
  @Override
  public AwsConfig toConnectorEntity(AwsConnectorDTO connectorDTO) {
    final AwsCredentialDTO credential = connectorDTO.getCredential();
    final AwsCredentialType credentialType = credential.getAwsCredentialType();
    AwsConfigBuilder awsConfigBuilder;
    switch (credentialType) {
      case INHERIT_FROM_DELEGATE:
        awsConfigBuilder = buildInheritFromDelegate(credential);
        break;
      case MANUAL_CREDENTIALS:
        awsConfigBuilder = buildManualCredential(credential);
        break;
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }
    return awsConfigBuilder.crossAccountAccess(credential.getCrossAccountAccess()).build();
  }

  private AwsConfigBuilder buildInheritFromDelegate(AwsCredentialDTO connector) {
    final AwsInheritFromDelegateSpecDTO config = (AwsInheritFromDelegateSpecDTO) connector.getConfig();
    final AwsIamCredential awsIamCredential =
        AwsIamCredential.builder().delegateSelector(config.getDelegateSelector()).build();
    return AwsConfig.builder().credentialType(AwsCredentialType.INHERIT_FROM_DELEGATE).credential(awsIamCredential);
  }

  private AwsConfigBuilder buildManualCredential(AwsCredentialDTO connector) {
    final AwsManualConfigSpecDTO config = (AwsManualConfigSpecDTO) connector.getConfig();
    final String secretKeyRef = SecretRefHelper.getSecretConfigString(config.getSecretKeyRef());
    AwsAccessKeyCredential accessKeyCredential =
        AwsAccessKeyCredential.builder().accessKey(config.getAccessKey()).secretKeyRef(secretKeyRef).build();
    return AwsConfig.builder().credentialType(AwsCredentialType.MANUAL_CREDENTIALS).credential(accessKeyCredential);
  }
}
