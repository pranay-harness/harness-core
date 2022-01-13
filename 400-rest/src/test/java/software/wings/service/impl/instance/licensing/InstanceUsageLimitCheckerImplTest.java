/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
