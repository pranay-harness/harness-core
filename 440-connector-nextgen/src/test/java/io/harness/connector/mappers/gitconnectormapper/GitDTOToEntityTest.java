/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.gitconnectormapper;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.delegate.beans.connector.scm.GitAuthType.SSH;
import static io.harness.delegate.beans.connector.scm.GitConnectionType.ACCOUNT;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication;
import io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication;
import io.harness.delegate.beans.connector.scm.genericgitconnector.CustomCommitAttributes;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthenticationDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class GitDTOToEntityTest extends CategoryTest {
  @InjectMocks GitDTOToEntity gitDTOToEntity;

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
    String validationRepo = "validationRepo";

    SecretRefData passwordRef = SecretRefHelper.createSecretRef(passwordReference);
    CustomCommitAttributes customCommitAttributes = CustomCommitAttributes.builder()
                                                        .authorEmail("author")
                                                        .authorName("authorName")
                                                        .commitMessage("commitMessage")
                                                        .build();
    GitHTTPAuthenticationDTO httpAuthentication =
        GitHTTPAuthenticationDTO.builder().username(userName).passwordRef(passwordRef).build();
    GitConfigDTO gitConfigDTO = GitConfigDTO.builder()
                                    .gitAuthType(HTTP)
                                    .gitConnectionType(ACCOUNT)
                                    .url(url)
                                    .validationRepo(validationRepo)
                                    .gitAuth(httpAuthentication)
                                    .build();
    GitConfig gitConfig = gitDTOToEntity.toConnectorEntity(gitConfigDTO);
    assertThat(gitConfig).isNotNull();
    assertThat(gitConfig.getUrl()).isEqualTo(url);
    assertThat(gitConfig.getValidationRepo()).isEqualTo(validationRepo);
    assertThat(gitConfig.getConnectionType()).isEqualTo(ACCOUNT);
    assertThat(gitConfig.getAuthType()).isEqualTo(HTTP);
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
    CustomCommitAttributes customCommitAttributes = CustomCommitAttributes.builder()
                                                        .authorEmail("author")
                                                        .authorName("authorName")
                                                        .commitMessage("commitMessage")
                                                        .build();
    GitSSHAuthenticationDTO httpAuthentication =
        GitSSHAuthenticationDTO.builder().encryptedSshKey(SecretRefHelper.createSecretRef(sshKeyReference)).build();
    GitConfigDTO gitConfigDTO =
        GitConfigDTO.builder().gitAuthType(SSH).gitConnectionType(ACCOUNT).url(url).gitAuth(httpAuthentication).build();
    GitConfig gitConfig = gitDTOToEntity.toConnectorEntity(gitConfigDTO);
    assertThat(gitConfig).isNotNull();
    assertThat(gitConfig.isSupportsGitSync()).isFalse();
    assertThat(gitConfig.getUrl()).isEqualTo(url);
    assertThat(gitConfig.getConnectionType()).isEqualTo(ACCOUNT);
    assertThat(gitConfig.getAuthType()).isEqualTo(SSH);
    GitSSHAuthentication gitSSHAuthentication = (GitSSHAuthentication) gitConfig.getAuthenticationDetails();
    assertThat(gitSSHAuthentication.getSshKeyReference()).isEqualTo(sshKeyReference);
  }
}
