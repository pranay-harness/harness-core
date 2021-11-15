package io.harness.connector.mappers.githubconnector;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.delegate.beans.connector.scm.github.GithubApiAccessType.GITHUB_APP;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.githubconnector.GithubAppApiAccess;
import io.harness.connector.entities.embedded.githubconnector.GithubHttpAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubSshAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubTokenApiAccess;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccess;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessType;
import io.harness.delegate.beans.connector.scm.github.GithubAppSpec;
import io.harness.delegate.beans.connector.scm.github.GithubAuthentication;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentials;
import io.harness.delegate.beans.connector.scm.github.GithubSshCredentials;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpec;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePassword;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameToken;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class GithubDTOToEntityTest extends CategoryTest {
  @InjectMocks GithubDTOToEntity githubDTOToEntity;

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

    final GithubAuthentication githubAuthentication =
        GithubAuthentication.builder()
            .authType(HTTP)
            .credentials(GithubHttpCredentials.builder()
                             .type(GithubHttpAuthenticationType.USERNAME_AND_PASSWORD)
                             .httpCredentialsSpec(GithubUsernamePassword.builder()
                                                      .passwordRef(SecretRefHelper.createSecretRef(passwordRef))
                                                      .username(username)
                                                      .build())
                             .build())
            .build();

    final GithubApiAccess githubApiAccess = GithubApiAccess.builder()
                                                .type(GITHUB_APP)
                                                .spec(GithubAppSpec.builder()
                                                          .applicationId(appId)
                                                          .installationId(insId)
                                                          .privateKeyRef(SecretRefHelper.createSecretRef(privateKeyRef))
                                                          .build())
                                                .build();
    final GithubConnector githubConnectorDTO = GithubConnector.builder()
                                                   .url(url)
                                                   .validationRepo(validationRepo)
                                                   .connectionType(GitConnectionType.ACCOUNT)
                                                   .authentication(githubAuthentication)
                                                   .apiAccess(githubApiAccess)
                                                   .build();
    final io.harness.connector.entities.embedded.githubconnector.GithubConnector githubConnector =
        githubDTOToEntity.toConnectorEntity(githubConnectorDTO);
    assertThat(githubConnector).isNotNull();
    assertThat(githubConnector.getUrl()).isEqualTo(url);
    assertThat(githubConnector.getValidationRepo()).isEqualTo(validationRepo);
    assertThat(githubConnector.getApiAccessType()).isEqualTo(GITHUB_APP);
    assertThat(githubConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(githubConnector.getAuthenticationDetails())
        .isEqualTo(GithubHttpAuthentication.builder()
                       .type(GithubHttpAuthenticationType.USERNAME_AND_PASSWORD)
                       .auth(io.harness.connector.entities.embedded.githubconnector.GithubUsernamePassword.builder()
                                 .username(username)
                                 .passwordRef(passwordRef)
                                 .build())
                       .build());
    assertThat(githubConnector.getGithubApiAccess())
        .isEqualTo(GithubAppApiAccess.builder()
                       .applicationId(appId)
                       .installationId(insId)
                       .privateKeyRef(privateKeyRef)
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

    final GithubAuthentication githubAuthentication =
        GithubAuthentication.builder()
            .authType(HTTP)
            .credentials(GithubHttpCredentials.builder()
                             .type(GithubHttpAuthenticationType.USERNAME_AND_PASSWORD)
                             .httpCredentialsSpec(GithubUsernamePassword.builder()
                                                      .passwordRef(SecretRefHelper.createSecretRef(passwordRef))
                                                      .usernameRef(SecretRefHelper.createSecretRef(usernameRef))
                                                      .build())
                             .build())
            .build();

    final GithubApiAccess githubApiAccess = GithubApiAccess.builder()
                                                .type(GITHUB_APP)
                                                .spec(GithubAppSpec.builder()
                                                          .applicationId(appId)
                                                          .installationId(insId)
                                                          .privateKeyRef(SecretRefHelper.createSecretRef(privateKeyRef))
                                                          .build())
                                                .build();
    final GithubConnector githubConnectorDTO = GithubConnector.builder()
                                                   .url(url)
                                                   .connectionType(GitConnectionType.REPO)
                                                   .authentication(githubAuthentication)
                                                   .apiAccess(githubApiAccess)
                                                   .build();
    final io.harness.connector.entities.embedded.githubconnector.GithubConnector githubConnector =
        githubDTOToEntity.toConnectorEntity(githubConnectorDTO);
    assertThat(githubConnector).isNotNull();
    assertThat(githubConnector.getUrl()).isEqualTo(url);
    assertThat(githubConnector.getApiAccessType()).isEqualTo(GITHUB_APP);
    assertThat(githubConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(githubConnector.getAuthenticationDetails())
        .isEqualTo(GithubHttpAuthentication.builder()
                       .type(GithubHttpAuthenticationType.USERNAME_AND_PASSWORD)
                       .auth(io.harness.connector.entities.embedded.githubconnector.GithubUsernamePassword.builder()
                                 .usernameRef(usernameRef)
                                 .passwordRef(passwordRef)
                                 .build())
                       .build());
    assertThat(githubConnector.getGithubApiAccess())
        .isEqualTo(GithubAppApiAccess.builder()
                       .applicationId(appId)
                       .installationId(insId)
                       .privateKeyRef(privateKeyRef)
                       .build());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_2() {
    final String url = "url";
    final String tokenRef = "tokenRef";
    final GithubAuthentication githubAuthentication =
        GithubAuthentication.builder()
            .authType(HTTP)
            .credentials(
                GithubHttpCredentials.builder()
                    .type(GithubHttpAuthenticationType.USERNAME_AND_TOKEN)
                    .httpCredentialsSpec(
                        GithubUsernameToken.builder().tokenRef(SecretRefHelper.createSecretRef(tokenRef)).build())
                    .build())
            .build();

    final GithubApiAccess githubApiAccess =
        GithubApiAccess.builder()
            .type(GithubApiAccessType.TOKEN)
            .spec(GithubTokenSpec.builder().tokenRef(SecretRefHelper.createSecretRef(tokenRef)).build())
            .build();
    final GithubConnector githubConnectorDTO = GithubConnector.builder()
                                                   .url(url)
                                                   .connectionType(GitConnectionType.REPO)
                                                   .authentication(githubAuthentication)
                                                   .apiAccess(githubApiAccess)
                                                   .build();
    final io.harness.connector.entities.embedded.githubconnector.GithubConnector githubConnector =
        githubDTOToEntity.toConnectorEntity(githubConnectorDTO);
    assertThat(githubConnector).isNotNull();
    assertThat(githubConnector.getUrl()).isEqualTo(url);
    assertThat(githubConnector.getApiAccessType()).isEqualTo(GithubApiAccessType.TOKEN);
    assertThat(githubConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(githubConnector.getAuthenticationDetails())
        .isEqualTo(GithubHttpAuthentication.builder()
                       .type(GithubHttpAuthenticationType.USERNAME_AND_TOKEN)
                       .auth(io.harness.connector.entities.embedded.githubconnector.GithubUsernameToken.builder()
                                 .tokenRef(tokenRef)
                                 .build())
                       .build());
    assertThat(githubConnector.getGithubApiAccess())
        .isEqualTo(GithubTokenApiAccess.builder().tokenRef(tokenRef).build());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_3() {
    final String url = "url";
    final String sshKeyRef = "sshKeyRef";
    final GithubAuthentication githubAuthentication =
        GithubAuthentication.builder()
            .authType(GitAuthType.SSH)
            .credentials(GithubSshCredentials.builder().sshKeyRef(SecretRefHelper.createSecretRef(sshKeyRef)).build())
            .build();

    final GithubConnector githubConnectorDTO = GithubConnector.builder()
                                                   .url(url)
                                                   .connectionType(GitConnectionType.REPO)
                                                   .authentication(githubAuthentication)
                                                   .build();
    final io.harness.connector.entities.embedded.githubconnector.GithubConnector githubConnector =
        githubDTOToEntity.toConnectorEntity(githubConnectorDTO);
    assertThat(githubConnector).isNotNull();
    assertThat(githubConnector.getUrl()).isEqualTo(url);
    assertThat(githubConnector.getAuthType()).isEqualTo(GitAuthType.SSH);
    assertThat(githubConnector.getAuthenticationDetails())
        .isEqualTo(GithubSshAuthentication.builder().sshKeyRef(sshKeyRef).build());
    assertThat(githubConnector.getGithubApiAccess()).isNull();
  }
}