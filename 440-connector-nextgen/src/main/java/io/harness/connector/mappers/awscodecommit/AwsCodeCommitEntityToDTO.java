package io.harness.connector.mappers.awscodecommit;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitAuthentication;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitConfig;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitSecretKeyAccessKey;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorDTO.AwsCodeCommitConnectorDTOBuilder;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsCredentialsDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitSecretKeyAccessKeyDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitUrlType;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class AwsCodeCommitEntityToDTO
    implements ConnectorEntityToDTOMapper<AwsCodeCommitConnectorDTO, AwsCodeCommitConfig> {
  @Override
  public AwsCodeCommitConnectorDTO createConnectorDTO(AwsCodeCommitConfig connector) {
    final AwsCodeCommitAuthentication authentication = connector.getAuthentication();
    final String url = connector.getUrl();
    final AwsCodeCommitUrlType urlType = connector.getUrlType();
    final AwsCodeCommitAuthType connectionType = authentication.getAuthType();
    final AwsCodeCommitHttpsAuthType credentialsType = authentication.getCredentialsType();

    AwsCodeCommitConnectorDTOBuilder builder = AwsCodeCommitConnectorDTO.builder().url(url).urlType(urlType);
    if (connectionType == AwsCodeCommitAuthType.HTTPS) {
      if (credentialsType == AwsCodeCommitHttpsAuthType.ACCESS_KEY_AND_SECRET_KEY) {
        AwsCodeCommitSecretKeyAccessKey credential = (AwsCodeCommitSecretKeyAccessKey) authentication.getCredential();
        SecretRefData secretKeyRef = SecretRefHelper.createSecretRef(credential.getSecretKeyRef());
        SecretRefData accessKeyRef = SecretRefHelper.createSecretRef(credential.getAccessKeyRef());
        String accessKey = credential.getAccessKey();
        builder.authentication(AwsCodeCommitAuthenticationDTO.builder()
                                   .authType(connectionType)
                                   .credentials(AwsCodeCommitHttpsCredentialsDTO.builder()
                                                    .type(credentialsType)
                                                    .httpCredentialsSpec(AwsCodeCommitSecretKeyAccessKeyDTO.builder()
                                                                             .accessKey(accessKey)
                                                                             .accessKeyRef(accessKeyRef)
                                                                             .secretKeyRef(secretKeyRef)
                                                                             .build())
                                                    .build())
                                   .build());
      }
    }
    return builder.build();
  }
}
