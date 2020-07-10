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
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class GitEntityToDTOTest extends ConnectorsBaseTest {
  @Inject @InjectMocks GitEntityToDTO gitEntityToDTO;

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_createGitConfigDTOForUserNamePassword() {
    String url = "url";
    String userName = "userName";
    String passwordReference = "password";
    CustomCommitAttributes customCommitAttributes = CustomCommitAttributes.builder()
                                                        .authorEmail("author")
                                                        .authorName("authorName")
                                                        .commitMessage("commitMessage")
                                                        .build();
    UserNamePasswordGitAuthentication userNamePasswordGitAuthentication =
        UserNamePasswordGitAuthentication.builder().userName(userName).passwordReference(passwordReference).build();
    GitConfig gitConfig = GitConfig.builder()
                              .supportsGitSync(true)
                              .authType(HTTP)
                              .url(url)
                              .connectionType(ACCOUNT)
                              .customCommitAttributes(customCommitAttributes)
                              .authenticationDetails(userNamePasswordGitAuthentication)
                              .build();
    GitConfigDTO gitConfigDTO = gitEntityToDTO.createGitConfigDTO(gitConfig);
    assertThat(gitConfigDTO).isNotNull();
    assertThat(gitConfigDTO.getGitAuthType()).isEqualTo(HTTP);
    assertThat(gitConfigDTO.getGitSyncConfig().isSyncEnabled()).isEqualTo(true);
    assertThat(gitConfigDTO.getGitSyncConfig().getCustomCommitAttributes()).isEqualTo(customCommitAttributes);
    GitHTTPAuthenticationDTO gitAuthentication = (GitHTTPAuthenticationDTO) gitConfigDTO.getGitAuth();
    assertThat(gitAuthentication.getGitType()).isEqualTo(ACCOUNT);
    assertThat(gitAuthentication.getUrl()).isEqualTo(url);
    assertThat(gitAuthentication.getUsername()).isEqualTo(userName);
    assertThat(gitAuthentication.getPasswordReference()).isEqualTo(passwordReference);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_createGitConfigDTOForSSHKey() {
    String url = "url";
    String sshKeyReference = "sshKeyReference";
    CustomCommitAttributes customCommitAttributes = CustomCommitAttributes.builder()
                                                        .authorEmail("author")
                                                        .authorName("authorName")
                                                        .commitMessage("commitMessage")
                                                        .build();
    GitSSHAuthentication sshAuthentication = GitSSHAuthentication.builder().sshKeyReference("sshKeyReference").build();
    GitConfig gitConfig = GitConfig.builder()
                              .supportsGitSync(true)
                              .authType(SSH)
                              .url(url)
                              .connectionType(ACCOUNT)
                              .customCommitAttributes(customCommitAttributes)
                              .authenticationDetails(sshAuthentication)
                              .build();
    GitConfigDTO gitConfigDTO = gitEntityToDTO.createGitConfigDTO(gitConfig);
    assertThat(gitConfigDTO).isNotNull();
    assertThat(gitConfigDTO.getGitAuthType()).isEqualTo(SSH);
    assertThat(gitConfigDTO.getGitSyncConfig().isSyncEnabled()).isEqualTo(true);
    assertThat(gitConfigDTO.getGitSyncConfig().getCustomCommitAttributes()).isEqualTo(customCommitAttributes);
    GitSSHAuthenticationDTO gitAuthentication = (GitSSHAuthenticationDTO) gitConfigDTO.getGitAuth();
    assertThat(gitAuthentication.getSshKeyReference()).isEqualTo(sshKeyReference);
    assertThat(gitAuthentication.getUrl()).isEqualTo(url);
    assertThat(gitAuthentication.getGitType()).isEqualTo(ACCOUNT);
  }
}