package io.harness.connector.mappers.awscodecommit;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitConfig;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthentication;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnector;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsCredentials;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitSecretKeyAccessKey;
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

    AwsCodeCommitSecretKeyAccessKey secretKeyAccessKeyDTO =
        AwsCodeCommitSecretKeyAccessKey.builder().accessKey(accessKey).secretKeyRef(secretKeySecretRefData).build();

    AwsCodeCommitHttpsCredentials httpsCredentialsDTO = AwsCodeCommitHttpsCredentials.builder()
                                                            .type(AwsCodeCommitHttpsAuthType.ACCESS_KEY_AND_SECRET_KEY)
                                                            .httpCredentialsSpec(secretKeyAccessKeyDTO)
                                                            .build();
    AwsCodeCommitAuthentication authenticationDTO = AwsCodeCommitAuthentication.builder()
                                                        .authType(AwsCodeCommitAuthType.HTTPS)
                                                        .credentials(httpsCredentialsDTO)
                                                        .build();
    AwsCodeCommitConnector connectorDTO = AwsCodeCommitConnector.builder()
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
    assertThat(((io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitSecretKeyAccessKey)
                       awsCodeCommitConfig.getAuthentication()
                           .getCredential())
                   .getAccessKey())
        .isEqualTo(accessKey);
    assertThat(((io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitSecretKeyAccessKey)
                       awsCodeCommitConfig.getAuthentication()
                           .getCredential())
                   .getSecretKeyRef())
        .isEqualTo(secretKeySecretRefData.toSecretRefStringValue());
  }
}