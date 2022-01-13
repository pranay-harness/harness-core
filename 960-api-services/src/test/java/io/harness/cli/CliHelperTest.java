/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cli;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class CliHelperTest extends CategoryTest {
  @InjectMocks private CliHelper cliHelper;
  @Mock LogCallback logCallback;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testExecuteCliCommand() throws InterruptedException, TimeoutException, IOException {
    CliResponse cliResponse =
        cliHelper.executeCliCommand("echo 1", TimeUnit.MINUTES.toMillis(1), Collections.emptyMap(), ".", logCallback);
    assertThat(cliResponse.getOutput()).isEqualTo("1\n");
    assertThat(cliResponse.getCommandExecutionStatus()).isEqualTo(SUCCESS);

    cliResponse = cliHelper.executeCliCommand(
        "echo1 $abc", TimeUnit.MINUTES.toMillis(1), Collections.emptyMap(), ".", logCallback);
    assertThat(cliResponse.getCommandExecutionStatus()).isEqualTo(FAILURE);
    assertThat(cliResponse.getError()).isNotNull();

    assertThatThrownBy(()
                           -> cliHelper.executeCliCommand(
                               "sleep 4", TimeUnit.MILLISECONDS.toMillis(1), Collections.emptyMap(), ".", logCallback))
        .isInstanceOf(TimeoutException.class);
  }
}
