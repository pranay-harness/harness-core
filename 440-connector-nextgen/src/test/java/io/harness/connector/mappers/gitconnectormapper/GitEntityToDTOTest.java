package io.harness.connector.mappers.gitconnectormapper;

import static io.harness.delegate.beans.connector.gitconnector.GitAuthType.HTTP;
import static io.harness.delegate.beans.connector.gitconnector.GitAuthType.SSH;
import static io.harness.delegate.beans.connector.gitconnector.GitConnectionType.ACCOUNT;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication;
import io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication;
import io.harness.delegate.beans.connector.gitconnector.CustomCommitAttributes;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitSSHAuthenticationDTO;
import io.harness.encryption.Scope;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class GitEntityToDTOTest extends CategoryTest {
  @InjectMocks GitEntityToDTO gitEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_createGitConfigDTOForUserNamePassword() {
    String url = "url";
    String userName = "userName";
    String passwordReference = Scope.ACCOUNT.getYamlRepresentation() + ".password";
    CustomCommitAttributes customCommitAttributes = CustomCommitAttributes.builder()
                                                        .authorEmail("author")
                                                        .authorName("authorName")
                                                        .commitMessage("commitMessage")
                                                        .build();
    GitUserNamePasswordAuthentication gitUserNamePasswordAuthentication =
        GitUserNamePasswordAuthentication.builder().userName(userName).passwordReference(passwordReference).build();
    GitConfig gitConfig = GitConfig.builder()
                              .supportsGitSync(true)
                              .authType(HTTP)
                              .url(url)
                              .connectionType(ACCOUNT)
                              .customCommitAttributes(customCommitAttributes)
                              .authenticationDetails(gitUserNamePasswordAuthentication)
                              .build();
    GitConfigDTO gitConfigDTO = gitEntityToDTO.createConnectorDTO((GitConfig) gitConfig);
    assertThat(gitConfigDTO).isNotNull();
    assertThat(gitConfigDTO.getGitAuthType()).isEqualTo(HTTP);
    assertThat(gitConfigDTO.getGitSyncConfig().isSyncEnabled()).isEqualTo(true);
    assertThat(gitConfigDTO.getGitSyncConfig().getCustomCommitAttributes()).isEqualTo(customCommitAttributes);
    GitHTTPAuthenticationDTO gitAuthentication = (GitHTTPAuthenticationDTO) gitConfigDTO.getGitAuth();
    assertThat(gitConfigDTO.getGitConnectionType()).isEqualTo(ACCOUNT);
    assertThat(gitConfigDTO.getUrl()).isEqualTo(url);
    assertThat(gitAuthentication.getUsername()).isEqualTo(userName);
    assertThat(gitAuthentication.getPasswordRef().toSecretRefStringValue()).isEqualTo(passwordReference);
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
    GitConfigDTO gitConfigDTO = gitEntityToDTO.createConnectorDTO((GitConfig) gitConfig);
    assertThat(gitConfigDTO).isNotNull();
    assertThat(gitConfigDTO.getGitAuthType()).isEqualTo(SSH);
    assertThat(gitConfigDTO.getGitSyncConfig().isSyncEnabled()).isEqualTo(true);
    assertThat(gitConfigDTO.getGitSyncConfig().getCustomCommitAttributes()).isEqualTo(customCommitAttributes);
    GitSSHAuthenticationDTO gitAuthentication = (GitSSHAuthenticationDTO) gitConfigDTO.getGitAuth();
    assertThat(gitAuthentication.getEncryptedSshKey()).isEqualTo(sshKeyReference);
    assertThat(gitConfigDTO.getUrl()).isEqualTo(url);
    assertThat(gitConfigDTO.getGitConnectionType()).isEqualTo(ACCOUNT);
  }
}
