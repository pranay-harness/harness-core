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
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitSecretKeyAccessKey;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsCredentialsDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsCredentialsSpecDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitSecretKeyAccessKeyDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitUrlType;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.govern.Switch;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.CI)
@Singleton
public class AwsCodeCommitEntityToDTO
    implements ConnectorEntityToDTOMapper<AwsCodeCommitConnectorDTO, AwsCodeCommitConfig> {
  @Override
  public AwsCodeCommitConnectorDTO createConnectorDTO(AwsCodeCommitConfig connector) {
    final AwsCodeCommitAuthentication authentication = connector.getAuthentication();
    final String url = connector.getUrl();
    final AwsCodeCommitUrlType urlType = connector.getUrlType();
    return AwsCodeCommitConnectorDTO.builder()
        .url(url)
        .urlType(urlType)
        .authentication(buildAwsCodeCommitAuthenticationDTO(authentication))
        .build();
  }

  public static AwsCodeCommitAuthenticationDTO buildAwsCodeCommitAuthenticationDTO(
      AwsCodeCommitAuthentication authentication) {
    AwsCodeCommitAuthenticationDTO authenticationDTO = null;
    final AwsCodeCommitAuthType connectionType = authentication.getAuthType();
    final AwsCodeCommitHttpsAuthType credentialsType = authentication.getCredentialsType();
    switch (connectionType) {
      case HTTPS:
        authenticationDTO = AwsCodeCommitAuthenticationDTO.builder()
                                .authType(connectionType)
                                .credentials(buildAwsCodeCommitHttpCredentialsDTO(credentialsType, authentication))
                                .build();
        break;
      default:
        Switch.unhandled(connectionType);
    }
    return authenticationDTO;
  }

  private static AwsCodeCommitHttpsCredentialsDTO buildAwsCodeCommitHttpCredentialsDTO(
      AwsCodeCommitHttpsAuthType credentialsType, AwsCodeCommitAuthentication authentication) {
    return AwsCodeCommitHttpsCredentialsDTO.builder()
        .type(credentialsType)
        .httpCredentialsSpec(buildAwsCodeCommitHttpsCredentialsSpecDTO(credentialsType, authentication))
        .build();
  }

  private static AwsCodeCommitHttpsCredentialsSpecDTO buildAwsCodeCommitHttpsCredentialsSpecDTO(
      AwsCodeCommitHttpsAuthType credentialsType, AwsCodeCommitAuthentication authentication) {
    AwsCodeCommitHttpsCredentialsSpecDTO awsCodeCommitHttpsCredentialsSpecDTO = null;
    switch (credentialsType) {
      case ACCESS_KEY_AND_SECRET_KEY:
        AwsCodeCommitSecretKeyAccessKey credential = (AwsCodeCommitSecretKeyAccessKey) authentication.getCredential();
        SecretRefData secretKeyRef = SecretRefHelper.createSecretRef(credential.getSecretKeyRef());
        SecretRefData accessKeyRef = SecretRefHelper.createSecretRef(credential.getAccessKeyRef());
        String accessKey = credential.getAccessKey();
        awsCodeCommitHttpsCredentialsSpecDTO = AwsCodeCommitSecretKeyAccessKeyDTO.builder()
                                                   .accessKey(accessKey)
                                                   .accessKeyRef(accessKeyRef)
                                                   .secretKeyRef(secretKeyRef)
                                                   .build();
        break;
      default:
        Switch.unhandled(credentialsType);
    }
    return awsCodeCommitHttpsCredentialsSpecDTO;
  }
}
