/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.awscodecommit;

import static io.harness.encryption.Scope.ACCOUNT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitAuthentication;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitConfig;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitSecretKeyAccessKey;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsCredentialsDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitSecretKeyAccessKeyDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitUrlType;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class AwsCodeCommitEntityToDTOTest extends CategoryTest {
  @InjectMocks AwsCodeCommitEntityToDTO awsCodeCommitEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.ALEKSANDAR)
  @Category(UnitTests.class)
  public void createConnectorDTOTest() {
    String url = "https://git-codecommit.eu-central-1.amazonaws.com/v1/repos/test";
    String accessKey = "AKIAIOSFODNN7EXAMPLE";
    String secretKeyRef = ACCOUNT.getYamlRepresentation() + ".secretKeyRef";
    AwsCodeCommitSecretKeyAccessKey awsCodeCommitSecretKeyAccessKey =
        AwsCodeCommitSecretKeyAccessKey.builder().accessKey(accessKey).secretKeyRef(secretKeyRef).build();
    AwsCodeCommitAuthentication awsCodeCommitAuthentication =
        AwsCodeCommitAuthentication.builder()
            .authType(AwsCodeCommitAuthType.HTTPS)
            .credentialsType(AwsCodeCommitHttpsAuthType.ACCESS_KEY_AND_SECRET_KEY)
            .credential(awsCodeCommitSecretKeyAccessKey)
            .build();
    AwsCodeCommitConfig awsCodeCommitConfig = AwsCodeCommitConfig.builder()
                                                  .url(url)
                                                  .urlType(AwsCodeCommitUrlType.REPO)
                                                  .authentication(awsCodeCommitAuthentication)
                                                  .build();
    AwsCodeCommitConnectorDTO connectorDTO = awsCodeCommitEntityToDTO.createConnectorDTO(awsCodeCommitConfig);
    assertThat(connectorDTO).isNotNull();
    assertThat(connectorDTO.getUrl()).isEqualTo(url);
    assertThat(connectorDTO.getUrlType()).isEqualTo(AwsCodeCommitUrlType.REPO);
    assertThat(connectorDTO.getAuthentication().getAuthType()).isEqualTo(AwsCodeCommitAuthType.HTTPS);
    assertThat(((AwsCodeCommitHttpsCredentialsDTO) connectorDTO.getAuthentication().getCredentials()).getType())
        .isEqualTo(AwsCodeCommitHttpsAuthType.ACCESS_KEY_AND_SECRET_KEY);
    AwsCodeCommitSecretKeyAccessKeyDTO secretKeyAccessKeyDTO =
        (AwsCodeCommitSecretKeyAccessKeyDTO) ((AwsCodeCommitHttpsCredentialsDTO) connectorDTO.getAuthentication()
                                                  .getCredentials())
            .getHttpCredentialsSpec();

    assertThat(secretKeyAccessKeyDTO.getAccessKey()).isEqualTo(accessKey);
    assertThat(secretKeyAccessKeyDTO.getSecretKeyRef()).isEqualTo(SecretRefHelper.createSecretRef(secretKeyRef));
  }
}
