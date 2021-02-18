package io.harness.connector.mappers.githubconnector;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.delegate.beans.connector.scm.github.GithubApiAccessType.GITHUB_APP;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.githubconnector.GithubAppApiAccess;
import io.harness.connector.entities.embedded.githubconnector.GithubConnector;
import io.harness.connector.entities.embedded.githubconnector.GithubHttpAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubSshAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubTokenApiAccess;
import io.harness.connector.entities.embedded.githubconnector.GithubUsernamePassword;
import io.harness.connector.entities.embedded.githubconnector.GithubUsernameToken;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessType;
import io.harness.delegate.beans.connector.scm.github.GithubAppSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameTokenDTO;
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

public class GithubDTOToEntityTest extends CategoryTest {
  @InjectMocks GithubDTOToEntity githubDTOToEntity;
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

    final GithubAuthenticationDTO githubAuthenticationDTO =
        GithubAuthenticationDTO.builder()
            .authType(HTTP)
            .credentials(
                GithubHttpCredentialsDTO.builder()
                    .type(GithubHttpAuthenticationType.USERNAME_AND_PASSWORD)
                    .httpCredentialsSpec(
                        GithubUsernamePasswordDTO.builder().passwordRef(passwordSecretRef).username(username).build())
                    .build())
            .build();

