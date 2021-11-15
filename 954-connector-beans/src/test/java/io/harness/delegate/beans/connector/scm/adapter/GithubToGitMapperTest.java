package io.harness.delegate.beans.connector.scm.adapter;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.delegate.beans.connector.scm.GitAuthType.SSH;
import static io.harness.delegate.beans.connector.scm.GitConnectionType.ACCOUNT;
import static io.harness.delegate.beans.connector.scm.GitConnectionType.REPO;
import static io.harness.delegate.beans.connector.scm.github.GithubApiAccessType.GITHUB_APP;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthentication;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthentication;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccess;
import io.harness.delegate.beans.connector.scm.github.GithubAppSpec;
import io.harness.delegate.beans.connector.scm.github.GithubAuthentication;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentials;
import io.harness.delegate.beans.connector.scm.github.GithubSshCredentials;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePassword;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;

import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

@OwnedBy(DX)
public class GithubToGitMapperTest extends CategoryTest {
  Set<String> delegateSelectors = new HashSet<>();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    delegateSelectors.add("abc");
    delegateSelectors.add("def");
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testMappingToHTTPGitConfigDTO() {
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
    final GithubConnector githubConnector = GithubConnector.builder()
                                                .url(url)
                                                .validationRepo(validationRepo)
                                                .connectionType(GitConnectionType.ACCOUNT)
                                                .authentication(githubAuthentication)
                                                .apiAccess(githubApiAccess)
                                                .delegateSelectors(delegateSelectors)
                                                .build();
    GitConfig gitConfig = GithubToGitMapper.mapToGitConfigDTO(githubConnector);
    assertThat(gitConfig).isNotNull();
    assertThat(gitConfig.getGitAuthType()).isEqualTo(HTTP);
    assertThat(gitConfig.getDelegateSelectors()).isEqualTo(delegateSelectors);
    GitHTTPAuthentication gitAuthentication = (GitHTTPAuthentication) gitConfig.getGitAuth();
    assertThat(gitConfig.getGitConnectionType()).isEqualTo(ACCOUNT);
    assertThat(gitConfig.getUrl()).isEqualTo(url);
    assertThat(gitConfig.getValidationRepo()).isEqualTo(validationRepo);
    assertThat(gitAuthentication.getUsername()).isEqualTo(username);
    assertThat(gitAuthentication.getPasswordRef().toSecretRefStringValue()).isEqualTo(passwordRef);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testMappingToSSHGitConfigDTO() {
    final String url = "url";
    String sshKeyReference = "sshKeyReference";
    final GithubAuthentication githubAuthentication =
        GithubAuthentication.builder()
            .authType(GitAuthType.SSH)
            .credentials(
                GithubSshCredentials.builder().sshKeyRef(SecretRefHelper.createSecretRef(sshKeyReference)).build())
            .build();

    final GithubConnector githubConnector = GithubConnector.builder()
                                                .url(url)
                                                .connectionType(GitConnectionType.REPO)
                                                .authentication(githubAuthentication)
                                                .delegateSelectors(delegateSelectors)
                                                .build();
    GitConfig gitConfig = GithubToGitMapper.mapToGitConfigDTO(githubConnector);
    assertThat(gitConfig).isNotNull();
    assertThat(gitConfig.getGitAuthType()).isEqualTo(SSH);
    GitSSHAuthentication gitAuthentication = (GitSSHAuthentication) gitConfig.getGitAuth();
    assertThat(gitAuthentication.getEncryptedSshKey()).isEqualTo(SecretRefHelper.createSecretRef(sshKeyReference));
    assertThat(gitConfig.getUrl()).isEqualTo(url);
    assertThat(gitConfig.getDelegateSelectors()).isEqualTo(delegateSelectors);
    assertThat(gitConfig.getGitConnectionType()).isEqualTo(REPO);
  }
}