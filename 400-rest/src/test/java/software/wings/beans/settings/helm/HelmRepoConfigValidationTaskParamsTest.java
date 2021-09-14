/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.settings.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.TATHAGAT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.rule.Owner;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class HelmRepoConfigValidationTaskParamsTest extends CategoryTest {
  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testFetchRequiredExecutionCapabilities() {
    HelmRepoConfigValidationTaskParams taskParams = HelmRepoConfigValidationTaskParams.builder().build();
    assertThat(taskParams.fetchRequiredExecutionCapabilities(null)).hasSize(0);
    taskParams.setDelegateSelectors(Collections.singleton("delegate1"));
    List<ExecutionCapability> executionCapabilities = taskParams.fetchRequiredExecutionCapabilities(null);
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0) instanceof SelectorCapability).isTrue();
  }
}
