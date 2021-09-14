/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.metrics;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AutoMetricContextTest extends CategoryTest {
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate() {
    String accountId = generateUuid();
    try (AutoMetricContext autoMetricContext = new AccountMetricContext(accountId)) {
      assertThat(ThreadContext.get(MetricConstants.METRIC_LABEL_PREFIX + "accountId")).isEqualTo(accountId);
    }
    assertThat(ThreadContext.get(MetricConstants.METRIC_LABEL_PREFIX + "accountId")).isNull();
  }

  private class AccountMetricContext extends AutoMetricContext {
    AccountMetricContext(String accountId) {
      put("accountId", accountId);
    }
  }
}
