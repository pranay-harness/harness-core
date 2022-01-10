/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.aws;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.aws.AwsAccessKeyCredential;
import io.harness.aws.AwsConfig;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsCredentialsDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitSecretKeyAccessKeyDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class AwsNgConfigMapperTest extends CategoryTest {
  @Mock SecretDecryptionService secretDecryptionService;
  @InjectMocks @Spy AwsNgConfigMapper awsNgConfigMapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testMapAwsConfig() {
    final String passwordRefIdentifier = "passwordRefIdentifier";
    final String accessKey = "accessKey";
    SecretRefData passwordSecretRef =
        SecretRefData.builder().identifier(passwordRefIdentifier).scope(Scope.ACCOUNT).build();
    doReturn(passwordRefIdentifier.toCharArray())
        .when(awsNgConfigMapper)
        .getDecryptedValueWithNullCheck(passwordSecretRef);
    final AwsManualConfigSpecDTO awsManualConfigSpecDTO =
        AwsManualConfigSpecDTO.builder().secretKeyRef(passwordSecretRef).accessKey(accessKey).build();
    final AwsCredentialDTO awsCredentialDTO = AwsCredentialDTO.builder()
                                                  .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
                                                  .crossAccountAccess(CrossAccountAccessDTO.builder().build())
                                                  .config(awsManualConfigSpecDTO)
                                                  .build();
    final AwsConfig awsConfig =
        awsNgConfigMapper.mapAwsConfigWithDecryption(awsCredentialDTO, AwsCredentialType.MANUAL_CREDENTIALS, null);
    assertThat(awsConfig).isNotNull();
    assertThat(awsConfig).isEqualTo(AwsConfig.builder()
                                        .crossAccountAccess(io.harness.aws.CrossAccountAccess.builder().build())
                                        .awsAccessKeyCredential(AwsAccessKeyCredential.builder()
                                                                    .secretKey(passwordRefIdentifier.toCharArray())
                                                                    .accessKey(accessKey)
                                                                    .build())
                                        .build());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testMapAwsCodeCommit() {
    final String secretKeyRefIdentifier = "secretKeyRefIdentifier";
    final String accessKey = "accessKey";
    SecretRefData secretKeySecretRef =
        SecretRefData.builder().identifier(secretKeyRefIdentifier).scope(Scope.ACCOUNT).build();
    doReturn(secretKeyRefIdentifier.toCharArray())
        .when(awsNgConfigMapper)
        .getDecryptedValueWithNullCheck(secretKeySecretRef);
    final AwsCodeCommitAuthenticationDTO awsCodeCommitAuthenticationDTO =
        AwsCodeCommitAuthenticationDTO.builder()
            .authType(AwsCodeCommitAuthType.HTTPS)
            .credentials(AwsCodeCommitHttpsCredentialsDTO.builder()
                             .type(AwsCodeCommitHttpsAuthType.ACCESS_KEY_AND_SECRET_KEY)
                             .httpCredentialsSpec(AwsCodeCommitSecretKeyAccessKeyDTO.builder()
                                                      .accessKey(accessKey)
                                                      .secretKeyRef(secretKeySecretRef)
                                                      .build())
                             .build())
            .build();
    final AwsConfig awsConfig = awsNgConfigMapper.mapAwsCodeCommit(awsCodeCommitAuthenticationDTO, null);
    assertThat(awsConfig).isNotNull();
    assertThat(awsConfig).isEqualTo(AwsConfig.builder()
                                        .awsAccessKeyCredential(AwsAccessKeyCredential.builder()
                                                                    .secretKey(secretKeyRefIdentifier.toCharArray())
                                                                    .accessKey(accessKey)
                                                                    .build())
                                        .build());
  }
}
