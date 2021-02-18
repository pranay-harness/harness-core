package io.harness.connector.mappers.gitconnectormapper;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.delegate.beans.connector.scm.GitAuthType.SSH;
import static io.harness.delegate.beans.connector.scm.GitConnectionType.ACCOUNT;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication;
import io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication;
import io.harness.delegate.beans.connector.scm.genericgitconnector.CustomCommitAttributes;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSyncConfig;
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

public class GitDTOToEntityTest extends CategoryTest {
  @InjectMocks GitDTOToEntity gitDTOToEntity;
  @Mock SecretRefService secretRefService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void toGitConfigForUserNamePassword() {
    String url = "url";
    String userName = "userName";
    String passwordIdentifier = "passwordIdentifier";
    String passwordReference = Scope.ACCOUNT.getYamlRepresentation() + "." + passwordIdentifier;
    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();
    SecretRefData passwordSecretRef =
        SecretRefData.builder().scope(Scope.ACCOUNT).identifier(passwordIdentifier).build();
    when(secretRefService.validateAndGetSecretConfigString(passwordSecretRef, ngAccess))
        .thenReturn(passwordSecretRef.toSecretRefStringValue());
    CustomCommitAttributes customCommitAttributes = CustomCommitAttributes.builder()
                                                        .authorEmail("author")
                                                        .authorName("authorName")
                                                        .commitMessage("commitMessage")
                                                        .build();
    GitSyncConfig gitSyncConfig =
        GitSyncConfig.builder().isSyncEnabled(true).customCommitAttributes(customCommitAttributes).build();
    GitHTTPAuthenticationDTO httpAuthentication =
        GitHTTPAuthenticationDTO.builder().username(userName).passwordRef(passwordSecretRef).build();
    GitConfigDTO gitConfigDTO = GitConfigDTO.builder()
                                    .gitSyncConfig(gitSyncConfig)
                                    .gitAuthType(HTTP)
                                    .gitConnectionType(ACCOUNT)
                                    .url(url)
                                    .gitAuth(httpAuthentication)
                                    .build();
    GitConfig gitConfig = gitDTOToEntity.toConnectorEntity(
        gitConfigDTO, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
    assertThat(gitConfig).isNotNull();
    assertThat(gitConfig.isSupportsGitSync()).isTrue();
    assertThat(gitConfig.getUrl()).isEqualTo(url);
    assertThat(gitConfig.getConnectionType()).isEqualTo(ACCOUNT);
    assertThat(gitConfig.getAuthType()).isEqualTo(HTTP);
    assertThat(gitConfig.getCustomCommitAttributes()).isEqualTo(customCommitAttributes);
    GitUserNamePasswordAuthentication gitUserNamePasswordAuthentication =
        (GitUserNamePasswordAuthentication) gitConfig.getAuthenticationDetails();
    assertThat(gitUserNamePasswordAuthentication.getUserName()).isEqualTo(userName);
    assertThat(gitUserNamePasswordAuthentication.getPasswordReference()).isEqualTo(passwordReference);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void toGitConfigForSSHKey() {
    String url = "url";
    String sshKeyReference = "sshKeyReference";
    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();
    SecretRefData sshKeyRef = SecretRefData.builder().scope(Scope.ACCOUNT).identifier(sshKeyReference).build();
    when(secretRefService.validateAndGetSecretConfigString(sshKeyRef, ngAccess))
        .thenReturn(sshKeyRef.toSecretRefStringValue());
    CustomCommitAttributes customCommitAttributes = CustomCommitAttributes.builder()
                                                        .authorEmail("author")
                                                        .authorName("authorName")
                                                        .commitMessage("commitMessage")
                                                        .build();
    GitSSHAuthenticationDTO httpAuthentication = GitSSHAuthenticationDTO.builder().encryptedSshKey(sshKeyRef).build();
    GitConfigDTO gitConfigDTO =
        GitConfigDTO.builder().gitAuthType(SSH).gitConnectionType(ACCOUNT).url(url).gitAuth(httpAuthentication).build();
    GitConfig gitConfig = gitDTOToEntity.toConnectorEntity(
        gitConfigDTO, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
    assertThat(gitConfig).isNotNull();
    assertThat(gitConfig.isSupportsGitSync()).isFalse();
    assertThat(gitConfig.getUrl()).isEqualTo(url);
    assertThat(gitConfig.getConnectionType()).isEqualTo(ACCOUNT);
    assertThat(gitConfig.getAuthType()).isEqualTo(SSH);
    GitSSHAuthentication gitSSHAuthentication = (GitSSHAuthentication) gitConfig.getAuthenticationDetails();
    assertThat(gitSSHAuthentication.getSshKeyReference()).isEqualTo(sshKeyRef.toSecretRefStringValue());
  }
}
