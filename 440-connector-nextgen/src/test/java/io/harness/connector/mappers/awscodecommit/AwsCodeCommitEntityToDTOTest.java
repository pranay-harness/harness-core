package io.harness.connector.mappers.awscodecommit;

import static io.harness.encryption.Scope.ACCOUNT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitAuthentication;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitConfig;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnector;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsCredentials;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitSecretKeyAccessKey;
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
    io.harness.connector.entities.embedded.awscodecommitconnector
        .AwsCodeCommitSecretKeyAccessKey awsCodeCommitSecretKeyAccessKey =
        io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitSecretKeyAccessKey.builder()
            .accessKey(accessKey)
            .secretKeyRef(secretKeyRef)
            .build();
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
    AwsCodeCommitConnector connectorDTO = awsCodeCommitEntityToDTO.createConnectorDTO(awsCodeCommitConfig);
    assertThat(connectorDTO).isNotNull();
    assertThat(connectorDTO.getUrl()).isEqualTo(url);
    assertThat(connectorDTO.getUrlType()).isEqualTo(AwsCodeCommitUrlType.REPO);
    assertThat(connectorDTO.getAuthentication().getAuthType()).isEqualTo(AwsCodeCommitAuthType.HTTPS);
    assertThat(((AwsCodeCommitHttpsCredentials) connectorDTO.getAuthentication().getCredentials()).getType())
        .isEqualTo(AwsCodeCommitHttpsAuthType.ACCESS_KEY_AND_SECRET_KEY);
    AwsCodeCommitSecretKeyAccessKey secretKeyAccessKeyDTO =
        (AwsCodeCommitSecretKeyAccessKey) ((AwsCodeCommitHttpsCredentials) connectorDTO.getAuthentication()
                                               .getCredentials())
            .getHttpCredentialsSpec();

    assertThat(secretKeyAccessKeyDTO.getAccessKey()).isEqualTo(accessKey);
    assertThat(secretKeyAccessKeyDTO.getSecretKeyRef()).isEqualTo(SecretRefHelper.createSecretRef(secretKeyRef));
  }
}