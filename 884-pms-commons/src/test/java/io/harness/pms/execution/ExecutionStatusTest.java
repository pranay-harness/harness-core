/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.execution.Status;
import io.harness.rule.Owner;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class ExecutionStatusTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testMappingFromStatusToExecutionStatus() {
    Set<ExecutionStatus> executionStatusesVisited = new HashSet<>();
    for (Status value : Status.values()) {
      if (Status.NO_OP == value || Status.UNRECOGNIZED == value) {
        continue;
      }
      ExecutionStatus executionStatus = ExecutionStatus.getExecutionStatus(value);
      assertThat(executionStatus).isNotNull();
      boolean contains = executionStatusesVisited.contains(executionStatus);
      assertThat(contains).isFalse();
      executionStatusesVisited.add(executionStatus);
    }
  }
}
