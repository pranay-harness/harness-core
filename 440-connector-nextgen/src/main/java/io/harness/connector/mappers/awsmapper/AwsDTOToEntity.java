package io.harness.connector.mappers.awsmapper;

import io.harness.connector.entities.embedded.awsconnector.AwsAccessKeyCredential;
import io.harness.connector.entities.embedded.awsconnector.AwsConfig;
import io.harness.connector.entities.embedded.awsconnector.AwsConfig.AwsConfigBuilder;
import io.harness.connector.entities.embedded.awsconnector.AwsIamCredential;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsInheritFromDelegateSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.NGAccess;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class AwsDTOToEntity implements ConnectorDTOToEntityMapper<AwsConnectorDTO, AwsConfig> {
  private SecretRefService secretRefService;

  @Override
  public AwsConfig toConnectorEntity(AwsConnectorDTO connectorDTO, NGAccess ngAccess) {
    final AwsCredentialDTO credential = connectorDTO.getCredential();
    final AwsCredentialType credentialType = credential.getAwsCredentialType();
    AwsConfigBuilder awsConfigBuilder;
    switch (credentialType) {
      case INHERIT_FROM_DELEGATE:
        awsConfigBuilder = buildInheritFromDelegate(credential);
        break;
      case MANUAL_CREDENTIALS:
        awsConfigBuilder = buildManualCredential(credential, ngAccess);
        break;
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }
    return awsConfigBuilder.crossAccountAccess(credential.getCrossAccountAccess()).build();
  }

  private AwsConfigBuilder buildInheritFromDelegate(AwsCredentialDTO connector) {
    final AwsInheritFromDelegateSpecDTO config = (AwsInheritFromDelegateSpecDTO) connector.getConfig();
    final AwsIamCredential awsIamCredential =
        AwsIamCredential.builder().delegateSelectors(config.getDelegateSelectors()).build();
    return AwsConfig.builder().credentialType(AwsCredentialType.INHERIT_FROM_DELEGATE).credential(awsIamCredential);
  }

  private AwsConfigBuilder buildManualCredential(AwsCredentialDTO connector, NGAccess ngAccess) {
    final AwsManualConfigSpecDTO config = (AwsManualConfigSpecDTO) connector.getConfig();
    final String secretKeyRef = secretRefService.validateAndGetSecretConfigString(config.getSecretKeyRef(), ngAccess);
    final String accessKeyRef = secretRefService.validateAndGetSecretConfigString(config.getAccessKeyRef(), ngAccess);
    AwsAccessKeyCredential accessKeyCredential = AwsAccessKeyCredential.builder()
                                                     .accessKey(config.getAccessKey())
                                                     .accessKeyRef(accessKeyRef)
                                                     .secretKeyRef(secretKeyRef)
                                                     .build();
    return AwsConfig.builder().credentialType(AwsCredentialType.MANUAL_CREDENTIALS).credential(accessKeyCredential);
  }
}
