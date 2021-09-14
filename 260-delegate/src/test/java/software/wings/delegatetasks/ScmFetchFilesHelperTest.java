/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.TMACARI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpecDTO;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.ProviderType;
import software.wings.beans.SettingAttribute;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class ScmFetchFilesHelperTest extends WingsBaseTest {
  private ScmFetchFilesHelper scmFetchFilesHelper;

  @Before
  public void setUp() throws Exception {
    scmFetchFilesHelper = new ScmFetchFilesHelper();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testShouldUseScm() {
    assertThat(scmFetchFilesHelper.shouldUseScm(
                   true, GitConfig.builder().sshSettingAttribute(null).providerType(ProviderType.GITHUB).build()))
        .isTrue();
    assertThat(scmFetchFilesHelper.shouldUseScm(
                   true, GitConfig.builder().sshSettingAttribute(null).providerType(ProviderType.GITLAB).build()))
        .isTrue();
    assertThat(scmFetchFilesHelper.shouldUseScm(
                   true, GitConfig.builder().sshSettingAttribute(null).providerType(ProviderType.GIT).build()))
        .isFalse();
    assertThat(scmFetchFilesHelper.shouldUseScm(false,
                   GitConfig.builder()
                       .sshSettingAttribute(SettingAttribute.Builder.aSettingAttribute().build())
                       .providerType(ProviderType.GITHUB)
                       .build()))
        .isFalse();
    assertThat(scmFetchFilesHelper.shouldUseScm(false,
                   GitConfig.builder()
                       .sshSettingAttribute(SettingAttribute.Builder.aSettingAttribute().build())
                       .providerType(ProviderType.GITLAB)
                       .build()))
        .isFalse();
    assertThat(scmFetchFilesHelper.shouldUseScm(
                   false, GitConfig.builder().sshSettingAttribute(null).providerType(ProviderType.GITHUB).build()))
        .isFalse();
    assertThat(scmFetchFilesHelper.shouldUseScm(
                   false, GitConfig.builder().sshSettingAttribute(null).providerType(ProviderType.GITLAB).build()))
        .isFalse();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetScmConnector() {
    ScmConnector gitHubScmConnector = scmFetchFilesHelper.getScmConnector(GitConfig.builder()
                                                                              .repoUrl("GitHubURL")
                                                                              .password("password".toCharArray())
                                                                              .providerType(ProviderType.GITHUB)
                                                                              .build());
    assertThat(gitHubScmConnector.getUrl()).isEqualTo("GitHubURL");
    assertThat(((GithubTokenSpecDTO) ((GithubConnectorDTO) gitHubScmConnector).getApiAccess().getSpec())
                   .getTokenRef()
                   .getDecryptedValue())
        .isEqualTo("password".toCharArray());

    ScmConnector gitLabScmConnector = scmFetchFilesHelper.getScmConnector(GitConfig.builder()
                                                                              .repoUrl("GitlabURL")
                                                                              .password("password".toCharArray())
                                                                              .providerType(ProviderType.GITLAB)
                                                                              .build());
    assertThat(gitLabScmConnector.getUrl()).isEqualTo("GitlabURL");
    assertThat(((GitlabTokenSpecDTO) ((GitlabConnectorDTO) gitLabScmConnector).getApiAccess().getSpec())
                   .getTokenRef()
                   .getDecryptedValue())
        .isEqualTo("password".toCharArray());

    assertThat(scmFetchFilesHelper.getScmConnector(GitConfig.builder().providerType(ProviderType.GIT).build()))
        .isNull();
  }
}