    final GithubApiAccessDTO githubApiAccessDTO = GithubApiAccessDTO.builder()
                                                      .type(GITHUB_APP)
                                                      .spec(GithubAppSpecDTO.builder()
                                                                .applicationId(appId)
                                                                .installationId(insId)
                                                                .privateKeyRef(privateSecretRef)
                                                                .build())
                                                      .build();
    final GithubConnectorDTO githubConnectorDTO = GithubConnectorDTO.builder()
                                                      .url(url)
                                                      .connectionType(GitConnectionType.REPO)
                                                      .authentication(githubAuthenticationDTO)
                                                      .apiAccess(githubApiAccessDTO)
                                                      .build();
    final GithubConnector githubConnector = githubDTOToEntity.toConnectorEntity(githubConnectorDTO, ngAccess);
    assertThat(githubConnector).isNotNull();
    assertThat(githubConnector.getUrl()).isEqualTo(url);
    assertThat(githubConnector.getApiAccessType()).isEqualTo(GITHUB_APP);
    assertThat(githubConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(githubConnector.getAuthenticationDetails())
        .isEqualTo(GithubHttpAuthentication.builder()
                       .type(GithubHttpAuthenticationType.USERNAME_AND_PASSWORD)
                       .auth(GithubUsernamePassword.builder()
                                 .username(username)
                                 .passwordRef(passwordSecretRef.toSecretRefStringValue())
                                 .build())
                       .build());
    assertThat(githubConnector.getGithubApiAccess())
        .isEqualTo(GithubAppApiAccess.builder()
                       .applicationId(appId)
                       .installationId(insId)
                       .privateKeyRef(privateSecretRef.toSecretRefStringValue())
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

    SecretRefData privateSecretRef = SecretRefData.builder().scope(Scope.ACCOUNT).identifier(privateIdentifier).build();
    when(secretRefService.validateAndGetSecretConfigString(privateSecretRef, ngAccess))
        .thenReturn(privateSecretRef.toSecretRefStringValue());

    SecretRefData usernameSecretRef =
        SecretRefData.builder().scope(Scope.ACCOUNT).identifier(usernameIdentifier).build();
    when(secretRefService.validateAndGetSecretConfigString(usernameSecretRef, ngAccess))
        .thenReturn(usernameSecretRef.toSecretRefStringValue());

    final GithubAuthenticationDTO githubAuthenticationDTO =
        GithubAuthenticationDTO.builder()
            .authType(HTTP)
            .credentials(GithubHttpCredentialsDTO.builder()
                             .type(GithubHttpAuthenticationType.USERNAME_AND_PASSWORD)
                             .httpCredentialsSpec(GithubUsernamePasswordDTO.builder()
                                                      .passwordRef(passwordSecretRef)
                                                      .usernameRef(usernameSecretRef)
                                                      .build())
                             .build())
            .build();

    final GithubApiAccessDTO githubApiAccessDTO = GithubApiAccessDTO.builder()
                                                      .type(GITHUB_APP)
                                                      .spec(GithubAppSpecDTO.builder()
                                                                .applicationId(appId)
                                                                .installationId(insId)
                                                                .privateKeyRef(privateSecretRef)
                                                                .build())
                                                      .build();
    final GithubConnectorDTO githubConnectorDTO = GithubConnectorDTO.builder()
                                                      .url(url)
                                                      .connectionType(GitConnectionType.REPO)
                                                      .authentication(githubAuthenticationDTO)
                                                      .apiAccess(githubApiAccessDTO)
                                                      .build();
    final GithubConnector githubConnector = githubDTOToEntity.toConnectorEntity(githubConnectorDTO, ngAccess);
    assertThat(githubConnector).isNotNull();
    assertThat(githubConnector.getUrl()).isEqualTo(url);
    assertThat(githubConnector.getApiAccessType()).isEqualTo(GITHUB_APP);
    assertThat(githubConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(githubConnector.getAuthenticationDetails())
        .isEqualTo(GithubHttpAuthentication.builder()
                       .type(GithubHttpAuthenticationType.USERNAME_AND_PASSWORD)
                       .auth(GithubUsernamePassword.builder()
                                 .usernameRef(usernameSecretRef.toSecretRefStringValue())
                                 .passwordRef(passwordSecretRef.toSecretRefStringValue())
                                 .build())
                       .build());
    assertThat(githubConnector.getGithubApiAccess())
        .isEqualTo(GithubAppApiAccess.builder()
                       .applicationId(appId)
                       .installationId(insId)
                       .privateKeyRef(privateSecretRef.toSecretRefStringValue())
                       .build());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_2() {
    final String url = "url";
    final String tokenIdentifier = "tokenIdentifier";

    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();
    SecretRefData tokenSecretRef = SecretRefData.builder().scope(Scope.ACCOUNT).identifier(tokenIdentifier).build();
    when(secretRefService.validateAndGetSecretConfigString(tokenSecretRef, ngAccess))
        .thenReturn(tokenSecretRef.toSecretRefStringValue());

    final GithubAuthenticationDTO githubAuthenticationDTO =
        GithubAuthenticationDTO.builder()
            .authType(HTTP)
            .credentials(GithubHttpCredentialsDTO.builder()
                             .type(GithubHttpAuthenticationType.USERNAME_AND_TOKEN)
                             .httpCredentialsSpec(GithubUsernameTokenDTO.builder().tokenRef(tokenSecretRef).build())
                             .build())
            .build();

    final GithubApiAccessDTO githubApiAccessDTO =
        GithubApiAccessDTO.builder()
            .type(GithubApiAccessType.TOKEN)
            .spec(GithubTokenSpecDTO.builder().tokenRef(tokenSecretRef).build())
            .build();
    final GithubConnectorDTO githubConnectorDTO = GithubConnectorDTO.builder()
                                                      .url(url)
                                                      .connectionType(GitConnectionType.REPO)
                                                      .authentication(githubAuthenticationDTO)
                                                      .apiAccess(githubApiAccessDTO)
                                                      .build();
    final GithubConnector githubConnector = githubDTOToEntity.toConnectorEntity(githubConnectorDTO, ngAccess);
    assertThat(githubConnector).isNotNull();
    assertThat(githubConnector.getUrl()).isEqualTo(url);
    assertThat(githubConnector.getApiAccessType()).isEqualTo(GithubApiAccessType.TOKEN);
    assertThat(githubConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(githubConnector.getAuthenticationDetails())
        .isEqualTo(GithubHttpAuthentication.builder()
                       .type(GithubHttpAuthenticationType.USERNAME_AND_TOKEN)
                       .auth(GithubUsernameToken.builder().tokenRef(tokenSecretRef.toSecretRefStringValue()).build())
                       .build());
    assertThat(githubConnector.getGithubApiAccess())
        .isEqualTo(GithubTokenApiAccess.builder().tokenRef(tokenSecretRef.toSecretRefStringValue()).build());
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

    final GithubAuthenticationDTO githubAuthenticationDTO =
        GithubAuthenticationDTO.builder()
            .authType(GitAuthType.SSH)
            .credentials(GithubSshCredentialsDTO.builder().sshKeyRef(sshSecretRef).build())
            .build();

    final GithubConnectorDTO githubConnectorDTO = GithubConnectorDTO.builder()
                                                      .url(url)
                                                      .connectionType(GitConnectionType.REPO)
                                                      .authentication(githubAuthenticationDTO)
                                                      .build();
    final GithubConnector githubConnector = githubDTOToEntity.toConnectorEntity(
        githubConnectorDTO, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
    assertThat(githubConnector).isNotNull();
    assertThat(githubConnector.getUrl()).isEqualTo(url);
    assertThat(githubConnector.getAuthType()).isEqualTo(GitAuthType.SSH);
    assertThat(githubConnector.getAuthenticationDetails())
        .isEqualTo(GithubSshAuthentication.builder().sshKeyRef(sshSecretRef.toSecretRefStringValue()).build());
    assertThat(githubConnector.getGithubApiAccess()).isNull();
  }
}