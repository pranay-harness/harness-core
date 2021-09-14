/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.core.winrm.executors;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.DINESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@OwnedBy(CDP)
public class WinRmExecutorFactoryTest extends CategoryTest {
  @Mock WinRmExecutorFactory winRmExecutorFactory = new WinRmExecutorFactory();
  WinRmSessionConfig winRmSessionConfig;

  @Test
  @Owner(developers = DINESH)
  @Category(UnitTests.class)
  public void shouldGetWinRmExecutor() {
    winRmSessionConfig = WinRmSessionConfig.builder().build();
    assertThat(winRmExecutorFactory.getExecutor(winRmSessionConfig, false))
        .isNotNull()
        .isInstanceOf(WinRmExecutor.class);
  }
}
