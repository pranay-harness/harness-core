package io.harness.connector.mappers.awsmapper;

import io.harness.connector.entities.embedded.awsconnector.AwsAccessKeyCredential;
import io.harness.connector.entities.embedded.awsconnector.AwsConfig;
import io.harness.connector.entities.embedded.awsconnector.AwsIamCredential;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO.AwsCredentialDTOBuilder;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsInheritFromDelegateSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;

@Singleton
public class AwsEntityToDTO extends ConnectorEntityToDTOMapper<AwsConnectorDTO, AwsConfig> {
  @Override
  public AwsConnectorDTO createConnectorDTO(AwsConfig connector) {
    final AwsCredentialType credentialType = connector.getCredentialType();
    AwsCredentialDTOBuilder awsCredentialDTOBuilder;
    switch (credentialType) {
      case INHERIT_FROM_DELEGATE:
        awsCredentialDTOBuilder = buildInheritFromDelegate((AwsIamCredential) connector.getCredential());
        break;
      case MANUAL_CREDENTIALS:
        awsCredentialDTOBuilder = buildManualCredential((AwsAccessKeyCredential) connector.getCredential());
        break;
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }
    return AwsConnectorDTO.builder()
        .credential(awsCredentialDTOBuilder.crossAccountAccess(connector.getCrossAccountAccess()).build())
        .build();
  }

  private AwsCredentialDTOBuilder buildManualCredential(AwsAccessKeyCredential credential) {
    final SecretRefData secretRef = SecretRefHelper.createSecretRef(credential.getSecretKeyRef());
    final AwsManualConfigSpecDTO awsManualConfigSpecDTO =
        AwsManualConfigSpecDTO.builder().accessKey(credential.getAccessKey()).secretKeyRef(secretRef).build();
    return AwsCredentialDTO.builder()
        .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
        .config(awsManualConfigSpecDTO);
  }

  private AwsCredentialDTOBuilder buildInheritFromDelegate(AwsIamCredential credential) {
    final AwsInheritFromDelegateSpecDTO specDTO =
        AwsInheritFromDelegateSpecDTO.builder().delegateSelector(credential.getDelegateSelector()).build();
    return AwsCredentialDTO.builder().awsCredentialType(AwsCredentialType.INHERIT_FROM_DELEGATE).config(specDTO);
  }
}
