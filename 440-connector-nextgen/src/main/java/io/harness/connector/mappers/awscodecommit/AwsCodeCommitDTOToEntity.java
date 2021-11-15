package io.harness.connector.mappers.awscodecommit;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitConfig;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitHttpsCredential;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthentication;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnector;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsCredentials;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitSecretKeyAccessKey;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitUrlType;
import io.harness.encryption.SecretRefHelper;
import io.harness.govern.Switch;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.CI)
@Singleton
public class AwsCodeCommitDTOToEntity
    implements ConnectorDTOToEntityMapper<AwsCodeCommitConnector, AwsCodeCommitConfig> {
  @Override
  public AwsCodeCommitConfig toConnectorEntity(AwsCodeCommitConnector connectorDTO) {
    final String url = connectorDTO.getUrl();
    final AwsCodeCommitUrlType urlType = connectorDTO.getUrlType();

    final AwsCodeCommitAuthentication authentication = connectorDTO.getAuthentication();

    return AwsCodeCommitConfig.builder()
        .url(url)
        .urlType(urlType)
        .authentication(buildAwsCodeCommitAuthentication(authentication))
        .build();
  }

  public static io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitAuthentication
  buildAwsCodeCommitAuthentication(AwsCodeCommitAuthentication authenticationDTO) {
    io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitAuthentication authentication = null;
    final AwsCodeCommitAuthType authType = authenticationDTO.getAuthType();
    switch (authType) {
      case HTTPS:
        AwsCodeCommitHttpsCredentials credentials = (AwsCodeCommitHttpsCredentials) authenticationDTO.getCredentials();
        authentication =
            io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitAuthentication.builder()
                .authType(authType)
                .credentialsType(credentials.getType())
                .credential(getAwsCodeCommitHttpsCredential(credentials))
                .build();
        break;
      default:
        Switch.unhandled(authType);
    }
    return authentication;
  }

  private static AwsCodeCommitHttpsCredential getAwsCodeCommitHttpsCredential(
      AwsCodeCommitHttpsCredentials credentials) {
    AwsCodeCommitHttpsCredential awsCodeCommitHttpsCredential = null;
    AwsCodeCommitHttpsAuthType type = credentials.getType();
    switch (type) {
      case ACCESS_KEY_AND_SECRET_KEY:
        AwsCodeCommitSecretKeyAccessKey httpCredentialsSpec =
            (AwsCodeCommitSecretKeyAccessKey) credentials.getHttpCredentialsSpec();
        final String accessKeyRef = SecretRefHelper.getSecretConfigString(httpCredentialsSpec.getAccessKeyRef());
        final String secretKeyRef = SecretRefHelper.getSecretConfigString(httpCredentialsSpec.getSecretKeyRef());
        final String accessKey = httpCredentialsSpec.getAccessKey();
        awsCodeCommitHttpsCredential =
            io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitSecretKeyAccessKey.builder()
                .accessKey(accessKey)
                .accessKeyRef(accessKeyRef)
                .secretKeyRef(secretKeyRef)
                .build();
        break;
      default:
        Switch.unhandled(type);
    }
    return awsCodeCommitHttpsCredential;
  }
}
