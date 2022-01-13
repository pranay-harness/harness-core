/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.rule.OwnerRule.TMACARI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.delegatetasks.validation.capabilities.GitConnectionCapability;

import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GitValidationParametersTest extends WingsBaseTest {
  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testFetchRequiredExecutionCapabilities() {
    GitValidationParameters gitValidationParameters =
        GitValidationParameters.builder().gitConfig(GitConfig.builder().repoUrl("https://abc").build()).build();

    List<ExecutionCapability> executionCapabilities = gitValidationParameters.fetchRequiredExecutionCapabilities(null);

    assertThat(executionCapabilities.size()).isEqualTo(1);
    assertThat(executionCapabilities.get(0)).isInstanceOf(GitConnectionCapability.class);

    gitValidationParameters = GitValidationParameters.builder()
                                  .gitConfig(GitConfig.builder().repoUrl("https://abc").build())
                                  .isGitHostConnectivityCheck(true)
                                  .build();

    executionCapabilities = gitValidationParameters.fetchRequiredExecutionCapabilities(null);
    assertThat(executionCapabilities.get(0)).isInstanceOf(HttpConnectionExecutionCapability.class);
  }
}
