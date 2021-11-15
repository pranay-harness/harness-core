package io.harness.connector.mappers.gitlabconnector;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabHttpAuthentication;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabSshAuthentication;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabTokenApiAccess;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccess;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabAuthentication;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnector;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentials;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabKerberos;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabSshCredentials;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpec;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernamePassword;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernameToken;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class GitlabDTOToEntityTest extends CategoryTest {
  @InjectMocks GitlabDTOToEntity gitlabDTOToEntity;

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

    final GitlabAuthentication gitlabAuthentication =
        GitlabAuthentication.builder()
            .authType(HTTP)
            .credentials(GitlabHttpCredentials.builder()
                             .type(GitlabHttpAuthenticationType.USERNAME_AND_PASSWORD)
                             .httpCredentialsSpec(GitlabUsernamePassword.builder()
                                                      .passwordRef(SecretRefHelper.createSecretRef(passwordRef))
                                                      .username(username)
                                                      .build())
                             .build())
            .build();

    final GitlabConnector gitlabConnectorDTO = GitlabConnector.builder()
                                                   .url(url)
                                                   .validationRepo(validationRepo)
                                                   .connectionType(GitConnectionType.ACCOUNT)
                                                   .authentication(gitlabAuthentication)
                                                   .build();
    final io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector gitlabConnector =
        gitlabDTOToEntity.toConnectorEntity(gitlabConnectorDTO);
    assertThat(gitlabConnector).isNotNull();
    assertThat(gitlabConnector.getUrl()).isEqualTo(url);
    assertThat(gitlabConnector.getValidationRepo()).isEqualTo(validationRepo);
    assertThat(gitlabConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(gitlabConnector.getAuthenticationDetails())
        .isEqualTo(GitlabHttpAuthentication.builder()
                       .type(GitlabHttpAuthenticationType.USERNAME_AND_PASSWORD)
                       .auth(io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernamePassword.builder()
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

    final GitlabAuthentication gitlabAuthentication =
        GitlabAuthentication.builder()
            .authType(HTTP)
            .credentials(GitlabHttpCredentials.builder()
                             .type(GitlabHttpAuthenticationType.USERNAME_AND_PASSWORD)
                             .httpCredentialsSpec(GitlabUsernamePassword.builder()
                                                      .passwordRef(SecretRefHelper.createSecretRef(passwordRef))
                                                      .usernameRef(SecretRefHelper.createSecretRef(usernameRef))
                                                      .build())
                             .build())
            .build();

    final GitlabConnector gitlabConnectorDTO = GitlabConnector.builder()
                                                   .url(url)
                                                   .connectionType(GitConnectionType.REPO)
                                                   .authentication(gitlabAuthentication)
                                                   .build();
    final io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector gitlabConnector =
        gitlabDTOToEntity.toConnectorEntity(gitlabConnectorDTO);
    assertThat(gitlabConnector).isNotNull();
    assertThat(gitlabConnector.getUrl()).isEqualTo(url);
    assertThat(gitlabConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(gitlabConnector.getAuthenticationDetails())
        .isEqualTo(GitlabHttpAuthentication.builder()
                       .type(GitlabHttpAuthenticationType.USERNAME_AND_PASSWORD)
                       .auth(io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernamePassword.builder()
                                 .usernameRef(usernameRef)
                                 .passwordRef(passwordRef)
                                 .build())
                       .build());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_2() {
    final String url = "url";
    final String tokenRef = "tokenRef";
    final GitlabAuthentication gitlabAuthentication =
        GitlabAuthentication.builder()
            .authType(HTTP)
            .credentials(
                GitlabHttpCredentials.builder()
                    .type(GitlabHttpAuthenticationType.USERNAME_AND_TOKEN)
                    .httpCredentialsSpec(
                        GitlabUsernameToken.builder().tokenRef(SecretRefHelper.createSecretRef(tokenRef)).build())
                    .build())
            .build();

    final GitlabApiAccess gitlabApiAccess =
        GitlabApiAccess.builder()
            .type(GitlabApiAccessType.TOKEN)
            .spec(GitlabTokenSpec.builder().tokenRef(SecretRefHelper.createSecretRef(tokenRef)).build())
            .build();
    final GitlabConnector gitlabConnectorDTO = GitlabConnector.builder()
                                                   .url(url)
                                                   .connectionType(GitConnectionType.REPO)
                                                   .authentication(gitlabAuthentication)
                                                   .apiAccess(gitlabApiAccess)
                                                   .build();
    final io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector gitlabConnector =
        gitlabDTOToEntity.toConnectorEntity(gitlabConnectorDTO);
    assertThat(gitlabConnector).isNotNull();
    assertThat(gitlabConnector.getUrl()).isEqualTo(url);
    assertThat(gitlabConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(gitlabConnector.getAuthenticationDetails())
        .isEqualTo(GitlabHttpAuthentication.builder()
                       .type(GitlabHttpAuthenticationType.USERNAME_AND_TOKEN)
                       .auth(io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernameToken.builder()
                                 .tokenRef(tokenRef)
                                 .build())
                       .build());
    assertThat(gitlabConnector.getGitlabApiAccess())
        .isEqualTo(GitlabTokenApiAccess.builder().tokenRef(tokenRef).build());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_3() {
    final String url = "url";
    final String sshKeyRef = "sshKeyRef";
    final GitlabAuthentication gitlabAuthentication =
        GitlabAuthentication.builder()
            .authType(GitAuthType.SSH)
            .credentials(GitlabSshCredentials.builder().sshKeyRef(SecretRefHelper.createSecretRef(sshKeyRef)).build())
            .build();

    final GitlabConnector gitlabConnectorDTO = GitlabConnector.builder()
                                                   .url(url)
                                                   .connectionType(GitConnectionType.REPO)
                                                   .authentication(gitlabAuthentication)
                                                   .build();
    final io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector gitlabConnector =
        gitlabDTOToEntity.toConnectorEntity(gitlabConnectorDTO);
    assertThat(gitlabConnector).isNotNull();
    assertThat(gitlabConnector.getUrl()).isEqualTo(url);
    assertThat(gitlabConnector.getAuthType()).isEqualTo(GitAuthType.SSH);
    assertThat(gitlabConnector.getAuthenticationDetails())
        .isEqualTo(GitlabSshAuthentication.builder().sshKeyRef(sshKeyRef).build());
    assertThat(gitlabConnector.getGitlabApiAccess()).isNull();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_4() {
    final String url = "url";
    final String kerberosKeyRef = "tokenRef";
    final GitlabAuthentication gitlabAuthentication =
        GitlabAuthentication.builder()
            .authType(HTTP)
            .credentials(GitlabHttpCredentials.builder()
                             .type(GitlabHttpAuthenticationType.KERBEROS)
                             .httpCredentialsSpec(GitlabKerberos.builder()
                                                      .kerberosKeyRef(SecretRefHelper.createSecretRef(kerberosKeyRef))
                                                      .build())
                             .build())
            .build();

    final GitlabApiAccess gitlabApiAccess =
        GitlabApiAccess.builder()
            .type(GitlabApiAccessType.TOKEN)
            .spec(GitlabTokenSpec.builder().tokenRef(SecretRefHelper.createSecretRef(kerberosKeyRef)).build())
            .build();
    final GitlabConnector gitlabConnectorDTO = GitlabConnector.builder()
                                                   .url(url)
                                                   .connectionType(GitConnectionType.REPO)
                                                   .authentication(gitlabAuthentication)
                                                   .apiAccess(gitlabApiAccess)
                                                   .build();
    final io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector gitlabConnector =
        gitlabDTOToEntity.toConnectorEntity(gitlabConnectorDTO);
    assertThat(gitlabConnector).isNotNull();
    assertThat(gitlabConnector.getUrl()).isEqualTo(url);
    assertThat(gitlabConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(gitlabConnector.getAuthenticationDetails())
        .isEqualTo(GitlabHttpAuthentication.builder()
                       .type(GitlabHttpAuthenticationType.KERBEROS)
                       .auth(io.harness.connector.entities.embedded.gitlabconnector.GitlabKerberos.builder()
                                 .kerberosKeyRef(kerberosKeyRef)
                                 .build())
                       .build());
    assertThat(gitlabConnector.getGitlabApiAccess())
        .isEqualTo(GitlabTokenApiAccess.builder().tokenRef(kerberosKeyRef).build());
  }
}