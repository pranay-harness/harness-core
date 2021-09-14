/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.facilitation;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.ExecutionCheck;
import io.harness.execution.NodeExecution;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class AbstractPreFacilitationCheckerTest extends OrchestrationTestBase {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestCheck() {
    RunPreFacilitationChecker rChecker = spy(RunPreFacilitationChecker.class);
    SkipPreFacilitationChecker sChecker = spy(SkipPreFacilitationChecker.class);
    rChecker.setNextChecker(sChecker);

    doReturn(ExecutionCheck.builder().proceed(true).build()).when(rChecker).performCheck(any(NodeExecution.class));
    doReturn(ExecutionCheck.builder().proceed(true).build()).when(sChecker).performCheck(any(NodeExecution.class));

    rChecker.check(any(NodeExecution.class));

    verify(rChecker).performCheck(any(NodeExecution.class));
    verify(sChecker).performCheck(any(NodeExecution.class));
  }
}
