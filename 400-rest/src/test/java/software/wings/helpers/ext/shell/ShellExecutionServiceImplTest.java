/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.shell;

import static io.harness.rule.OwnerRule.AADITI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.shell.ShellExecutionRequest;
import io.harness.shell.ShellExecutionResponse;
import io.harness.shell.ShellExecutionServiceImpl;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class ShellExecutionServiceImplTest extends WingsBaseTest {
  @Inject @InjectMocks private ShellExecutionServiceImpl shellExecutionService;

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testExecuteShellScript() {
    ShellExecutionRequest shellExecutionRequest = ShellExecutionRequest.builder()
                                                      .scriptString("echo \"foo\" > $ARTIFACT_RESULT_PATH")
                                                      .workingDirectory("/tmp")
                                                      .build();
    ShellExecutionResponse shellExecutionResponse = shellExecutionService.execute(shellExecutionRequest);
    assertThat(shellExecutionResponse).isNotNull();
    assertThat(shellExecutionResponse.getExitValue()).isEqualTo(0);
    assertThat(shellExecutionResponse.getShellExecutionData()).isNotEmpty();
    assertThat(shellExecutionResponse.getShellExecutionData().get("ARTIFACT_RESULT_PATH")).isNotNull();
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testExecuteScriptTimeout() {
    ShellExecutionRequest shellExecutionRequest =
        ShellExecutionRequest.builder().scriptString("sleep 10").workingDirectory("/tmp").timeoutSeconds(1).build();
    ShellExecutionResponse shellExecutionResponse = shellExecutionService.execute(shellExecutionRequest);
    assertThat(shellExecutionResponse).isNotNull();
    assertThat(shellExecutionResponse.getExitValue()).isNotEqualTo(0);
    assertThat(shellExecutionResponse.getShellExecutionData()).isNull();
  }
}
