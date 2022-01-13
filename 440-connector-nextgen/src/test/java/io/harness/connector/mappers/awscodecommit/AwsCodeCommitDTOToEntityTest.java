/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.awscodecommit;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitConfig;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitSecretKeyAccessKey;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsCredentialsDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitSecretKeyAccessKeyDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitUrlType;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class AwsCodeCommitDTOToEntityTest extends CategoryTest {
  @InjectMocks AwsCodeCommitDTOToEntity awsCodeCommitDTOToEntity;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.ALEKSANDAR)
  @Category(UnitTests.class)
  public void toConnectorEntityTest() {
    String url = "https://git-codecommit.eu-central-1.amazonaws.com/v1/repos/test";
    String accessKey = "AKIAIOSFODNN7EXAMPLE";
    String secretKeyRef = "secretKeyRefIdentifier";
    SecretRefData secretKeySecretRefData =
        SecretRefData.builder().identifier(secretKeyRef).scope(Scope.ACCOUNT).build();

    AwsCodeCommitSecretKeyAccessKeyDTO secretKeyAccessKeyDTO =
        AwsCodeCommitSecretKeyAccessKeyDTO.builder().accessKey(accessKey).secretKeyRef(secretKeySecretRefData).build();

    AwsCodeCommitHttpsCredentialsDTO httpsCredentialsDTO =
        AwsCodeCommitHttpsCredentialsDTO.builder()
            .type(AwsCodeCommitHttpsAuthType.ACCESS_KEY_AND_SECRET_KEY)
            .httpCredentialsSpec(secretKeyAccessKeyDTO)
            .build();
    AwsCodeCommitAuthenticationDTO authenticationDTO = AwsCodeCommitAuthenticationDTO.builder()
                                                           .authType(AwsCodeCommitAuthType.HTTPS)
                                                           .credentials(httpsCredentialsDTO)
                                                           .build();
    AwsCodeCommitConnectorDTO connectorDTO = AwsCodeCommitConnectorDTO.builder()
                                                 .authentication(authenticationDTO)
                                                 .url(url)
                                                 .urlType(AwsCodeCommitUrlType.REPO)
                                                 .build();
    AwsCodeCommitConfig awsCodeCommitConfig = awsCodeCommitDTOToEntity.toConnectorEntity(connectorDTO);
    assertThat(awsCodeCommitConfig).isNotNull();
    assertThat(awsCodeCommitConfig.getUrl()).isEqualTo(url);
    assertThat(awsCodeCommitConfig.getUrlType()).isEqualTo(AwsCodeCommitUrlType.REPO);
    assertThat(awsCodeCommitConfig.getAuthentication().getAuthType()).isEqualTo(AwsCodeCommitAuthType.HTTPS);
    assertThat(awsCodeCommitConfig.getAuthentication().getCredentialsType())
        .isEqualTo(AwsCodeCommitHttpsAuthType.ACCESS_KEY_AND_SECRET_KEY);
    assertThat(
        ((AwsCodeCommitSecretKeyAccessKey) awsCodeCommitConfig.getAuthentication().getCredential()).getAccessKey())
        .isEqualTo(accessKey);
    assertThat(
        ((AwsCodeCommitSecretKeyAccessKey) awsCodeCommitConfig.getAuthentication().getCredential()).getSecretKeyRef())
        .isEqualTo(secretKeySecretRefData.toSecretRefStringValue());
  }
}
