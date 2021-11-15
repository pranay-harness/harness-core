package io.harness.connector.mappers.awscodecommit;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitConfig;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthentication;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnector;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsCredentials;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsCredentialsSpec;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitSecretKeyAccessKey;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitUrlType;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.govern.Switch;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.CI)
@Singleton
public class AwsCodeCommitEntityToDTO
    implements ConnectorEntityToDTOMapper<AwsCodeCommitConnector, AwsCodeCommitConfig> {
  @Override
  public AwsCodeCommitConnector createConnectorDTO(AwsCodeCommitConfig connector) {
    final io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitAuthentication authentication =
        connector.getAuthentication();
    final String url = connector.getUrl();
    final AwsCodeCommitUrlType urlType = connector.getUrlType();
    return AwsCodeCommitConnector.builder()
        .url(url)
        .urlType(urlType)
        .authentication(buildAwsCodeCommitAuthenticationDTO(authentication))
        .build();
  }

  public static AwsCodeCommitAuthentication buildAwsCodeCommitAuthenticationDTO(
      io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitAuthentication authentication) {
    AwsCodeCommitAuthentication authenticationDTO = null;
    final AwsCodeCommitAuthType connectionType = authentication.getAuthType();
    final AwsCodeCommitHttpsAuthType credentialsType = authentication.getCredentialsType();
    switch (connectionType) {
      case HTTPS:
        authenticationDTO = AwsCodeCommitAuthentication.builder()
                                .authType(connectionType)
                                .credentials(buildAwsCodeCommitHttpCredentialsDTO(credentialsType, authentication))
                                .build();
        break;
      default:
        Switch.unhandled(connectionType);
    }
    return authenticationDTO;
  }

  private static AwsCodeCommitHttpsCredentials buildAwsCodeCommitHttpCredentialsDTO(
      AwsCodeCommitHttpsAuthType credentialsType,
      io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitAuthentication authentication) {
    return AwsCodeCommitHttpsCredentials.builder()
        .type(credentialsType)
        .httpCredentialsSpec(buildAwsCodeCommitHttpsCredentialsSpecDTO(credentialsType, authentication))
        .build();
  }

  private static AwsCodeCommitHttpsCredentialsSpec buildAwsCodeCommitHttpsCredentialsSpecDTO(
      AwsCodeCommitHttpsAuthType credentialsType,
      io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitAuthentication authentication) {
    AwsCodeCommitHttpsCredentialsSpec awsCodeCommitHttpsCredentialsSpec = null;
    switch (credentialsType) {
      case ACCESS_KEY_AND_SECRET_KEY:
        io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitSecretKeyAccessKey credential =
            (io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitSecretKeyAccessKey)
                authentication.getCredential();
        SecretRefData secretKeyRef = SecretRefHelper.createSecretRef(credential.getSecretKeyRef());
        SecretRefData accessKeyRef = SecretRefHelper.createSecretRef(credential.getAccessKeyRef());
        String accessKey = credential.getAccessKey();
        awsCodeCommitHttpsCredentialsSpec = AwsCodeCommitSecretKeyAccessKey.builder()
                                                .accessKey(accessKey)
                                                .accessKeyRef(accessKeyRef)
                                                .secretKeyRef(secretKeyRef)
                                                .build();
        break;
      default:
        Switch.unhandled(credentialsType);
    }
    return awsCodeCommitHttpsCredentialsSpec;
  }
}
