/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SkipStateExecutionDataTest extends CategoryTest {
  private static final String ASSERTION_EXPRESSION = "${env.name} == \"qa\"";
  private SkipStateExecutionData skipStateExecutionData;

  @Before
  public void setup() {
    skipStateExecutionData = SkipStateExecutionData.builder().skipAssertionExpression(ASSERTION_EXPRESSION).build();
    skipStateExecutionData.setStatus(ExecutionStatus.SKIPPED);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetExecutionSummary() {
    assertThat(skipStateExecutionData.getExecutionSummary())
        .containsAllEntriesOf(ImmutableMap.of("skipAssertionExpression",
            ExecutionDataValue.builder().displayName("Skip Condition").value(ASSERTION_EXPRESSION).build()));
  }
}
