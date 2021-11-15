package io.harness.connector.mappers.gitconnectormapper;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.delegate.beans.connector.scm.GitAuthType.SSH;
import static io.harness.delegate.beans.connector.scm.GitConnectionType.ACCOUNT;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication;
import io.harness.delegate.beans.connector.scm.genericgitconnector.CustomCommitAttributes;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthentication;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthentication;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefHelper;
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
    String validationRepo = "validationRepo";
    CustomCommitAttributes customCommitAttributes = CustomCommitAttributes.builder()
                                                        .authorEmail("author")
                                                        .authorName("authorName")
                                                        .commitMessage("commitMessage")
                                                        .build();
    GitUserNamePasswordAuthentication gitUserNamePasswordAuthentication =
        GitUserNamePasswordAuthentication.builder().userName(userName).passwordReference(passwordReference).build();
    io.harness.connector.entities.embedded.gitconnector.GitConfig gitConfig =
        io.harness.connector.entities.embedded.gitconnector.GitConfig.builder()
            .supportsGitSync(true)
            .authType(HTTP)
            .url(url)
            .validationRepo(validationRepo)
            .connectionType(ACCOUNT)
            .customCommitAttributes(customCommitAttributes)
            .authenticationDetails(gitUserNamePasswordAuthentication)
            .build();
    GitConfig gitConfigDTO =
        gitEntityToDTO.createConnectorDTO((io.harness.connector.entities.embedded.gitconnector.GitConfig) gitConfig);
    assertThat(gitConfigDTO).isNotNull();
    assertThat(gitConfigDTO.getGitAuthType()).isEqualTo(HTTP);
    GitHTTPAuthentication gitAuthentication = (GitHTTPAuthentication) gitConfigDTO.getGitAuth();
    assertThat(gitConfigDTO.getGitConnectionType()).isEqualTo(ACCOUNT);
    assertThat(gitConfigDTO.getUrl()).isEqualTo(url);
    assertThat(gitConfigDTO.getValidationRepo()).isEqualTo(validationRepo);
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
    io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication sshAuthentication =
        io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication.builder()
            .sshKeyReference("sshKeyReference")
            .build();
    io.harness.connector.entities.embedded.gitconnector.GitConfig gitConfig =
        io.harness.connector.entities.embedded.gitconnector.GitConfig.builder()
            .supportsGitSync(true)
            .authType(SSH)
            .url(url)
            .connectionType(ACCOUNT)
            .customCommitAttributes(customCommitAttributes)
            .authenticationDetails(sshAuthentication)
            .build();
    GitConfig gitConfigDTO =
        gitEntityToDTO.createConnectorDTO((io.harness.connector.entities.embedded.gitconnector.GitConfig) gitConfig);
    assertThat(gitConfigDTO).isNotNull();
    assertThat(gitConfigDTO.getGitAuthType()).isEqualTo(SSH);
    GitSSHAuthentication gitAuthentication = (GitSSHAuthentication) gitConfigDTO.getGitAuth();
    assertThat(gitAuthentication.getEncryptedSshKey()).isEqualTo(SecretRefHelper.createSecretRef(sshKeyReference));
    assertThat(gitConfigDTO.getUrl()).isEqualTo(url);
    assertThat(gitConfigDTO.getGitConnectionType()).isEqualTo(ACCOUNT);
  }
}
