/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.instance.licensing;

import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InstanceUsageLimitCheckerImplTest extends CategoryTest {
  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testIsWithinLimit() {
    double actualUsage = 90;
    double allowed = 100;
    boolean withinLimit = InstanceUsageLimitCheckerImpl.isWithinLimit(actualUsage, 85, allowed);
    assertThat(withinLimit).isFalse();
    withinLimit = InstanceUsageLimitCheckerImpl.isWithinLimit(actualUsage, 95, allowed);
    assertThat(withinLimit).isTrue();
  }
}
