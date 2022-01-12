/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.commands;

import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.testframework.framework.ManagerExecutor;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.zeroturnaround.exec.ProcessExecutor;

public class InspectCommandTest extends AbstractFunctionalTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(FunctionalTests.class)
  @Ignore("Needs more work to make it works")
  public void testIsnpectCommand() throws IOException, TimeoutException, InterruptedException {
    ProcessExecutor inspect = ManagerExecutor.managerProcessExecutor(AbstractFunctionalTest.class, "inspect");
    String output = inspect.readOutput(true).execute().outputUTF8();
    assertThat(output).contains("the inspection finished");
  }
}
