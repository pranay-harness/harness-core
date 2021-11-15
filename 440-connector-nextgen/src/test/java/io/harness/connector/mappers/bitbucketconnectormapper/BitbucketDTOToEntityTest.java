package io.harness.connector.mappers.bitbucketconnectormapper;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketSshAuthentication;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketAuthentication;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentials;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketSshCredentials;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernamePassword;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class BitbucketDTOToEntityTest extends CategoryTest {
  @InjectMocks BitbucketDTOToEntity bitbucketDTOToEntity;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_0() {
    final String url = "url";
    final String passwordRef = "passwordRef";
    final String username = "username";
    final String appId = "appId";
    final String insId = "insId";
    final String privateKeyRef = "privateKeyRef";
    final String validationRepo = "validationRepo";

    final BitbucketAuthentication bitbucketAuthentication =
        BitbucketAuthentication.builder()
            .authType(HTTP)
            .credentials(BitbucketHttpCredentials.builder()
                             .type(BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD)
                             .httpCredentialsSpec(BitbucketUsernamePassword.builder()
                                                      .passwordRef(SecretRefHelper.createSecretRef(passwordRef))
                                                      .username(username)
                                                      .build())
                             .build())
            .build();

    final BitbucketConnector bitbucketConnectorDTO = BitbucketConnector.builder()
                                                         .url(url)
                                                         .validationRepo(validationRepo)
                                                         .connectionType(GitConnectionType.ACCOUNT)
                                                         .authentication(bitbucketAuthentication)
                                                         .build();
    final io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector bitbucketConnector =
        bitbucketDTOToEntity.toConnectorEntity(bitbucketConnectorDTO);
    assertThat(bitbucketConnector).isNotNull();
    assertThat(bitbucketConnector.getUrl()).isEqualTo(url);
    assertThat(bitbucketConnector.getValidationRepo()).isEqualTo(validationRepo);
    assertThat(bitbucketConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(bitbucketConnector.getAuthenticationDetails())
        .isEqualTo(
            BitbucketHttpAuthentication.builder()
                .type(BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD)
                .auth(io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePassword.builder()
                          .username(username)
                          .passwordRef(passwordRef)
                          .build())
                .build());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_1() {
    final String url = "url";
    final String passwordRef = "passwordRef";
    final String usernameRef = "usernameRef";
    final String appId = "appId";
    final String insId = "insId";
    final String privateKeyRef = "privateKeyRef";

    final BitbucketAuthentication bitbucketAuthentication =
        BitbucketAuthentication.builder()
            .authType(HTTP)
            .credentials(BitbucketHttpCredentials.builder()
                             .type(BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD)
                             .httpCredentialsSpec(BitbucketUsernamePassword.builder()
                                                      .passwordRef(SecretRefHelper.createSecretRef(passwordRef))
                                                      .usernameRef(SecretRefHelper.createSecretRef(usernameRef))
                                                      .build())
                             .build())
            .build();

    final BitbucketConnector bitbucketConnectorDTO = BitbucketConnector.builder()
                                                         .url(url)
                                                         .connectionType(GitConnectionType.REPO)
                                                         .authentication(bitbucketAuthentication)
                                                         .build();
    final io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector bitbucketConnector =
        bitbucketDTOToEntity.toConnectorEntity(bitbucketConnectorDTO);
    assertThat(bitbucketConnector).isNotNull();
    assertThat(bitbucketConnector.getUrl()).isEqualTo(url);
    assertThat(bitbucketConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(bitbucketConnector.getAuthenticationDetails())
        .isEqualTo(
            BitbucketHttpAuthentication.builder()
                .type(BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD)
                .auth(io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePassword.builder()
                          .usernameRef(usernameRef)
                          .passwordRef(passwordRef)
                          .build())
                .build());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_3() {
    final String url = "url";
    final String sshKeyRef = "sshKeyRef";
    final BitbucketAuthentication bitbucketAuthentication =
        BitbucketAuthentication.builder()
            .authType(GitAuthType.SSH)
            .credentials(
                BitbucketSshCredentials.builder().sshKeyRef(SecretRefHelper.createSecretRef(sshKeyRef)).build())
            .build();

    final BitbucketConnector bitbucketConnectorDTO = BitbucketConnector.builder()
                                                         .url(url)
                                                         .connectionType(GitConnectionType.REPO)
                                                         .authentication(bitbucketAuthentication)
                                                         .build();
    final io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector bitbucketConnector =
        bitbucketDTOToEntity.toConnectorEntity(bitbucketConnectorDTO);
    assertThat(bitbucketConnector).isNotNull();
    assertThat(bitbucketConnector.getUrl()).isEqualTo(url);
    assertThat(bitbucketConnector.getAuthType()).isEqualTo(GitAuthType.SSH);
    assertThat(bitbucketConnector.getAuthenticationDetails())
        .isEqualTo(BitbucketSshAuthentication.builder().sshKeyRef(sshKeyRef).build());
    assertThat(bitbucketConnector.getBitbucketApiAccess()).isNull();
  }
}