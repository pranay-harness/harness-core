/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.awscodecommit;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitAuthentication;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitConfig;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitHttpsCredential;
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
import io.harness.govern.Switch;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.CI)
@Singleton
public class AwsCodeCommitDTOToEntity
    implements ConnectorDTOToEntityMapper<AwsCodeCommitConnectorDTO, AwsCodeCommitConfig> {
  @Override
  public AwsCodeCommitConfig toConnectorEntity(AwsCodeCommitConnectorDTO connectorDTO) {
    final String url = connectorDTO.getUrl();
    final AwsCodeCommitUrlType urlType = connectorDTO.getUrlType();

    final AwsCodeCommitAuthenticationDTO authentication = connectorDTO.getAuthentication();

    return AwsCodeCommitConfig.builder()
        .url(url)
        .urlType(urlType)
        .authentication(buildAwsCodeCommitAuthentication(authentication))
        .build();
  }

  public static AwsCodeCommitAuthentication buildAwsCodeCommitAuthentication(
      AwsCodeCommitAuthenticationDTO authenticationDTO) {
    AwsCodeCommitAuthentication authentication = null;
    final AwsCodeCommitAuthType authType = authenticationDTO.getAuthType();
    switch (authType) {
      case HTTPS:
        AwsCodeCommitHttpsCredentialsDTO credentials =
            (AwsCodeCommitHttpsCredentialsDTO) authenticationDTO.getCredentials();
        authentication = AwsCodeCommitAuthentication.builder()
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
      AwsCodeCommitHttpsCredentialsDTO credentials) {
    AwsCodeCommitHttpsCredential awsCodeCommitHttpsCredential = null;
    AwsCodeCommitHttpsAuthType type = credentials.getType();
    switch (type) {
      case ACCESS_KEY_AND_SECRET_KEY:
        AwsCodeCommitSecretKeyAccessKeyDTO httpCredentialsSpec =
            (AwsCodeCommitSecretKeyAccessKeyDTO) credentials.getHttpCredentialsSpec();
        final String accessKeyRef = SecretRefHelper.getSecretConfigString(httpCredentialsSpec.getAccessKeyRef());
        final String secretKeyRef = SecretRefHelper.getSecretConfigString(httpCredentialsSpec.getSecretKeyRef());
        final String accessKey = httpCredentialsSpec.getAccessKey();
        awsCodeCommitHttpsCredential = AwsCodeCommitSecretKeyAccessKey.builder()
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
