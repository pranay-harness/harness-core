package io.harness.connector.mappers.awscodecommit;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitAuthentication;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitConfig;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitConfig.AwsCodeCommitConfigBuilder;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitSecretKeyAccessKey;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsCredentialsDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitSecretKeyAccessKeyDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitUrlType;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class AwsCodeCommitDTOToEntity
    implements ConnectorDTOToEntityMapper<AwsCodeCommitConnectorDTO, AwsCodeCommitConfig> {
  @Override
  public AwsCodeCommitConfig toConnectorEntity(AwsCodeCommitConnectorDTO connectorDTO) {
    final String url = connectorDTO.getUrl();
    final AwsCodeCommitUrlType urlType = connectorDTO.getUrlType();
    final AwsCodeCommitAuthenticationDTO authentication = connectorDTO.getAuthentication();
    final AwsCodeCommitAuthType authType = authentication.getAuthType();
    AwsCodeCommitConfigBuilder builder = AwsCodeCommitConfig.builder().url(url).urlType(urlType);
    if (authType == AwsCodeCommitAuthType.HTTPS) {
      AwsCodeCommitHttpsCredentialsDTO credentials = (AwsCodeCommitHttpsCredentialsDTO) authentication.getCredentials();
      AwsCodeCommitHttpsAuthType type = credentials.getType();
      if (type == AwsCodeCommitHttpsAuthType.ACCESS_KEY_AND_SECRET_KEY) {
        AwsCodeCommitSecretKeyAccessKeyDTO httpCredentialsSpec =
            (AwsCodeCommitSecretKeyAccessKeyDTO) credentials.getHttpCredentialsSpec();
        final String accessKeyRef = SecretRefHelper.getSecretConfigString(httpCredentialsSpec.getAccessKeyRef());
        final String secretKeyRef = SecretRefHelper.getSecretConfigString(httpCredentialsSpec.getSecretKeyRef());
        final String accessKey = httpCredentialsSpec.getAccessKey();
        builder.authentication(AwsCodeCommitAuthentication.builder()
                                   .authType(authType)
                                   .credentialsType(type)
                                   .credential(AwsCodeCommitSecretKeyAccessKey.builder()
                                                   .accessKey(accessKey)
                                                   .accessKeyRef(accessKeyRef)
                                                   .secretKeyRef(secretKeyRef)
                                                   .build())
                                   .build());
      }
    }
    return builder.build();
  }
}
