package io.harness.connector.mappers.gitconnectormapper;

import static io.harness.delegate.beans.connector.gitconnector.GitAuthType.HTTP;
import static io.harness.delegate.beans.connector.gitconnector.GitAuthType.SSH;
import static io.harness.delegate.beans.connector.gitconnector.GitConnectionType.ACCOUNT;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorsBaseTest;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication;
import io.harness.connector.entities.embedded.gitconnector.UserNamePasswordGitAuthentication;
import io.harness.delegate.beans.connector.gitconnector.CustomCommitAttributes;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitSSHAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitSyncConfig;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class GitDTOToEntityTest extends ConnectorsBaseTest {
  @Inject @InjectMocks GitDTOToEntity gitDTOToEntity;

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void toGitConfigForUserNamePassword() {
    String url = "url";
    String userName = "userName";
    String passwordReference = "password";
    CustomCommitAttributes customCommitAttributes = CustomCommitAttributes.builder()
                                                        .authorEmail("author")
                                                        .authorName("authorName")
                                                        .commitMessage("commitMessage")
                                                        .build();
    GitSyncConfig gitSyncConfig =
        GitSyncConfig.builder().isSyncEnabled(true).customCommitAttributes(customCommitAttributes).build();
    GitHTTPAuthenticationDTO httpAuthentication = GitHTTPAuthenticationDTO.builder()
                                                      .gitType(ACCOUNT)
                                                      .url(url)
                                                      .username(userName)
                                                      .passwordReference(passwordReference)
                                                      .build();
    GitConfigDTO gitConfigDTO =
        GitConfigDTO.builder().gitSyncConfig(gitSyncConfig).gitAuthType(HTTP).gitAuth(httpAuthentication).build();
    GitConfig gitConfig = gitDTOToEntity.toGitConfig(gitConfigDTO);
    assertThat(gitConfig).isNotNull();
    assertThat(gitConfig.isSupportsGitSync()).isTrue();
    assertThat(gitConfig.getUrl()).isEqualTo(url);
    assertThat(gitConfig.getConnectionType()).isEqualTo(ACCOUNT);
    assertThat(gitConfig.getAuthType()).isEqualTo(HTTP);
    assertThat(gitConfig.getCustomCommitAttributes()).isEqualTo(customCommitAttributes);
    UserNamePasswordGitAuthentication userNamePasswordGitAuthentication =
        (UserNamePasswordGitAuthentication) gitConfig.getAuthenticationDetails();
    assertThat(userNamePasswordGitAuthentication.getUserName()).isEqualTo(userName);
    assertThat(userNamePasswordGitAuthentication.getPasswordReference()).isEqualTo(passwordReference);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void toGitConfigForSSHKey() {
    String url = "url";
    String sshKeyReference = "sshKeyReference";
    CustomCommitAttributes customCommitAttributes = CustomCommitAttributes.builder()
                                                        .authorEmail("author")
                                                        .authorName("authorName")
                                                        .commitMessage("commitMessage")
                                                        .build();
    GitSyncConfig gitSyncConfig =
        GitSyncConfig.builder().isSyncEnabled(true).customCommitAttributes(customCommitAttributes).build();
    GitSSHAuthenticationDTO httpAuthentication =
        GitSSHAuthenticationDTO.builder().gitType(ACCOUNT).url(url).sshKeyReference(sshKeyReference).build();
    GitConfigDTO gitConfigDTO =
        GitConfigDTO.builder().gitSyncConfig(gitSyncConfig).gitAuthType(SSH).gitAuth(httpAuthentication).build();
    GitConfig gitConfig = gitDTOToEntity.toGitConfig(gitConfigDTO);
    assertThat(gitConfig).isNotNull();
    assertThat(gitConfig.isSupportsGitSync()).isTrue();
    assertThat(gitConfig.getUrl()).isEqualTo(url);
    assertThat(gitConfig.getConnectionType()).isEqualTo(ACCOUNT);
    assertThat(gitConfig.getAuthType()).isEqualTo(SSH);
    assertThat(gitConfig.getCustomCommitAttributes()).isEqualTo(customCommitAttributes);
    GitSSHAuthentication gitSSHAuthentication = (GitSSHAuthentication) gitConfig.getAuthenticationDetails();
    assertThat(gitSSHAuthentication.getSshKeyReference()).isEqualTo(sshKeyReference);
  }
}