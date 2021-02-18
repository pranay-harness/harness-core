package io.harness.connector.mappers.gitlabconnector;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabHttpAuthentication;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabKerberos;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabSshAuthentication;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabTokenApiAccess;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernamePassword;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernameToken;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabKerberosDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernameTokenDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.service.SecretRefService;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GitlabDTOToEntityTest extends CategoryTest {
  @InjectMocks GitlabDTOToEntity gitlabDTOToEntity;
  @Mock SecretRefService secretRefService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_0() {
    final String url = "url";
    final String passwordIdentifier = "passwordRef";
    final String username = "username";
    final String appId = "appId";
    final String insId = "insId";
    final String privateIdentifier = "privateKeyRef";

    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();
    SecretRefData passwordSecretRef =
        SecretRefData.builder().scope(Scope.ACCOUNT).identifier(passwordIdentifier).build();
    when(secretRefService.validateAndGetSecretConfigString(passwordSecretRef, ngAccess))
        .thenReturn(passwordSecretRef.toSecretRefStringValue());

    SecretRefData privateSecretRef = SecretRefData.builder().scope(Scope.ACCOUNT).identifier(privateIdentifier).build();
    when(secretRefService.validateAndGetSecretConfigString(privateSecretRef, ngAccess))
        .thenReturn(privateSecretRef.toSecretRefStringValue());

    final GitlabAuthenticationDTO gitlabAuthenticationDTO =
        GitlabAuthenticationDTO.builder()
            .authType(HTTP)
            .credentials(
                GitlabHttpCredentialsDTO.builder()
                    .type(GitlabHttpAuthenticationType.USERNAME_AND_PASSWORD)
                    .httpCredentialsSpec(
                        GitlabUsernamePasswordDTO.builder().passwordRef(passwordSecretRef).username(username).build())
                    .build())
            .build();

    final GitlabConnectorDTO gitlabConnectorDTO = GitlabConnectorDTO.builder()
                                                      .url(url)
                                                      .connectionType(GitConnectionType.REPO)
                                                      .authentication(gitlabAuthenticationDTO)
                                                      .build();
    final GitlabConnector gitlabConnector = gitlabDTOToEntity.toConnectorEntity(gitlabConnectorDTO, ngAccess);
    assertThat(gitlabConnector).isNotNull();
    assertThat(gitlabConnector.getUrl()).isEqualTo(url);
    assertThat(gitlabConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(gitlabConnector.getAuthenticationDetails())
        .isEqualTo(GitlabHttpAuthentication.builder()
                       .type(GitlabHttpAuthenticationType.USERNAME_AND_PASSWORD)
                       .auth(GitlabUsernamePassword.builder()
                                 .username(username)
                                 .passwordRef(passwordSecretRef.toSecretRefStringValue())
                                 .build())
                       .build());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_1() {
    final String url = "url";
    final String passwordIdentifier = "passwordRef";
    final String usernameIdentifier = "usernameRef";
    final String appId = "appId";
    final String insId = "insId";
    final String privateIdentifier = "privateKeyRef";

    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();
    SecretRefData passwordSecretRef =
        SecretRefData.builder().scope(Scope.ACCOUNT).identifier(passwordIdentifier).build();
    when(secretRefService.validateAndGetSecretConfigString(passwordSecretRef, ngAccess))
        .thenReturn(passwordSecretRef.toSecretRefStringValue());

    SecretRefData usernameSecretRef =
        SecretRefData.builder().scope(Scope.ACCOUNT).identifier(privateIdentifier).build();
    when(secretRefService.validateAndGetSecretConfigString(usernameSecretRef, ngAccess))
        .thenReturn(usernameSecretRef.toSecretRefStringValue());

    final GitlabAuthenticationDTO gitlabAuthenticationDTO =
        GitlabAuthenticationDTO.builder()
            .authType(HTTP)
            .credentials(GitlabHttpCredentialsDTO.builder()
                             .type(GitlabHttpAuthenticationType.USERNAME_AND_PASSWORD)
                             .httpCredentialsSpec(GitlabUsernamePasswordDTO.builder()
                                                      .passwordRef(passwordSecretRef)
                                                      .usernameRef(usernameSecretRef)
                                                      .build())
                             .build())
            .build();

    final GitlabConnectorDTO gitlabConnectorDTO = GitlabConnectorDTO.builder()
                                                      .url(url)
                                                      .connectionType(GitConnectionType.REPO)
                                                      .authentication(gitlabAuthenticationDTO)
                                                      .build();
    final GitlabConnector gitlabConnector = gitlabDTOToEntity.toConnectorEntity(gitlabConnectorDTO, ngAccess);
    assertThat(gitlabConnector).isNotNull();
    assertThat(gitlabConnector.getUrl()).isEqualTo(url);
    assertThat(gitlabConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(gitlabConnector.getAuthenticationDetails())
        .isEqualTo(GitlabHttpAuthentication.builder()
                       .type(GitlabHttpAuthenticationType.USERNAME_AND_PASSWORD)
                       .auth(GitlabUsernamePassword.builder()
                                 .usernameRef(usernameSecretRef.toSecretRefStringValue())
                                 .passwordRef(passwordSecretRef.toSecretRefStringValue())
                                 .build())
                       .build());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_2() {
    final String url = "url";
    final String tokenIdentifier = "tokenRef";

    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();
    SecretRefData tokenSecretRef = SecretRefData.builder().scope(Scope.ACCOUNT).identifier(tokenIdentifier).build();
    when(secretRefService.validateAndGetSecretConfigString(tokenSecretRef, ngAccess))
        .thenReturn(tokenSecretRef.toSecretRefStringValue());

    final GitlabAuthenticationDTO gitlabAuthenticationDTO =
        GitlabAuthenticationDTO.builder()
            .authType(HTTP)
            .credentials(GitlabHttpCredentialsDTO.builder()
                             .type(GitlabHttpAuthenticationType.USERNAME_AND_TOKEN)
                             .httpCredentialsSpec(GitlabUsernameTokenDTO.builder().tokenRef(tokenSecretRef).build())
                             .build())
            .build();

    final GitlabApiAccessDTO gitlabApiAccessDTO =
        GitlabApiAccessDTO.builder()
            .type(GitlabApiAccessType.TOKEN)
            .spec(GitlabTokenSpecDTO.builder().tokenRef(tokenSecretRef).build())
            .build();
    final GitlabConnectorDTO gitlabConnectorDTO = GitlabConnectorDTO.builder()
                                                      .url(url)
                                                      .connectionType(GitConnectionType.REPO)
                                                      .authentication(gitlabAuthenticationDTO)
                                                      .apiAccess(gitlabApiAccessDTO)
                                                      .build();
    final GitlabConnector gitlabConnector = gitlabDTOToEntity.toConnectorEntity(gitlabConnectorDTO, ngAccess);
    assertThat(gitlabConnector).isNotNull();
    assertThat(gitlabConnector.getUrl()).isEqualTo(url);
    assertThat(gitlabConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(gitlabConnector.getAuthenticationDetails())
        .isEqualTo(GitlabHttpAuthentication.builder()
                       .type(GitlabHttpAuthenticationType.USERNAME_AND_TOKEN)
                       .auth(GitlabUsernameToken.builder().tokenRef(tokenSecretRef.toSecretRefStringValue()).build())
                       .build());
    assertThat(gitlabConnector.getGitlabApiAccess())
        .isEqualTo(GitlabTokenApiAccess.builder().tokenRef(tokenSecretRef.toSecretRefStringValue()).build());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_3() {
    final String url = "url";
    final String sshKeyIdentifier = "sshKeyRef";

    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();
    SecretRefData sshSecretRef = SecretRefData.builder().scope(Scope.ACCOUNT).identifier(sshKeyIdentifier).build();
    when(secretRefService.validateAndGetSecretConfigString(sshSecretRef, ngAccess))
        .thenReturn(sshSecretRef.toSecretRefStringValue());

    final GitlabAuthenticationDTO gitlabAuthenticationDTO =
        GitlabAuthenticationDTO.builder()
            .authType(GitAuthType.SSH)
            .credentials(GitlabSshCredentialsDTO.builder().sshKeyRef(sshSecretRef).build())
            .build();

    final GitlabConnectorDTO gitlabConnectorDTO = GitlabConnectorDTO.builder()
                                                      .url(url)
                                                      .connectionType(GitConnectionType.REPO)
                                                      .authentication(gitlabAuthenticationDTO)
                                                      .build();
    final GitlabConnector gitlabConnector = gitlabDTOToEntity.toConnectorEntity(gitlabConnectorDTO, ngAccess);
    assertThat(gitlabConnector).isNotNull();
    assertThat(gitlabConnector.getUrl()).isEqualTo(url);
    assertThat(gitlabConnector.getAuthType()).isEqualTo(GitAuthType.SSH);
    assertThat(gitlabConnector.getAuthenticationDetails())
        .isEqualTo(GitlabSshAuthentication.builder().sshKeyRef(sshSecretRef.toSecretRefStringValue()).build());
    assertThat(gitlabConnector.getGitlabApiAccess()).isNull();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_4() {
    final String url = "url";
    final String kerberosKeyIdentifier = "tokenRef";

    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();
    SecretRefData kerberosSecretRef =
        SecretRefData.builder().scope(Scope.ACCOUNT).identifier(kerberosKeyIdentifier).build();
    when(secretRefService.validateAndGetSecretConfigString(kerberosSecretRef, ngAccess))
        .thenReturn(kerberosSecretRef.toSecretRefStringValue());

    final GitlabAuthenticationDTO gitlabAuthenticationDTO =
        GitlabAuthenticationDTO.builder()
            .authType(HTTP)
            .credentials(GitlabHttpCredentialsDTO.builder()
                             .type(GitlabHttpAuthenticationType.KERBEROS)
                             .httpCredentialsSpec(GitlabKerberosDTO.builder().kerberosKeyRef(kerberosSecretRef).build())
                             .build())
            .build();

    final GitlabApiAccessDTO gitlabApiAccessDTO =
        GitlabApiAccessDTO.builder()
            .type(GitlabApiAccessType.TOKEN)
            .spec(GitlabTokenSpecDTO.builder().tokenRef(kerberosSecretRef).build())
            .build();
    final GitlabConnectorDTO gitlabConnectorDTO = GitlabConnectorDTO.builder()
                                                      .url(url)
                                                      .connectionType(GitConnectionType.REPO)
                                                      .authentication(gitlabAuthenticationDTO)
                                                      .apiAccess(gitlabApiAccessDTO)
                                                      .build();
    final GitlabConnector gitlabConnector = gitlabDTOToEntity.toConnectorEntity(gitlabConnectorDTO, ngAccess);
    assertThat(gitlabConnector).isNotNull();
    assertThat(gitlabConnector.getUrl()).isEqualTo(url);
    assertThat(gitlabConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(gitlabConnector.getAuthenticationDetails())
        .isEqualTo(
            GitlabHttpAuthentication.builder()
                .type(GitlabHttpAuthenticationType.KERBEROS)
                .auth(GitlabKerberos.builder().kerberosKeyRef(kerberosSecretRef.toSecretRefStringValue()).build())
                .build());
    assertThat(gitlabConnector.getGitlabApiAccess())
        .isEqualTo(GitlabTokenApiAccess.builder().tokenRef(kerberosSecretRef.toSecretRefStringValue()).build());
  }
}