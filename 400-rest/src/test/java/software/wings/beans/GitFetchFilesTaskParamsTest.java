/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.TMACARI;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.delegatetasks.validation.capabilities.GitConnectionCapability;
import software.wings.service.impl.ContainerServiceParams;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GitFetchFilesTaskParamsTest extends WingsBaseTest {
  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testFetchRequiredExecutionCapabilities() {
    GitFetchFilesTaskParams gitFetchFilesTaskParams =
        GitFetchFilesTaskParams.builder()
            .containerServiceParams(ContainerServiceParams.builder()
                                        .settingAttribute(aSettingAttribute().build())
                                        .masterUrl("http://foo.bar")
                                        .build())
            .build();

    gitFetchFilesTaskParams.setBindTaskFeatureSet(true);
    List<ExecutionCapability> executionCapabilities = gitFetchFilesTaskParams.fetchRequiredExecutionCapabilities(null);

    assertThat(executionCapabilities.size()).isEqualTo(1);
    assertThat(executionCapabilities.get(0)).isInstanceOf(HttpConnectionExecutionCapability.class);
    assertThat(((HttpConnectionExecutionCapability) executionCapabilities.get(0)).getHost()).isEqualTo("foo.bar");

    Map<String, GitFetchFilesConfig> gitFetchFilesConfigMap = new HashMap<>();
    gitFetchFilesConfigMap.put("Service",
        GitFetchFilesConfig.builder()
            .gitConfig(GitConfig.builder().repoUrl("http://abc.xyz").build())
            .encryptedDataDetails(Collections.emptyList())
            .build());
    gitFetchFilesConfigMap.put("Environment",
        GitFetchFilesConfig.builder()
            .gitConfig(GitConfig.builder().repoUrl("http://hello.world").build())
            .encryptedDataDetails(Collections.emptyList())
            .build());
    gitFetchFilesTaskParams.setGitFetchFilesConfigMap(gitFetchFilesConfigMap);

    gitFetchFilesTaskParams.setBindTaskFeatureSet(false);
    gitFetchFilesTaskParams.setGitHostConnectivityCheck(true);
    executionCapabilities = gitFetchFilesTaskParams.fetchRequiredExecutionCapabilities(null);

    assertThat(executionCapabilities.size()).isEqualTo(2);
    assertThat(executionCapabilities.get(0)).isInstanceOf(HttpConnectionExecutionCapability.class);
    assertThat(executionCapabilities.get(1)).isInstanceOf(HttpConnectionExecutionCapability.class);

    gitFetchFilesTaskParams.setGitHostConnectivityCheck(false);
    executionCapabilities = gitFetchFilesTaskParams.fetchRequiredExecutionCapabilities(null);

    assertThat(executionCapabilities.size()).isEqualTo(2);
    assertThat(executionCapabilities.get(0)).isInstanceOf(GitConnectionCapability.class);
    assertThat(executionCapabilities.get(1)).isInstanceOf(GitConnectionCapability.class);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testFetchRequiredExecutionCapabilitiesForDifferentRepoTypes() {
    GitFetchFilesTaskParams gitFetchFilesTaskParams =
        GitFetchFilesTaskParams.builder()
            .containerServiceParams(ContainerServiceParams.builder()
                                        .settingAttribute(aSettingAttribute().build())
                                        .masterUrl("http://foo.bar")
                                        .build())
            .build();
    Map<String, GitFetchFilesConfig> gitFetchFilesConfigMap = new HashMap<>();
    gitFetchFilesConfigMap.put("Service",
        GitFetchFilesConfig.builder()
            .gitConfig(GitConfig.builder().repoUrl("http://abc.xyz").build())
            .encryptedDataDetails(Collections.emptyList())
            .build());
    HostConnectionAttributes hostConnectionAttributes = new HostConnectionAttributes();
    hostConnectionAttributes.setSshPort(22);
    SettingAttribute sshSettingAttribute = new SettingAttribute();
    sshSettingAttribute.setValue(hostConnectionAttributes);
    gitFetchFilesConfigMap.put("Environment",
        GitFetchFilesConfig.builder()
            .gitConfig(
                GitConfig.builder().repoUrl("git@github.com/abc").sshSettingAttribute(sshSettingAttribute).build())
            .encryptedDataDetails(Collections.emptyList())
            .build());
    gitFetchFilesTaskParams.setGitFetchFilesConfigMap(gitFetchFilesConfigMap);

    gitFetchFilesTaskParams.setBindTaskFeatureSet(false);
    gitFetchFilesTaskParams.setGitHostConnectivityCheck(true);
    List<ExecutionCapability> executionCapabilities = gitFetchFilesTaskParams.fetchRequiredExecutionCapabilities(null);

    assertThat(executionCapabilities.size()).isEqualTo(2);
    assertThat(executionCapabilities.get(0)).isInstanceOf(HttpConnectionExecutionCapability.class);
    assertThat(executionCapabilities.get(1)).isInstanceOf(SocketConnectivityExecutionCapability.class);

    gitFetchFilesTaskParams.setGitHostConnectivityCheck(false);
    executionCapabilities = gitFetchFilesTaskParams.fetchRequiredExecutionCapabilities(null);

    assertThat(executionCapabilities.size()).isEqualTo(2);
    assertThat(executionCapabilities.get(0)).isInstanceOf(GitConnectionCapability.class);
    assertThat(executionCapabilities.get(1)).isInstanceOf(GitConnectionCapability.class);
  }
}
